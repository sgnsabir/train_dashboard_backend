package com.banenor.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Value;

/**
 * Security toggles like two-factor auth.
 */
@Value
@Builder
public class SecuritySettingsDTO {

    /**
     * Is two-factor (SMS) enabled?
     */
    boolean twoFactorEnabled;

    /**
     * Phone number for 2FA SMS.
     * Must be e.g. +1234567890
     */
    @Pattern(
            regexp    = "^\\+?[0-9]{7,15}$",
            message   = "Phone number must be between 7 and 15 digits, optional leading +"
    )
    String phoneNumber;
}
