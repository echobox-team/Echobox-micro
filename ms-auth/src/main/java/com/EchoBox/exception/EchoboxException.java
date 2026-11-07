package com.EchoBox.exception;

import lombok.Getter;

@Getter
public class EchoboxException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Object[] args;

    public EchoboxException(ErrorCode errorCode) {
        super(errorCode.getCode());
        this.errorCode = errorCode;
        this.args = null;
    }

    public EchoboxException(ErrorCode errorCode, Object... args) {
        super(errorCode.getCode());
        this.errorCode = errorCode;
        this.args = args;
    }
}
