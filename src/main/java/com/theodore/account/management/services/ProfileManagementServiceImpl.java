package com.theodore.account.management.services;

import com.theodore.account.management.entities.UserProfile;
import com.theodore.account.management.mappers.UserProfileMapper;
import com.theodore.account.management.models.dto.requests.AuthUserManageAccountRequestDto;
import com.theodore.account.management.models.dto.requests.UserChangeInformationRequestDto;
import com.theodore.account.management.repositories.UserProfileRepository;
import com.theodore.account.management.utils.CacheNames;
import com.theodore.infrastructure.common.exceptions.NotFoundException;
import com.theodore.infrastructure.common.exceptions.ReferenceMismatchException;
import com.theodore.infrastructure.common.saga.SagaOrchestrator;
import com.theodore.infrastructure.common.utils.MobilityUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;

@Service
public class ProfileManagementServiceImpl implements ProfileManagementService {

    private static final String SEND_AUTH_USER_ACCOUNT_CHANGES_STEP = "send-auth-user-account-changes";
    private static final String SAVE_USER_PROFILE_STEP = "save-user-profile";

    private final UserProfileRepository userProfileRepository;
    private final SagaCompensationActionService sagaCompensationActionService;
    private final UserProfileMapper userProfileMapper;
    private final AuthServerGrpcClient authServerGrpcClient;

    public ProfileManagementServiceImpl(UserProfileRepository userProfileRepository,
                                        SagaCompensationActionService sagaCompensationActionService,
                                        UserProfileMapper userProfileMapper,
                                        AuthServerGrpcClient authServerGrpcClient) {
        this.userProfileRepository = userProfileRepository;
        this.sagaCompensationActionService = sagaCompensationActionService;
        this.userProfileMapper = userProfileMapper;
        this.authServerGrpcClient = authServerGrpcClient;
    }

    @Override
    public void adminProfileManagement(UserChangeInformationRequestDto requestDto) {

        String oldEmail = MobilityUtils.normalizeEmail(requestDto.getOldEmail());
        String newEmail = MobilityUtils.normalizeEmail(requestDto.getNewEmail());

        final Optional<UserProfile> optionalUserProfile = userProfileRepository.findByEmailIgnoreCase(oldEmail);
        final AtomicReference<String> authUserId = new AtomicReference<>();

        String logMsg = "Admin Account Management";

        var authServerAccManageRequest = new AuthUserManageAccountRequestDto(oldEmail,
                newEmail,
                requestDto.getPhoneNumber(),
                requestDto.getOldPassword(),
                requestDto.getNewPassword()
        );

        new SagaOrchestrator()
                .step(SEND_AUTH_USER_ACCOUNT_CHANGES_STEP,
                        () -> {
                            var authUser = requireNonNull(authServerGrpcClient.manageAuthServerUserAccount(authServerAccManageRequest),
                                    "Auth Server User response is null");
                            authUserId.set(authUser.id());
                        },
                        () -> sagaCompensationActionService.authServerCredentialsRollback(authUserId.get(), newEmail, logMsg)
                )
                .step(SAVE_USER_PROFILE_STEP,
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

                            userProfileRepository.save(userProfile);
                        },
                        () -> {
                        }
                ).run();
    }

    @Override
    @Cacheable(cacheNames = CacheNames.USER_ID_FROM_EMAIL, key = "#id", unless = "#result == null")
    public String getUserIdToCreateDriver(String username) {
        UserProfile userProfile = userProfileRepository.findByEmailIgnoreCase(username)
                .orElseThrow(() -> new NotFoundException("Username not found"));
        return userProfile.getId();
    }


}
