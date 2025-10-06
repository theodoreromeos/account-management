package com.theodore.account.management.exceptions;

public class EmailTokenVerificationFailedException extends RuntimeException {

    public EmailTokenVerificationFailedException() {
        super("Email verification failed");
    }

}
