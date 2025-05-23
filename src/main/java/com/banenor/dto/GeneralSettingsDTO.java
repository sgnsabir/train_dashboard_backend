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
    @NotBlank(message = "Username must not be blank")
    String username;
    @NotBlank(message = "Email must not be blank")
    @Email(message = "Must be a valid email address")
    String email;
    String avatarUrl;

    String language;
    String timezone;
    String dateFormat;
    private Theme theme;

    public enum Theme {
        LIGHT, DARK, SYSTEM
    }
}
