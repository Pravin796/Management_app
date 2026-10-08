package com.pravin.maintenance_app.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.pravin.maintenance_app.exception.BusinessValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public Map<String, String> uploadImage(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new BusinessValidationException("File must not be null or empty");
        }

        try {
            Map<String, Object> options = new HashMap<>();
            if (folder != null && !folder.isBlank()) {
                options.put("folder", folder);
            }

            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), options);

            Map<String, String> result = new HashMap<>();
            result.put("url", (String) uploadResult.get("secure_url"));
            result.put("publicId", (String) uploadResult.get("public_id"));

            return result;
        } catch (IOException e) {
            log.error("Error uploading image to Cloudinary", e);
            throw new RuntimeException("Failed to upload image. Please try again later.");
        }
    }

    public void deleteImage(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            throw new BusinessValidationException("publicId must not be null or blank");
        }

        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (Exception e) {
            log.error("Error deleting image from Cloudinary with publicId: {}", publicId, e);
            throw new RuntimeException("Failed to delete image. Please try again later.");
        }
    }
}
