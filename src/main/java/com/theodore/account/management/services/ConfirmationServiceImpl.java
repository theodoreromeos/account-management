package com.theodore.account.management.services;

import com.theodore.account.management.entities.EmailVerificationToken;
import com.theodore.account.management.entities.OrganizationUserRegistrationRequest;
import com.theodore.account.management.entities.UserProfile;
import com.theodore.account.management.enums.AccountConfirmedBy;
import com.theodore.account.management.enums.RegistrationStatus;
import com.theodore.account.management.exceptions.AccountConfirmationException;
import com.theodore.account.management.exceptions.InvalidStatusException;
import com.theodore.account.management.exceptions.InvalidTokenException;
import com.theodore.account.management.models.dto.requests.ConfirmOrgAdminEmailRequestDto;
import com.theodore.account.management.models.dto.responses.OrgAdminInfoResponseDto;
import com.theodore.account.management.repositories.EmailVerificationTokenRepository;
import com.theodore.queue.common.emails.EmailDto;
import com.theodore.racingmodel.exceptions.NotFoundException;
import com.theodore.user.ConfirmationStatus;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ConfirmationServiceImpl implements ConfirmationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfirmationServiceImpl.class);

    private static final String TOKEN_ID = "tid";
    private static final String EMAIL = "email";

    private static final String USER_NOT_FOUND = "User not found";


    private final EmailTokenService emailTokenService;
    private final UserProfileService userProfileService;
    private final OrganizationUserRegistrationRequestService organizationUserRegistrationRequestService;
    private final AuthServerGrpcClient authServerGrpcClient;
    private final MessagingService messagingService;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;

    public ConfirmationServiceImpl(EmailTokenService emailTokenService,
                                   UserProfileService userProfileService,
                                   OrganizationUserRegistrationRequestService organizationUserRegistrationRequestService,
                                   AuthServerGrpcClient authServerGrpcClient,
                                   MessagingService messagingService,
                                   EmailVerificationTokenRepository emailVerificationTokenRepository) {
        this.emailTokenService = emailTokenService;
        this.userProfileService = userProfileService;
        this.organizationUserRegistrationRequestService = organizationUserRegistrationRequestService;
        this.authServerGrpcClient = authServerGrpcClient;
        this.messagingService = messagingService;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
    }

    @Override
    public void confirmSimpleUserEmail(String token) {

        Jws<Claims> claims = emailTokenService.parseToken(token);

        String email = claims.getBody().get(EMAIL, String.class);

        var verificationToken = processVerificationToken(claims);

        checkUserProfileDetails(verificationToken.getUserId(), email);

        //send to auth server that user is authenticated
        var response = authServerGrpcClient.authServerNewUserConfirmation(verificationToken.getUserId());
        if (!response.getConfirmationStatus().equals(ConfirmationStatus.CONFIRMED)) {
            throw new AccountConfirmationException("Authorization server responded negatively");
        }
        // send successful confirmation email - with rabbitmq to email service
        var successfulConfirmationEmail = new EmailDto(List.of(email),
                "User Registration Confirmation Successful",
                "User Account created successfully");

        markTokenAsUsed(verificationToken);

        messagingService.sendToEmailService(successfulConfirmationEmail);
    }

    @Override
    @Transactional
    public void confirmOrganizationUserEmailByUser(String token) {

        LOGGER.trace("confirmOrganizationUserEmail - token: {}", token);

        Jws<Claims> claims = emailTokenService.parseToken(token);

        var verificationToken = processVerificationToken(claims);

        String email = claims.getBody().get(EMAIL, String.class);

        UserProfile user = checkAndGetUserProfile(verificationToken.getUserId(), email);

        OrganizationUserRegistrationRequest registrationRequest = getOrganizationUserRegistrationRequest(email);

        if (!RegistrationStatus.PENDING_EMPLOYEE.equals(registrationRequest.getStatus())) {
            throw new InvalidStatusException("Status should be PENDING_EMPLOYEE");
        }
        registrationRequest.setStatus(RegistrationStatus.PENDING_COMPANY);

        organizationUserRegistrationRequestService.saveOrganizationUserRegistrationRequest(registrationRequest);

        var adminInfoList = authServerGrpcClient
                .getOrganizationAdminInfoFromAuthServer(registrationRequest.getOrganizationRegistrationNumber());

        var emailList = new ArrayList<EmailDto>();

        String emailToken = emailTokenService.createOrganizationUserToken(user.getOrganization(),
                user.getId(),
                user.getEmail(),
                AccountConfirmedBy.ORGANIZATION);

        LOGGER.info("ORG USER TOKEN : {}", emailToken);

        for (OrgAdminInfoResponseDto adminInfo : adminInfoList) {
            String link = String.format("%s/organization/confirm?token=%s", baseUrl(), emailToken);//todo
            //send to email service for the organization to approve
            LOGGER.info("SENDING EMAIL TO : {}", adminInfo.email());//todo remove it later
            emailList.add(new EmailDto(List.of(adminInfo.email()), "User Registration Confirmation", link));
        }
        markTokenAsUsed(verificationToken);
    }

    @Override
    @Transactional
    public void confirmOrganizationUserEmailByOrganization(String token) {

        Jws<Claims> claims = emailTokenService.parseToken(token);

        var verificationToken = processVerificationToken(claims);

        String email = claims.getBody().get(EMAIL, String.class);

        String orgRegistrationNumber = claims.getBody().get("organization", String.class);

        String userId = verificationToken.getUserId();

        checkOrganizationUserProfile(userId, email, orgRegistrationNumber);

        OrganizationUserRegistrationRequest registrationRequest = getOrganizationUserRegistrationRequest(email);

        if (!RegistrationStatus.PENDING_COMPANY.equals(registrationRequest.getStatus())) {
            throw new InvalidStatusException("Status should be PENDING_COMPANY");
        }

        registrationRequest.setStatus(RegistrationStatus.APPROVED);

        organizationUserRegistrationRequestService.saveOrganizationUserRegistrationRequest(registrationRequest);

        //send to auth server that user is authenticated
        var response = authServerGrpcClient.authServerNewUserConfirmation(userId);

        if (ConfirmationStatus.CONFIRMED.equals(response.getConfirmationStatus())) {
            LOGGER.info("EMAIL {} CONFIRMED", email);
            markTokenAsUsed(verificationToken);
            // send successful confirmation email - rabbitmq to email service
        } else {
            LOGGER.info("EMAIL {} CONFIRMATION FAILED", email);
            // throw exception
        }
    }

    @Override
    public void confirmOrganizationAdminEmail(ConfirmOrgAdminEmailRequestDto request, String token) {
        LOGGER.info("CONFIRMING ADMIN ACCOUNT");
        if (!request.newPassword().equals(request.confirmNewPassword())) {
            throw new IllegalArgumentException("Password mismatch");
        }
        Jws<Claims> claims = emailTokenService.parseToken(token);

        String email = claims.getBody().get(EMAIL, String.class);

        var verificationToken = processVerificationToken(claims);

        checkUserProfileDetails(verificationToken.getUserId(), email);

        var response = authServerGrpcClient.confirmAdminAccount(verificationToken.getUserId(), request.oldPassword(), request.newPassword());
        LOGGER.info("response was  : {}", response.getConfirmationStatus());
        markTokenAsUsed(verificationToken);
    }


    private void checkUserProfileDetails(String userId, String email) {
        UserProfile user = userProfileService.findUserProfileById(userId)
                .orElseThrow(() -> new NotFoundException(USER_NOT_FOUND));
        if (!user.getEmail().equals(email)) {
            throw new JwtException("Token mismatch - email");
        }
    }

    private UserProfile checkAndGetUserProfile(String userId, String email) {
        UserProfile user = userProfileService.findUserProfileById(userId)
                .orElseThrow(() -> new NotFoundException(USER_NOT_FOUND));
        if (!user.getEmail().equals(email)) {
            throw new JwtException("Token mismatch - email");
        }
        return user;
    }

    private void checkOrganizationUserProfile(String userId, String email, String orgRegistrationNumber) {
        UserProfile user = userProfileService.findUserProfileById(userId)
                .orElseThrow(() -> new NotFoundException(USER_NOT_FOUND));
        if (email == null || orgRegistrationNumber == null) {
            throw new InvalidTokenException("e-mail and registration number cannot be null");
        }
        if (!user.getEmail().equals(email)) {
            throw new InvalidTokenException("Token email mismatch with database email");
        }
        if (user.getOrganization() == null
                || !orgRegistrationNumber.equals(user.getOrganization().getRegistrationNumber())) {
            throw new InvalidTokenException("Token org registration number different from database org registration number");
        }
    }

    private OrganizationUserRegistrationRequest getOrganizationUserRegistrationRequest(String email) {
        return organizationUserRegistrationRequestService
                .findByOrganizationUserEmail(email)
                .orElseThrow(() -> new NotFoundException("Organization User Registration Request not found"));
    }

    private EmailVerificationToken processVerificationToken(Jws<Claims> claims) {
        Long tokenId = claims.getBody().get(TOKEN_ID, Long.class);
        String jti = claims.getBody().getId();

        var verificationToken = emailVerificationTokenRepository.findById(tokenId)
                .orElseThrow(() -> new NotFoundException("Verification token not found"));

        checkVerificationToken(verificationToken, jti);

        LOGGER.info("Verification token process complete");
        return verificationToken;
    }

    private void checkVerificationToken(EmailVerificationToken token, String jti) {
        LOGGER.info("VERIFYING TOKEN");
        if (!token.getJti().equals(jti)) {
            throw new InvalidTokenException("Invalid token id");
        }
        if (!token.getStatus().equals(EmailVerificationToken.VerificationStatus.PENDING)) {
            throw new InvalidTokenException("Invalid token status");
        }
    }

    private void markTokenAsUsed(EmailVerificationToken token) {
        token.setStatus(EmailVerificationToken.VerificationStatus.USED);
        emailVerificationTokenRepository.save(token);
    }

    private String baseUrl() {//todo remove it
        return "http://localhost";
    }

}
