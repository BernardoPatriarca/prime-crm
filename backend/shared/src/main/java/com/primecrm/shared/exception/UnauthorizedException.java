package com.primecrm.shared.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String message) {
        super("UNAUTHORIZED", HttpStatus.UNAUTHORIZED, message);
    }

    public UnauthorizedException(String errorCode, String message) {
        super(errorCode, HttpStatus.UNAUTHORIZED, message);
    }
}
