package com.theodore.account.management.services;

import com.theodore.account.management.entities.OrganizationUserRegistrationRequest;
import com.theodore.account.management.entities.UserProfile;
import com.theodore.account.management.enums.RegistrationEmailPurpose;
import com.theodore.account.management.enums.RegistrationStatus;
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

import java.util.Optional;

@Service
public class ConfirmationServiceImpl implements ConfirmationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfirmationServiceImpl.class);

    private static final String PURPOSE = "purpose";
    private static final String EMAIL = "email";


    private final EmailTokenService emailTokenService;
    private final UserProfileService userProfileService;
    private final OrganizationUserRegistrationRequestService organizationUserRegistrationRequestService;
    private final AuthServerGrpcClient authServerGrpcClient;
    private final MessagingService messagingService;

    public ConfirmationServiceImpl(EmailTokenService emailTokenService,
                                   UserProfileService userProfileService,
                                   OrganizationUserRegistrationRequestService organizationUserRegistrationRequestService,
                                   AuthServerGrpcClient authServerGrpcClient,
                                   MessagingService messagingService) {
        this.emailTokenService = emailTokenService;
        this.userProfileService = userProfileService;
        this.organizationUserRegistrationRequestService = organizationUserRegistrationRequestService;
        this.authServerGrpcClient = authServerGrpcClient;
        this.messagingService = messagingService;
    }

    @Override
    public void confirmSimpleUserEmail(String token) {

        Jws<Claims> claims = emailTokenService.parseToken(token);
        String purpose = claims.getBody().get(PURPOSE, String.class);
        if (!RegistrationEmailPurpose.PERSONAL.toString().equals(purpose)) {
            throw new JwtException("Token mismatch");
        }
        String userId = claims.getBody().getSubject();

        String email = claims.getBody().get(EMAIL, String.class);

        checkUserProfileDetails(userId, email);

        //send to auth server that user is authenticated
        var response = authServerGrpcClient.authServerNewUserConfirmation(userId);
        if (!response.getConfirmationStatus().equals(ConfirmationStatus.CONFIRMED)) {
            throw new RuntimeException("confirmation failed");//todo: better exception or no exception
        }
        // send successful confirmation email - with rabbitmq to email service
        var successfulConfirmationEmail = new EmailDto(email,
                "User Registration Confirmation Successful",
                "User Account created successfully");
        messagingService.sendToEmailService(successfulConfirmationEmail);
    }

    @Override
    @Transactional
    public void confirmOrganizationUserEmail(String token) {

        LOGGER.trace("confirmOrganizationUserEmail - token: {}", token);

        Jws<Claims> claims = emailTokenService.parseToken(token);
        String purpose = claims.getBody().get(PURPOSE, String.class);
        if (!RegistrationEmailPurpose.ORGANIZATION_USER.toString().equals(purpose)) {
            throw new JwtException("Token mismatch");
        }
        String userId = claims.getBody().getSubject();
        String email = claims.getBody().get(EMAIL, String.class);

        UserProfile user = checkAndGetUserProfile(userId, email);

        OrganizationUserRegistrationRequest registrationRequest = getOrganizationUserRegistrationRequest(email);

        if (!RegistrationStatus.PENDING_EMPLOYEE.equals(registrationRequest.getStatus())) {
            throw new JwtException("Token mismatch");//todo: change this exception
        }
        registrationRequest.setStatus(RegistrationStatus.PENDING_COMPANY);

        organizationUserRegistrationRequestService.saveOrganizationUserRegistrationRequest(registrationRequest);

        /// //
        String emailToken = emailTokenService.createOrganizationUserToken(user, RegistrationEmailPurpose.ORGANIZATION_ADMIN.toString());
        String link = String.format("%s/organization/confirm?token=%s", baseUrl(), emailToken);//todo
        LOGGER.trace("THE LINK : {}", link);//todo: remove
        //send to email service for the organization to approve
        var confirmationEmail = new EmailDto(registrationRequest.getCompanyEmail(), "User Registration Confirmation", link);
        /// //
    }

    @Override
    public void organizationAdminApprovalRequest(String token) {

        Jws<Claims> claims = emailTokenService.parseToken(token);
        String purpose = claims.getBody().get(PURPOSE, String.class);
        if (!RegistrationEmailPurpose.ORGANIZATION_ADMIN.toString().equals(purpose)) {
            throw new JwtException("Token mismatch");
        }
        String userId = claims.getBody().getSubject();

        String email = claims.getBody().get(EMAIL, String.class);

        checkUserProfileDetails(userId, email);

        OrganizationUserRegistrationRequest registrationRequest = getOrganizationUserRegistrationRequest(email);

        if (!RegistrationStatus.PENDING_COMPANY.equals(registrationRequest.getStatus())) {
            throw new JwtException("Token mismatch");//todo: change this exception
        }

        registrationRequest.setStatus(RegistrationStatus.APPROVED);

        organizationUserRegistrationRequestService.saveOrganizationUserRegistrationRequest(registrationRequest);

        //send to auth server that user is authenticated
        var response = authServerGrpcClient.authServerNewUserConfirmation(userId);

        // send successful confirmation email - rabbitmq to email service if response is successful
    }

    private void checkUserProfileDetails(String userId, String email) {
        UserProfile user = userProfileService.findUserProfileById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (!user.getEmail().equals(email)) {
            throw new JwtException("Token mismatch");//todo: better exception
        }
    }

    private UserProfile checkAndGetUserProfile(String userId, String email) {
        UserProfile user = userProfileService.findUserProfileById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (!user.getEmail().equals(email)) {
            throw new JwtException("Token mismatch");//todo: better exception
        }
        return user;
    }

    private OrganizationUserRegistrationRequest getOrganizationUserRegistrationRequest(String email) {
        Optional<OrganizationUserRegistrationRequest> optionalRegistrationRequest = organizationUserRegistrationRequestService
                .findByOrganizationUserEmail(email);

        if (optionalRegistrationRequest.isEmpty()) {
            throw new JwtException("Token mismatch");//todo: change this exception
        }

        return optionalRegistrationRequest.get();
    }

    private String baseUrl() {//todo remove it
        return "https://your-domain.com";  // or inject via @Value
    }

}
