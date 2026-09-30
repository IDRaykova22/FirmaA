package com.firmaa.techdept.exceptions;

/** A business rule was broken (invalid input, forbidden action). Sent to the client as 400 with the message. */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
