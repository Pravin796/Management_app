package com.pravin.maintenance_app.controller;

import com.pravin.maintenance_app.dto.AdminResetPasswordResponse;
import com.pravin.maintenance_app.dto.UserResponse;
import com.pravin.maintenance_app.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class AdminUserControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void testGetAllUsers_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    @WithMockUser(username = "residentUser", roles = "USER")
    void testGetAllUsers_ResidentForbidden_Returns403() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("You are not allowed to access this resource"));
    }

    @Test
    @WithMockUser(username = "adminUser", roles = "ADMIN")
    void testGetAllUsers_AdminAccess_Returns200() throws Exception {
        UserResponse u1 = new UserResponse();
        u1.setId(10L);
        u1.setName("Alice");
        u1.setRoomNumber("101");

        when(userService.getAllUsers()).thenReturn(List.of(u1));

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].name").value("Alice"))
                .andExpect(jsonPath("$[0].roomNumber").value("101"));
    }

    @Test
    @WithMockUser(username = "residentUser", roles = "USER")
    void testGetUserByRoomId_ResidentForbidden_Returns403() throws Exception {
        mockMvc.perform(get("/api/admin/users/room/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "adminUser", roles = "ADMIN")
    void testGetUserByRoomId_AdminAccess_Returns200() throws Exception {
        UserResponse u = new UserResponse();
        u.setId(10L);
        u.setName("Bob");
        u.setRoomId(1L);
        u.setRoomNumber("101");

        when(userService.getUserByRoomId(1L)).thenReturn(u);

        mockMvc.perform(get("/api/admin/users/room/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Bob"))
                .andExpect(jsonPath("$.roomNumber").value("101"));
    }

    @Test
    @WithMockUser(username = "residentUser", roles = "USER")
    void testResetPassword_ResidentForbidden_Returns403() throws Exception {
        mockMvc.perform(post("/api/admin/users/10/reset-password"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "adminUser", roles = "ADMIN")
    void testResetPassword_AdminAccess_Returns200() throws Exception {
        AdminResetPasswordResponse response = AdminResetPasswordResponse.builder()
                .userId(10L)
                .roomNumber("101")
                .temporaryPassword("TempPass123")
                .message("Password reset successfully. User must change the password after login.")
                .build();

        when(userService.adminResetPassword(10L)).thenReturn(response);

        mockMvc.perform(post("/api/admin/users/10/reset-password"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(10))
                .andExpect(jsonPath("$.roomNumber").value("101"))
                .andExpect(jsonPath("$.temporaryPassword").value("TempPass123"));
    }
}
