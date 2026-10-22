package com.pravin.maintenance_app.controller;

import com.pravin.maintenance_app.ENUM.MaintenanceStatus;
import com.pravin.maintenance_app.ENUM.PaymentMethod;
import com.pravin.maintenance_app.ENUM.PaymentStatus;
import com.pravin.maintenance_app.dto.UserDashboardResponse;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class UserControllerTest {

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
    void testGetDashboard_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/users/me/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    @WithMockUser(username = "9876543210")
    void testGetDashboard_Authenticated_Returns200AndDashboard() throws Exception {
        UserDashboardResponse.CurrentMaintenanceDto currentMaintenance =
                new UserDashboardResponse.CurrentMaintenanceDto(
                        YearMonth.of(2026, 10),
                        new BigDecimal("500.00"),
                        MaintenanceStatus.PENDING
                );

        UserDashboardResponse.RecentPaymentDto recentPayment =
                new UserDashboardResponse.RecentPaymentDto(
                        101L,
                        new BigDecimal("500.00"),
                        PaymentMethod.ONLINE,
                        PaymentStatus.VERIFIED,
                        LocalDateTime.of(2026, 10, 15, 10, 30)
                );

        UserDashboardResponse mockResponse = new UserDashboardResponse(
                "Jane Resident",
                "202",
                new BigDecimal("500.00"),
                new BigDecimal("500.00"),
                currentMaintenance,
                recentPayment
        );

        when(userService.getUserDashboard()).thenReturn(mockResponse);

        mockMvc.perform(get("/api/users/me/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.residentName").value("Jane Resident"))
                .andExpect(jsonPath("$.roomNumber").value("202"))
                .andExpect(jsonPath("$.maintenanceRate").value(500.00))
                .andExpect(jsonPath("$.outstandingBalance").value(500.00))
                .andExpect(jsonPath("$.currentMaintenance.billingMonth").value("2026-10"))
                .andExpect(jsonPath("$.currentMaintenance.amount").value(500.00))
                .andExpect(jsonPath("$.currentMaintenance.status").value("PENDING"))
                .andExpect(jsonPath("$.recentPayment.paymentId").value(101))
                .andExpect(jsonPath("$.recentPayment.amount").value(500.00))
                .andExpect(jsonPath("$.recentPayment.method").value("ONLINE"))
                .andExpect(jsonPath("$.recentPayment.status").value("VERIFIED"));
    }
}
