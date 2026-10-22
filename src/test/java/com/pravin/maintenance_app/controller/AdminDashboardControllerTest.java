package com.pravin.maintenance_app.controller;

import com.pravin.maintenance_app.dto.AdminDashboardResponse;
import com.pravin.maintenance_app.service.AdminDashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class AdminDashboardControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private AdminDashboardService adminDashboardService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void testGetAdminDashboard_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    @WithMockUser(username = "residentUser", roles = "USER")
    void testGetAdminDashboard_ResidentForbidden_Returns403() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("You are not allowed to access this resource"));
    }

    @Test
    @WithMockUser(username = "adminUser", roles = "ADMIN")
    void testGetAdminDashboard_AdminAccess_Returns200AndDashboard() throws Exception {
        AdminDashboardResponse mockResponse = new AdminDashboardResponse(
                15L,
                new BigDecimal("5000.00"),
                new BigDecimal("2500.00"),
                3L,
                2L,
                7L
        );

        when(adminDashboardService.getAdminDashboard()).thenReturn(mockResponse);

        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRooms").value(15))
                .andExpect(jsonPath("$.totalMaintenanceCollected").value(5000.00))
                .andExpect(jsonPath("$.totalOutstandingBalance").value(2500.00))
                .andExpect(jsonPath("$.pendingOnlinePayments").value(3))
                .andExpect(jsonPath("$.pendingCashRequests").value(2))
                .andExpect(jsonPath("$.verifiedCashPayments").value(7));
    }
}
