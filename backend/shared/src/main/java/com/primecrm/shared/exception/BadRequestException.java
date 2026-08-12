package com.primecrm.shared.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends ApiException {

    public BadRequestException(String errorCode, String message) {
        super(errorCode, HttpStatus.BAD_REQUEST, message);
    }

    public BadRequestException(String message) {
        super("BAD_REQUEST", HttpStatus.BAD_REQUEST, message);
    }
}
