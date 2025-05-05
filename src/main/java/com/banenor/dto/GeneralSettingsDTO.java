package com.banenor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Value;

/**
 * User’s general profile info.
 */
@Value
@Builder
public class GeneralSettingsDTO {

    /**
     * Display name / handle.
     */
    @NotBlank(message = "Username must not be blank")
    String username;

    /**
     * Contact email.
     */
    @NotBlank(message = "Email must not be blank")
    @Email(message = "Must be a valid email address")
    String email;

    /**
     * URL to avatar image.
     */
    String avatarUrl;
}
