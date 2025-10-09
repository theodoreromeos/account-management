package com.theodore.account.management.enums;

public enum AccountConfirmedBy {

    USER, ORGANIZATION;

    public static AccountConfirmedBy getAccountConfirmedByFromString(String str) {
        return valueOf(str.toUpperCase());
    }
}
