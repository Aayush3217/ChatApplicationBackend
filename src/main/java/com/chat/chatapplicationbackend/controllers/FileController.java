package com.chat.chatapplicationbackend.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@RestController
@RequestMapping("/api/files")
@CrossOrigin("*")
public class FileController {

    private final String uploadDir = "uploads/";

    // Allowed MIME types
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "video/mp4", "video/webm",
            "audio/mpeg", "audio/ogg", "audio/wav",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain"
    );

    @PostMapping("/upload")
    public ResponseEntity<?> upload(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request
    ) throws IOException {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
        }

        String mimeType = file.getContentType();
        if (mimeType == null || !ALLOWED_TYPES.contains(mimeType)) {
            return ResponseEntity.badRequest().body(Map.of("error", "File type not allowed: " + mimeType));
        }

        // Sanitize filename
        String originalName = file.getOriginalFilename();
        String sanitized = originalName != null
                ? originalName.replaceAll("[^a-zA-Z0-9._-]", "_")
                : "file";
        String fileName = System.currentTimeMillis() + "_" + sanitized;

        Path path = Paths.get(uploadDir, fileName);
        Files.createDirectories(path.getParent());
        Files.write(path, file.getBytes());

        String baseUrl = request.getScheme() + "://" +
                request.getServerName() +
                (request.getServerPort() == 80 || request.getServerPort() == 443
                        ? "" : ":" + request.getServerPort());

        String fileUrl = baseUrl + "/uploads/" + fileName;

        return ResponseEntity.ok(Map.of(
                "url", fileUrl,
                "fileName", originalName != null ? originalName : fileName,
                "fileSize", file.getSize(),
                "mimeType", mimeType,
                "type", resolveMessageType(mimeType)
        ));
    }

    private String resolveMessageType(String mimeType) {
        if (mimeType.startsWith("image/")) return "IMAGE";
        if (mimeType.startsWith("video/")) return "VIDEO";
        if (mimeType.startsWith("audio/")) return "AUDIO";
        return "FILE";
    }
}

