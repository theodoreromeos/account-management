package com.theodore.account.management.services;

import com.theodore.account.management.entities.UserProfile;
import com.theodore.account.management.mappers.UserProfileMapper;
import com.theodore.account.management.models.dto.requests.AuthUserManageAccountRequestDto;
import com.theodore.account.management.models.dto.requests.UserChangeInformationRequestDto;
import com.theodore.racingmodel.exceptions.ReferenceMismatchException;
import com.theodore.racingmodel.saga.SagaOrchestrator;
import com.theodore.racingmodel.utils.MobilityUtils;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;

@Service
public class ProfileManagementServiceImpl implements ProfileManagementService {

    private final UserProfileService userProfileService;
    private final SagaCompensationActionService sagaCompensationActionService;
    private final UserProfileMapper userProfileMapper;
    private final AuthServerGrpcClient authServerGrpcClient;

    public ProfileManagementServiceImpl(UserProfileService userProfileService,
                                        SagaCompensationActionService sagaCompensationActionService,
                                        UserProfileMapper userProfileMapper,
                                        AuthServerGrpcClient authServerGrpcClient) {
        this.userProfileService = userProfileService;
        this.sagaCompensationActionService = sagaCompensationActionService;
        this.userProfileMapper = userProfileMapper;
        this.authServerGrpcClient = authServerGrpcClient;
    }

    @Override
    public void adminProfileManagement(UserChangeInformationRequestDto requestDto) {

        String oldEmail = MobilityUtils.normalizeEmail(requestDto.getOldEmail());
        String newEmail = MobilityUtils.normalizeEmail(requestDto.getNewEmail());

        final Optional<UserProfile> optionalUserProfile = userProfileService.findByEmail(oldEmail);
        final AtomicReference<String> authUserId = new AtomicReference<>();

        String logMsg = "Admin Account Management";

        var authServerAccManageRequest = new AuthUserManageAccountRequestDto(oldEmail,
                newEmail,
                requestDto.getPhoneNumber(),
                requestDto.getOldPassword(),
                requestDto.getNewPassword()
        );

        new SagaOrchestrator()
                .step(
                        () -> {
                            var authUser = requireNonNull(authServerGrpcClient.manageAuthServerUserAccount(authServerAccManageRequest),
                                    "Auth Server User response is null");
                            authUserId.set(authUser.id());
                        },
                        () -> sagaCompensationActionService.authServerCredentialsRollback(authUserId.get(), newEmail, logMsg)
                )
                .step(
                        () -> {
                            final var userProfile = optionalUserProfile
                                    .map(userProf -> {
                                        if (!authUserId.get().equals(userProf.getId())) {
                                            throw new ReferenceMismatchException("Id mismatch between auth server and account management");
                                        }
                                        return userProfileMapper.mapUserProfileChangesToEntity(requestDto, userProf);
                                    })
                                    .orElseGet(() -> {
                                        var userProf = userProfileMapper.mapUserProfileChangesToEntity(requestDto, new UserProfile());
                                        userProf.setId(authUserId.get());
                                        return userProf;
                                    });

                            userProfileService.saveUserProfile(userProfile);
                        },
                        () -> {
                        }
                ).run();
    }
}
