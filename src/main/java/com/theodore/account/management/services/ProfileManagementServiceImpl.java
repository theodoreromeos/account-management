package com.theodore.account.management.services;

import com.theodore.account.management.entities.UserProfile;
import com.theodore.account.management.mappers.UserProfileMapper;
import com.theodore.account.management.models.dto.requests.AuthUserManageAccountRequestDto;
import com.theodore.account.management.models.dto.requests.UserChangeInformationRequestDto;
import com.theodore.account.management.models.dto.responses.AuthUserIdResponseDto;
import com.theodore.racingmodel.exceptions.ReferenceMismatchException;
import com.theodore.racingmodel.saga.SagaOrchestrator;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

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

        final Optional<UserProfile> optionalUserProfile = userProfileService.findByEmail(requestDto.getOldEmail());
        final AtomicReference<AuthUserIdResponseDto> authUserIdAtomicReference = new AtomicReference<>();

        String logMsg = "Admin Account Management";

        var authServerAccManageRequest = new AuthUserManageAccountRequestDto(requestDto.getOldEmail(),
                requestDto.getNewEmail(),
                requestDto.getPhoneNumber(),
                requestDto.getOldPassword(),
                requestDto.getNewPassword()
        );

        new SagaOrchestrator()
                .step(
                        () -> {
                            var authUser = authServerGrpcClient.manageAuthServerUserAccount(authServerAccManageRequest);
                            authUserIdAtomicReference.set(authUser);
                        },
                        () -> {
                            var authUserResponse = Objects.requireNonNull(authUserIdAtomicReference.get(), "authUser not set");
                            sagaCompensationActionService.authServerCredentialsRollback(authUserResponse.id(), requestDto.getNewEmail(), logMsg);
                        }
                )
                .step(
                        () -> {
                            final var authUserId = Objects.requireNonNull(authUserIdAtomicReference.get(), "authUser not set").id();
                            final var userProfile = optionalUserProfile
                                    .map(userProf -> {
                                        if (!authUserId.equals(userProf.getId())) {
                                            throw new ReferenceMismatchException("Id mismatch between auth server and account management");
                                        }
                                        return userProfileMapper.mapUserProfileChangesToEntity(requestDto, userProf);
                                    })
                                    .orElseGet(() -> {
                                        var userProf = userProfileMapper.mapUserProfileChangesToEntity(requestDto, new UserProfile());
                                        userProf.setId(authUserId);
                                        return userProf;
                                    });

                            userProfileService.saveUserProfile(userProfile);
                        },
                        () -> {
                        }
                );
    }
}
