package com.banenor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.AssertTrue;

/**
 * Sent by the client when the user clicks the link in their “reset password” email.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetRequest {
    /** The user’s email (for extra safety you can re-verify it). */
    @NotBlank
    @Email
    private String email;

    /**
     * The secure, time-limited token you generated and emailed to the user.
     * Typically a JWT or UUID stored server-side.
     */
    @NotBlank
    private String token;

    /**
     * The new password.
     * Enforce your strength policy here—this example requires 8+ chars, at least one letter & digit.
     */
    @NotBlank
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
            message = "Password must contain letters and numbers"
    )
    private String newPassword;

    /** Repeat for client-side confirmation. */
    @NotBlank
    private String confirmNewPassword;

    @AssertTrue(message = "New password and confirmation must match")
    public boolean isMatching() {
        return newPassword != null && newPassword.equals(confirmNewPassword);
    }
}
