package com.pravin.maintenance_app.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.pravin.maintenance_app.exception.BusinessValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CloudinaryServiceTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

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
        MultipartFile emptyFile = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);
        assertThrows(BusinessValidationException.class, () ->
                cloudinaryService.uploadImage(emptyFile, "folder")
        );
    }

    @Test
    void uploadImage_WhenNonImageFile_ThrowsException() {
        MultipartFile textFile = new MockMultipartFile("file", "document.txt", "text/plain", "hello".getBytes());
        BusinessValidationException ex = assertThrows(BusinessValidationException.class, () ->
                cloudinaryService.uploadImage(textFile, "folder")
        );
        assertTrue(ex.getMessage().contains("Only image files"));
    }

    @Test
    void uploadImage_WhenOversizedFile_ThrowsException() {
        byte[] oversizedBytes = new byte[11 * 1024 * 1024]; // 11MB
        MultipartFile largeFile = new MockMultipartFile("file", "huge.jpg", "image/jpeg", oversizedBytes);
        BusinessValidationException ex = assertThrows(BusinessValidationException.class, () ->
                cloudinaryService.uploadImage(largeFile, "folder")
        );
        assertTrue(ex.getMessage().contains("must not exceed 10MB"));
    }

    @Test
    void uploadImage_WhenValidImage_ReturnsUrlAndPublicId() throws IOException {
        MultipartFile validFile = new MockMultipartFile("file", "screenshot.png", "image/png", new byte[]{1, 2, 3});
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), any(Map.class))).thenReturn(Map.of(
                "secure_url", "https://res.cloudinary.com/test/image/upload/v123/screenshot.png",
                "public_id", "payment_proofs/screenshot_abc123"
        ));

        Map<String, String> result = cloudinaryService.uploadImage(validFile, "payment_proofs");

        assertNotNull(result);
        assertEquals("https://res.cloudinary.com/test/image/upload/v123/screenshot.png", result.get("url"));
        assertEquals("payment_proofs/screenshot_abc123", result.get("publicId"));
        verify(uploader).upload(any(byte[].class), eq(Map.of("folder", "payment_proofs")));
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
