package com.theodore.account.management.services;

import com.theodore.account.management.entities.UserProfile;
import com.theodore.account.management.mappers.UserProfileMapper;
import com.theodore.account.management.models.AuthUserManageAccountRequestDto;
import com.theodore.account.management.models.UserChangeInformationRequestDto;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ProfileManagementServiceImpl implements ProfileManagementService {

    private final UserProfileService userProfileService;
    private final UserProfileMapper userProfileMapper;
    private final AuthServerGrpcClient authServerGrpcClient;

    public ProfileManagementServiceImpl(UserProfileService userProfileService,
                                        UserProfileMapper userProfileMapper,
                                        AuthServerGrpcClient authServerGrpcClient) {
        this.userProfileService = userProfileService;
        this.userProfileMapper = userProfileMapper;
        this.authServerGrpcClient = authServerGrpcClient;
    }

    @Override
    @Transactional
    public void adminProfileManagement(UserChangeInformationRequestDto requestDto) {

        Optional<UserProfile> optionalUserProfile = userProfileService.findByEmail(requestDto.getOldEmail());
        
        if (optionalUserProfile.isPresent()) {
            UserProfile userProfile = userProfileMapper.mapUserProfileChangesToEntity(requestDto, optionalUserProfile.get());
            userProfileService.saveUserProfile(userProfile);
        } else {
            var authUser = authServerGrpcClient.manageAuthServerUserAccount(
                    new AuthUserManageAccountRequestDto(requestDto.getOldEmail(),
                            requestDto.getNewEmail(),
                            requestDto.getPhoneNumber(),
                            requestDto.getOldPassword(),
                            requestDto.getNewPassword()
                    ));
            UserProfile userProfile = userProfileMapper.mapUserProfileChangesToEntity(requestDto, new UserProfile());
            userProfile.setId(authUser.id());
            userProfileService.saveUserProfile(userProfile);
        }

    }
}
