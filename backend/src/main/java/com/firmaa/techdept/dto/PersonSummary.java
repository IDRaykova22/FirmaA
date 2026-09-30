package com.firmaa.techdept.dto;

import com.firmaa.techdept.models.User;

/** Short reference to a user, used inside workstation and project responses. */
public record PersonSummary(Long id, String username, String firstName, String lastName) {

    public static PersonSummary from(User u) {
        return new PersonSummary(u.getId(), u.getUsername(),
                EmployeeResponse.orEmpty(u.getFirstName()), EmployeeResponse.orEmpty(u.getLastName()));
    }
}
