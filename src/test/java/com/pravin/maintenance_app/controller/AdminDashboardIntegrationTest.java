package com.pravin.maintenance_app.controller;

import com.pravin.maintenance_app.ENUM.Role;
import com.pravin.maintenance_app.ENUM.UserStatus;
import com.pravin.maintenance_app.entity.User;
import com.pravin.maintenance_app.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class AdminDashboardIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private CurrentUserService currentUserService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @WithMockUser(username = "adminUser", roles = "ADMIN")
    void testGetAdminDashboard_RealServiceAndDatabase_ReturnsValidMetrics() throws Exception {
        User adminUser = new User();
        adminUser.setId(999L);
        adminUser.setName("Admin Test");
        adminUser.setRole(Role.ADMIN);
        adminUser.setStatus(UserStatus.ACTIVE);
        when(currentUserService.getCurrentUser()).thenReturn(adminUser);

        mockMvc.perform(get("/api/admin/dashboard"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRooms").isNumber())
                .andExpect(jsonPath("$.totalMaintenanceCollected").isNumber())
                .andExpect(jsonPath("$.totalOutstandingBalance").isNumber())
                .andExpect(jsonPath("$.pendingOnlinePayments").isNumber())
                .andExpect(jsonPath("$.pendingCashRequests").isNumber())
                .andExpect(jsonPath("$.verifiedCashPayments").isNumber());
    }

    @Test
    @WithMockUser(username = "residentUser", roles = "USER")
    void testGetAdminDashboard_ResidentForbidden_EndToEnd() throws Exception {
        User residentUser = new User();
        residentUser.setId(888L);
        residentUser.setName("Resident Test");
        residentUser.setRole(Role.USER);
        residentUser.setStatus(UserStatus.ACTIVE);
        when(currentUserService.getCurrentUser()).thenReturn(residentUser);

        mockMvc.perform(get("/api/admin/dashboard"))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("You are not allowed to access this resource"));
    }

    @Test
    void testGetAdminDashboard_Unauthenticated_EndToEnd() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }
}
