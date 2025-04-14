package com.banenor.controller;

import com.banenor.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import org.springframework.core.io.buffer.DataBufferUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/users/avatar")
@RequiredArgsConstructor
public class UserAvatarController {

    private final UserService userService;

    /**
     * Directory where avatar images will be stored.
     * This value can be configured via the property "app.upload.dir".
     */
    @Value("${app.upload.dir:uploads/avatars}")
    private String uploadDir;

    /**
     * Endpoint to upload a user's avatar image.
     * Accepts multipart/form-data with parameters 'userId' and 'file'.
     * Saves the file to the upload directory and updates the user's avatar URL.
     *
     * @param userId   the ID of the user whose avatar is being updated.
     * @param filePart the uploaded file.
     * @return a Mono emitting a ResponseEntity with the avatar URL on success.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<String>> uploadAvatar(@RequestParam("userId") Long userId,
                                                     @RequestPart("file") FilePart filePart) {
        // Ensure the upload directory exists (create it if necessary)
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath();
        try {
            Files.createDirectories(uploadPath);
        } catch (IOException ex) {
            log.error("Failed to create upload directory: {}", ex.getMessage(), ex);
            return Mono.just(ResponseEntity.status(500).body("Upload directory error"));
        }

        // Generate a unique filename with the original file extension.
        String originalFilename = filePart.filename();
        String ext = "";
        int lastDot = originalFilename.lastIndexOf('.');
        if (lastDot >= 0) {
            ext = originalFilename.substring(lastDot);
        }
        String generatedFilename = UUID.randomUUID().toString() + ext;
        Path targetPath = uploadPath.resolve(generatedFilename);

        log.info("Uploading avatar for user {} to {}", userId, targetPath);

        // Write the file contents to the target path.
        // DataBufferUtils.write returns a Flux<DataBuffer>; we use then() to wait for completion.
        return DataBufferUtils.write(filePart.content(), targetPath, StandardOpenOption.CREATE)
                .subscribeOn(Schedulers.boundedElastic())
                .then(Mono.defer(() -> {
                    // Construct the URL for accessing the uploaded avatar.
                    // In production, consider serving static files via a CDN or dedicated file server.
                    String avatarUrl = "/uploads/avatars/" + generatedFilename;
                    // Update the user's avatar URL using the UserService.
                    return userService.updateAvatar(userId, avatarUrl)
                            .map(updatedUser -> {
                                log.debug("Avatar updated for user {}: {}", userId, avatarUrl);
                                return ResponseEntity.ok(avatarUrl);
                            });
                }))
                .onErrorResume(ex -> {
                    log.error("Error uploading avatar for user {}: {}", userId, ex.getMessage(), ex);
                    return Mono.just(ResponseEntity.status(500).body("Failed to upload avatar"));
                });
    }
}
