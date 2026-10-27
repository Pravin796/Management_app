package com.pravin.maintenance_app.controller;

import com.pravin.maintenance_app.service.CloudinaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class ImageUploadControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private CloudinaryService cloudinaryService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void testUploadImage_Unauthorized() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "receipt.png",
                MediaType.IMAGE_PNG_VALUE,
                "fake-image-bytes".getBytes()
        );

        mockMvc.perform(multipart("/api/uploads/image").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "9876543210", roles = {"RESIDENT"})
    void testUploadImage_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "receipt.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "valid-jpeg-bytes".getBytes()
        );

        when(cloudinaryService.uploadImage(any(), eq("payment_proofs")))
                .thenReturn(Map.of(
                        "url", "https://res.cloudinary.com/demo/image/upload/v1234/payment_proofs/receipt.jpg",
                        "publicId", "payment_proofs/receipt"
                ));

        mockMvc.perform(multipart("/api/uploads/image")
                        .file(file)
                        .param("folder", "payment_proofs"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value("https://res.cloudinary.com/demo/image/upload/v1234/payment_proofs/receipt.jpg"))
                .andExpect(jsonPath("$.publicId").value("payment_proofs/receipt"));
    }

    @Test
    @WithMockUser(username = "9876543210", roles = {"RESIDENT"})
    void testUploadImage_ServiceThrowsBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "script.sh",
                MediaType.TEXT_PLAIN_VALUE,
                "echo hello".getBytes()
        );

        when(cloudinaryService.uploadImage(any(), any()))
                .thenThrow(new IllegalArgumentException("Only image files are allowed"));

        mockMvc.perform(multipart("/api/uploads/image").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only image files are allowed"));
    }

    @Test
    @WithMockUser(username = "9876543210", roles = {"RESIDENT"})
    void testUploadImage_ServiceThrowsBusinessValidationException() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "oversized.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[100]
        );

        when(cloudinaryService.uploadImage(any(), any()))
                .thenThrow(new com.pravin.maintenance_app.exception.BusinessValidationException("Image file size must not exceed 10MB"));

        mockMvc.perform(multipart("/api/uploads/image").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Image file size must not exceed 10MB"));
    }
}
