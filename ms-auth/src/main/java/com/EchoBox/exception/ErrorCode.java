package com.EchoBox.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    GENERIC_ERROR("ERR_1000"),
    VALIDATION_ERROR("ERR_1001"),
    INVALID_INPUT("ERR_1002"),
    RESOURCE_NOT_FOUND("ERR_1003"),
    RESOURCE_ALREADY_EXISTS("ERR_1004"),
    USER_NOT_FOUND("ERR_2000"),
    USER_ALREADY_EXISTS("ERR_2001"),
    USER_INVALID_EMAIL("ERR_2002"),
    USER_INVALID_DATA("ERR_2003"),
    DATABASE_ERROR("ERR_8000"),
    DATABASE_CONNECTION_ERROR("ERR_8001"),
    UNAUTHORIZED_ACCESS("ERR_9000"),
    INVALID_TOKEN("ERR_9001"),
    TOKEN_EXPIRED("ERR_9002"),
    INSUFFICIENT_PRIVILEGES("ERR_9003");

    private final String code;

    ErrorCode(String code) {
        this.code = code;
    }
}
