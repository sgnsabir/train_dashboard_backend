package com.banenor.controller;

import com.banenor.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
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
     * Directory where avatar images will be stored (configurable via 'app.upload.dir').
     */
    @Value("${app.upload.dir:uploads/avatars}")
    private String uploadDir;

    /**
     * Upload and update a user's avatar.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<String>> uploadAvatar(@RequestParam("userId") Long userId,
                                                     @RequestPart("file") FilePart filePart) {
        // Ensure upload directory exists
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath();
        try {
            Files.createDirectories(uploadPath);
        } catch (IOException ex) {
            log.error("Failed to create upload directory: {}", ex.getMessage(), ex);
            return Mono.just(ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Upload directory error"));
        }

        // Generate unique filename
        String originalFilename = filePart.filename();
        String ext = "";
        int dot = originalFilename.lastIndexOf('.');
        if (dot >= 0) {
            ext = originalFilename.substring(dot);
        }
        String generatedFilename = UUID.randomUUID().toString() + ext;
        Path targetPath = uploadPath.resolve(generatedFilename);
        String avatarUrl = "/uploads/avatars/" + generatedFilename;

        log.info("Uploading avatar for user {} -> {}", userId, targetPath);

        // Write file reactively, then update the user's avatar URL in the DB
        return DataBufferUtils.write(filePart.content(), targetPath, StandardOpenOption.CREATE)
                .subscribeOn(Schedulers.boundedElastic())
                .then(userService.updateAvatar(userId, avatarUrl))
                .map(updatedProfile -> {
                    log.debug("Avatar successfully updated for user {}: {}", userId, avatarUrl);
                    return ResponseEntity.ok(avatarUrl);
                })
                .onErrorResume(ex -> {
                    log.error("Error uploading avatar for user {}: {}", userId, ex.getMessage(), ex);
                    return Mono.just(ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Failed to upload avatar"));
                });
    }
}
