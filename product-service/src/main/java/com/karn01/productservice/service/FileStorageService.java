package com.karn01.productservice.service;

import com.karn01.productservice.config.FileStorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {
    private final FileStorageProperties properties;

    public String saveFile(MultipartFile file) throws IOException {

        String uploadDir = properties.getUploadDir();

        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

        Path path = Paths.get(uploadDir);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }

        Path filePath = path.resolve(fileName);
        Files.write(filePath, file.getBytes());

        return "/images/" + fileName;
    }

}
