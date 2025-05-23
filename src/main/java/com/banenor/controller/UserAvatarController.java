package com.banenor.controller;

import com.banenor.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/users/avatar")
@RequiredArgsConstructor
public class UserAvatarController {

    private final UserService userService;

    @Value("${app.upload.dir:uploads/avatars}")
    private String uploadDir;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<String>> uploadAvatar(
            @RequestParam("userId") Long userId,
            @RequestPart("file") FilePart filePart) {

        // Validate image type
        String contentType = filePart.headers().getContentType() != null
                ? filePart.headers().getContentType().toString()
                : "";
        if (!MimeTypeUtils.IMAGE_JPEG_VALUE.equals(contentType) &&
                !MimeTypeUtils.IMAGE_PNG_VALUE.equals(contentType)) {
            log.warn("Unsupported file type [{}] for user {}", contentType, userId);
            return Mono.just(ResponseEntity
                    .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                    .body("Only PNG and JPEG images are allowed"));
        }

        Path baseDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        String ext = MimeTypeUtils.IMAGE_PNG_VALUE.equals(contentType) ? ".png" : ".jpg";
        String filename = UUID.randomUUID() + ext;
        Path target = baseDir.resolve(filename);

        return Mono.fromCallable(() -> {
                    Files.createDirectories(baseDir);
                    return target;
                })
                .publishOn(Schedulers.boundedElastic())
                .flatMap(path ->
                        DataBufferUtils.write(
                                        filePart.content(),
                                        path,
                                        StandardOpenOption.CREATE_NEW
                                )
                                .then(Mono.just(path))
                )
                .flatMap(path -> {
                    String url = "/uploads/avatars/" + filename;
                    return userService.updateAvatar(userId, url)
                            .thenReturn(url);
                })
                .map(url -> {
                    log.info("Avatar uploaded for user {}: {}", userId, url);
                    return ResponseEntity.ok(url);
                })
                .onErrorResume(FileAlreadyExistsException.class, e -> {
                    log.error("Filename collision saving avatar for user {}: {}", userId, e.getMessage(), e);
                    return Mono.just(ResponseEntity
                            .status(HttpStatus.CONFLICT)
                            .body("Filename collision, please retry"));
                })
                .onErrorResume(IOException.class, e -> {
                    log.error("I/O error uploading avatar for user {}: {}", userId, e.getMessage(), e);
                    return Mono.just(ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Failed to save file"));
                })
                .onErrorResume(e -> {
                    log.error("Unexpected error uploading avatar for user {}: {}", userId, e.getMessage(), e);
                    return Mono.just(ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Unexpected server error"));
                });
    }
}
