package com.pravin.maintenance_app.service;

import com.cloudinary.Cloudinary;
import com.pravin.maintenance_app.exception.BusinessValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class CloudinaryServiceTest {

    @Mock
    private Cloudinary cloudinary;

    private CloudinaryService cloudinaryService;

    @BeforeEach
    void setUp() {
        cloudinaryService = new CloudinaryService(cloudinary);
    }

    @Test
    void uploadImage_WhenFileIsNull_ThrowsException() {
        assertThrows(BusinessValidationException.class, () ->
                cloudinaryService.uploadImage(null, "folder")
        );
    }

    @Test
    void uploadImage_WhenFileIsEmpty_ThrowsException() {
        MultipartFile emptyFile = new MockMultipartFile("file", new byte[0]);
        assertThrows(BusinessValidationException.class, () ->
                cloudinaryService.uploadImage(emptyFile, "folder")
        );
    }

    @Test
    void deleteImage_WhenPublicIdIsNull_ThrowsException() {
        assertThrows(BusinessValidationException.class, () ->
                cloudinaryService.deleteImage(null)
        );
    }

    @Test
    void deleteImage_WhenPublicIdIsBlank_ThrowsException() {
        assertThrows(BusinessValidationException.class, () ->
                cloudinaryService.deleteImage("  ")
        );
    }
}
