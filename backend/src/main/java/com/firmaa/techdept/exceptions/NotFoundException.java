package com.firmaa.techdept.exceptions;

/** The requested record doesn't exist. Sent to the client as 404. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
