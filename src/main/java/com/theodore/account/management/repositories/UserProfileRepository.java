package com.theodore.account.management.repositories;

import com.theodore.account.management.entities.UserProfile;
import org.springframework.data.repository.CrudRepository;

public interface UserProfileRepository  extends CrudRepository<UserProfile, Long> {

    boolean existsByEmailAndMobileNumberAllIgnoreCase(String email, String mobileNumber);

}
