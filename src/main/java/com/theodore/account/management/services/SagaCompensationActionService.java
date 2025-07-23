package com.theodore.account.management.services;

public interface SagaCompensationActionService {

    void authServerCredentialsRollback(String authUserId, String email, String logMsg);

}
