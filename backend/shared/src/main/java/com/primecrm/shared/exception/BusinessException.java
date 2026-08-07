package com.primecrm.shared.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends ApiException {

    public BusinessException(String errorCode, String message) {
        super(errorCode, HttpStatus.UNPROCESSABLE_ENTITY, message);
    }

    public BusinessException(String message) {
        super("BUSINESS_RULE_VIOLATION", HttpStatus.UNPROCESSABLE_ENTITY, message);
    }
}
