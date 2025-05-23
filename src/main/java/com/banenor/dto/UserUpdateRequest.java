package com.banenor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UserUpdateRequest {
    @NotBlank(message = "Username must not be blank")
    private String username;
    @Email(message = "Invalid email format")
    private String email;

    /** URL for the user's avatar image. */
    private String avatar;

    /** Basic phone‐number validation */
    @Pattern(regexp = "^\\+?[0-9\\- ]{7,15}$", message = "Invalid phone number format")
    private String phone;
}
