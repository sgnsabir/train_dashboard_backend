package com.banenor.controller;

import com.banenor.dto.UserDTO;
import com.banenor.dto.UserUpdateRequest;
import com.banenor.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Management", description = "Endpoints for user management")
@Validated
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Get User By ID",
            description = "Retrieve details of a user by their ID (Admin only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User details retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<UserDTO>> getUser(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .doOnError(ex -> log.error("Error retrieving user with id {}: {}", id, ex.getMessage(), ex));
    }

    @Operation(summary = "Get All Users",
            description = "Retrieve a list of all users (Admin only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User list retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Flux<UserDTO> getAllUsers() {
        return userService.getAllUsers()
                .doOnError(ex -> log.error("Error retrieving all users: {}", ex.getMessage(), ex))
                .onErrorResume(e -> Flux.empty());
    }

    @Operation(summary = "Update User",
            description = "Update details for a specific user (Admin only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User updated successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<UserDTO>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        return userService.updateUser(id, request)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .doOnError(ex -> log.error("Error updating user with id {}: {}", id, ex.getMessage(), ex));
    }

    @Operation(summary = "Delete User",
            description = "Delete a specific user by ID (Admin only)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User deleted successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<Void>> deleteUser(@PathVariable Long id) {
        return userService.deleteUser(id)
                .thenReturn(ResponseEntity.noContent().<Void>build())
                .doOnError(ex -> log.error("Error deleting user with id {}: {}", id, ex.getMessage(), ex));
    }

    @Operation(summary = "Update Current User Profile",
            description = "Update email, avatar and phone for the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public Mono<ResponseEntity<UserDTO>> updateCurrentUser(
            @AuthenticationPrincipal Mono<UserDetails> principalMono,
            @Valid @RequestBody UserUpdateRequest request) {

        return principalMono
                .flatMap(principal ->
                        userService.updateOwnProfile(
                                        principal.getUsername(),
                                        request.getEmail(),
                                        request.getAvatar(),
                                        request.getPhone()
                                )
                                .map(ResponseEntity::ok)
                                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).build())
                )
                .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()));
    }
}
