package com.chat.chatapplicationbackend.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@CrossOrigin("*")
public class FileController {

    private final String uploadDir = "uploads/";

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file, HttpServletRequest request)
            throws IOException {

        if(file.isEmpty()){
            return ResponseEntity.badRequest().body("File is empty");
        }

        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();

        Path path = Paths.get(uploadDir, fileName);

        // ✅ create folder safely
        Files.createDirectories(path.getParent());

        Files.write(path, file.getBytes());

//        String fileUrl = "http://localhost:8080/uploads/" + fileName;

        // ✅ Dynamic base URL
        String baseUrl = request.getScheme() + "://" +
                request.getServerName() +
                (request.getServerPort() == 80 || request.getServerPort() == 443
                        ? ""
                        : ":" + request.getServerPort());

        String fileUrl = baseUrl + "/uploads/" + fileName;

        return ResponseEntity.ok(Map.of("url", fileUrl));
    }
}
