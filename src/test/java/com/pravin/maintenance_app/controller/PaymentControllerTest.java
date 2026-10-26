package com.pravin.maintenance_app.controller;

import com.pravin.maintenance_app.ENUM.PaymentMethod;
import com.pravin.maintenance_app.ENUM.PaymentStatus;
import com.pravin.maintenance_app.dto.PaymentResponse;
import com.pravin.maintenance_app.entity.Payment;
import com.pravin.maintenance_app.entity.Room;
import com.pravin.maintenance_app.mapper.PaymentMapper;
import com.pravin.maintenance_app.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class PaymentControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private PaymentMapper paymentMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    private Payment mockPayment(Long id, Long roomId, PaymentStatus status) {
        Payment p = new Payment();
        p.setId(id);
        p.setStatus(status);
        p.setAmount(new BigDecimal("500.00"));
        Room room = new Room();
        room.setId(roomId);
        room.setRoomNumber("101");
        p.setRoom(room);
        return p;
    }

    private PaymentResponse mockPaymentResponse(Long id, String roomNumber, PaymentStatus status) {
        return PaymentResponse.builder()
                .id(id)
                .roomNumber(roomNumber)
                .amount(new BigDecimal("500.00"))
                .method(PaymentMethod.ONLINE)
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testGetPaymentsByRoom_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/payments/room/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    @WithMockUser(username = "residentUser", roles = "USER")
    void testGetPaymentsByRoom_ResidentOwnRoom_Returns200() throws Exception {
        Payment payment = mockPayment(10L, 1L, PaymentStatus.VERIFIED);
        PaymentResponse response = mockPaymentResponse(10L, "101", PaymentStatus.VERIFIED);

        when(paymentService.getPaymentsByRoom(1L)).thenReturn(List.of(payment));
        when(paymentMapper.toResponse(payment)).thenReturn(response);

        mockMvc.perform(get("/api/payments/room/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].roomNumber").value("101"))
                .andExpect(jsonPath("$[0].status").value("VERIFIED"));
    }

    @Test
    @WithMockUser(username = "residentUser", roles = "USER")
    void testGetPaymentsByRoom_ResidentOtherRoom_Returns403() throws Exception {
        when(paymentService.getPaymentsByRoom(2L))
                .thenThrow(new AccessDeniedException("You are not allowed to access payments for this room"));

        mockMvc.perform(get("/api/payments/room/2"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("You are not allowed to access payments for this room"));
    }

    @Test
    @WithMockUser(username = "adminUser", roles = "ADMIN")
    void testGetPaymentsByRoom_Admin_Returns200() throws Exception {
        Payment payment = mockPayment(10L, 2L, PaymentStatus.VERIFIED);
        PaymentResponse response = mockPaymentResponse(10L, "102", PaymentStatus.VERIFIED);

        when(paymentService.getPaymentsByRoom(2L)).thenReturn(List.of(payment));
        when(paymentMapper.toResponse(payment)).thenReturn(response);

        mockMvc.perform(get("/api/payments/room/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].roomNumber").value("102"));
    }

    @Test
    void testVerifyPayment_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(put("/api/payments/10/verify"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    @WithMockUser(username = "residentUser", roles = "USER")
    void testVerifyPayment_Resident_Returns403() throws Exception {
        mockMvc.perform(put("/api/payments/10/verify"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "adminUser", roles = "ADMIN")
    void testVerifyPayment_Admin_Returns200() throws Exception {
        Payment verified = mockPayment(10L, 1L, PaymentStatus.VERIFIED);
        PaymentResponse response = mockPaymentResponse(10L, "101", PaymentStatus.VERIFIED);

        when(paymentService.verifyPayment(10L)).thenReturn(verified);
        when(paymentMapper.toResponse(verified)).thenReturn(response);

        mockMvc.perform(put("/api/payments/10/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("VERIFIED"));
    }

    @Test
    void testGetPaymentById_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/payments/10"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @WithMockUser(username = "residentUser", roles = "USER")
    void testGetPaymentById_ResidentOwnPayment_Returns200() throws Exception {
        Payment payment = mockPayment(10L, 1L, PaymentStatus.VERIFIED);
        PaymentResponse response = mockPaymentResponse(10L, "101", PaymentStatus.VERIFIED);

        when(paymentService.getPaymentById(10L)).thenReturn(payment);
        when(paymentMapper.toResponse(payment)).thenReturn(response);

        mockMvc.perform(get("/api/payments/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    @WithMockUser(username = "residentUser", roles = "USER")
    void testGetPaymentById_ResidentOtherPayment_Returns403() throws Exception {
        when(paymentService.getPaymentById(10L))
                .thenThrow(new AccessDeniedException("You are not allowed to access this payment"));

        mockMvc.perform(get("/api/payments/10"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("You are not allowed to access this payment"));
    }

    @Test
    @WithMockUser(username = "adminUser", roles = "ADMIN")
    void testGetPaymentById_Admin_Returns200() throws Exception {
        Payment payment = mockPayment(10L, 2L, PaymentStatus.VERIFIED);
        PaymentResponse response = mockPaymentResponse(10L, "102", PaymentStatus.VERIFIED);

        when(paymentService.getPaymentById(10L)).thenReturn(payment);
        when(paymentMapper.toResponse(payment)).thenReturn(response);

        mockMvc.perform(get("/api/payments/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void testGetAllPendingPayments_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/payments/admin/pending"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @WithMockUser(username = "residentUser", roles = "USER")
    void testGetAllPendingPayments_Resident_Returns403() throws Exception {
        mockMvc.perform(get("/api/payments/admin/pending"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "adminUser", roles = "ADMIN")
    void testGetAllPendingPayments_Admin_Returns200() throws Exception {
        Payment payment = mockPayment(10L, 2L, PaymentStatus.PENDING);
        PaymentResponse response = mockPaymentResponse(10L, "102", PaymentStatus.PENDING);

        when(paymentService.getAllPendingPayments()).thenReturn(List.of(payment));
        when(paymentMapper.toResponse(payment)).thenReturn(response);

        mockMvc.perform(get("/api/payments/admin/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void testRejectPayment_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(post("/api/payments/10/reject"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @WithMockUser(username = "residentUser", roles = "USER")
    void testRejectPayment_Resident_Returns403() throws Exception {
        mockMvc.perform(post("/api/payments/10/reject"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "adminUser", roles = "ADMIN")
    void testRejectPayment_Admin_Returns200() throws Exception {
        Payment payment = mockPayment(10L, 2L, PaymentStatus.REJECTED);
        PaymentResponse response = mockPaymentResponse(10L, "102", PaymentStatus.REJECTED);

        when(paymentService.rejectPayment(10L)).thenReturn(payment);
        when(paymentMapper.toResponse(payment)).thenReturn(response);

        mockMvc.perform(post("/api/payments/10/reject"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }
}
