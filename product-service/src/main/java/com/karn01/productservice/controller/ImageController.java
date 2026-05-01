package com.karn01.productservice.controller;

import com.karn01.productservice.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/admin/images")
@RequiredArgsConstructor
public class ImageController {

    private final FileStorageService fileStorageService;

    @PostMapping
    public String upload(@RequestParam("file") MultipartFile file) throws Exception {
        return fileStorageService.saveFile(file);
    }
}
