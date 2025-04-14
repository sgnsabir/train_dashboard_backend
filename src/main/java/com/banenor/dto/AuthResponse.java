package com.banenor.dto;

import lombok.Data;
import java.util.List;

@Data
public class AuthResponse {
    private String token;
    private String username;
    private List<String> roles;
}
