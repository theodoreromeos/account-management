package com.theodore.account.management.services;

import com.theodore.account.management.entities.OrganizationUserRegistrationRequest;
import com.theodore.account.management.entities.UserProfile;
import com.theodore.account.management.enums.RegistrationEmailPurpose;
import com.theodore.account.management.enums.RegistrationStatus;
import com.theodore.racingmodel.exceptions.NotFoundException;
import com.theodore.account.management.repositories.OrganizationUserRegistrationRequestRepository;
import com.theodore.account.management.repositories.UserProfileRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ConfirmationServiceImpl implements ConfirmationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfirmationServiceImpl.class);

    private final EmailTokenServiceImpl emailTokenService;
    private final UserProfileRepository userProfileRepository;
    private final OrganizationUserRegistrationRequestRepository organizationUserRegistrationRequestRepository;

    public ConfirmationServiceImpl(EmailTokenServiceImpl emailTokenService, UserProfileRepository userProfileRepository,
                                   OrganizationUserRegistrationRequestRepository organizationUserRegistrationRequestRepository) {
        this.emailTokenService = emailTokenService;
        this.userProfileRepository = userProfileRepository;
        this.organizationUserRegistrationRequestRepository = organizationUserRegistrationRequestRepository;
    }

    @Override
    public void confirmSimpleUserEmail(String token) {

        LOGGER.trace("confirmSimpleUserEmail -  token: {}", token);

        Jws<Claims> claims = emailTokenService.parseToken(token);
        String purpose = claims.getBody().get("purpose", String.class);
        if (!RegistrationEmailPurpose.PERSONAL.toString().equals(purpose)) {
            throw new JwtException("Token mismatch");
        }
        Long userId = Long.valueOf(claims.getBody().getSubject());//todo : check if having user id here is the best choice
        String emailInToken = claims.getBody().get("email", String.class);

        UserProfile user = userProfileRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!user.getEmail().equals(emailInToken)) {
            throw new JwtException("Token mismatch");
        }

        //send to auth server that user is authenticated
        //user.setEmailVerified(true);
        // send successful confirmation email
    }

    @Override
    public void confirmOrganizationUserEmail(String token) {

        LOGGER.trace("confirmOrganizationUserEmail -  token: {}", token);

        Jws<Claims> claims = emailTokenService.parseToken(token);
        String purpose = claims.getBody().get("purpose", String.class);
        if (!RegistrationEmailPurpose.ORGANIZATION_USER.toString().equals(purpose)) {
            throw new JwtException("Token mismatch");
        }
        Long userId = Long.valueOf(claims.getBody().getSubject());//todo : check if having user id here is the best choice
        String emailInToken = claims.getBody().get("email", String.class);

        UserProfile user = userProfileRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!user.getEmail().equals(emailInToken)) {
            throw new JwtException("Token mismatch");
        }

        Optional<OrganizationUserRegistrationRequest> optionalRegistrationRequest = organizationUserRegistrationRequestRepository
                .findByOrgUserEmail(user.getEmail());

        if (optionalRegistrationRequest.isEmpty()) {
            throw new JwtException("Token mismatch");//todo: change this exception
        }

        OrganizationUserRegistrationRequest registrationRequest = optionalRegistrationRequest.get();

        if (!RegistrationStatus.PENDING_EMPLOYEE.equals(registrationRequest.getStatus())) {
            throw new JwtException("Token mismatch");//todo: change this exception
        }
        registrationRequest.setStatus(RegistrationStatus.PENDING_COMPANY);

        organizationUserRegistrationRequestRepository.save(registrationRequest);

        /// //

        String emailToken = emailTokenService.createToken(user, RegistrationEmailPurpose.ORGANIZATION_ADMIN.toString());
        String link = String.format("%s/organization/confirm?token=%s", baseUrl(), emailToken);//todo

        //send to email service for the organization to approve
        /// //
    }

    @Override
    public void organizationAdminApprovalRequest(String token) {

        Jws<Claims> claims = emailTokenService.parseToken(token);
        String purpose = claims.getBody().get("purpose", String.class);
        if (!RegistrationEmailPurpose.ORGANIZATION_ADMIN.toString().equals(purpose)) {
            throw new JwtException("Token mismatch");
        }
        Long userId = Long.valueOf(claims.getBody().getSubject());//todo : check if having user id here is the best choice
        String emailInToken = claims.getBody().get("email", String.class);

        UserProfile user = userProfileRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!user.getEmail().equals(emailInToken)) {
            throw new JwtException("Token mismatch");
        }

        Optional<OrganizationUserRegistrationRequest> optionalRegistrationRequest = organizationUserRegistrationRequestRepository
                .findByOrgUserEmail(user.getEmail());

        if (optionalRegistrationRequest.isEmpty()) {
            throw new JwtException("Token mismatch");//todo: change this exception
        }

        OrganizationUserRegistrationRequest registrationRequest = optionalRegistrationRequest.get();

        if (!RegistrationStatus.PENDING_COMPANY.equals(registrationRequest.getStatus())) {
            throw new JwtException("Token mismatch");//todo: change this exception
        }

        registrationRequest.setStatus(RegistrationStatus.APPROVED);

        organizationUserRegistrationRequestRepository.save(registrationRequest);

        //send to auth server that user is authenticated
        //user.setEmailVerified(true);

        // send successful confirmation email
    }

    private String baseUrl() {//todo remove it
        return "https://your-domain.com";  // or inject via @Value
    }

}
