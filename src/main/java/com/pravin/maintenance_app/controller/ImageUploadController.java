package com.pravin.maintenance_app.controller;

import com.pravin.maintenance_app.dto.ApiErrorResponse;
import com.pravin.maintenance_app.service.CloudinaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "Image Upload", description = "Endpoints for generic image uploads")
@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class ImageUploadController {

    private final CloudinaryService cloudinaryService;

    @Operation(summary = "Upload image", description = "Uploads an image to Cloudinary and returns URL and public ID")
    @ApiResponse(responseCode = "201", description = "Uploaded successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request (e.g., empty file)", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @PostMapping("/image")
    public ResponseEntity<Map<String, String>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", required = false) String folder
    ) {
        Map<String, String> response = cloudinaryService.uploadImage(file, folder);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
