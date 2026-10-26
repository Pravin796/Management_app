package com.pravin.maintenance_app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pravin.maintenance_app.ENUM.RoomStatus;
import com.pravin.maintenance_app.dto.CreateRoomRequest;
import com.pravin.maintenance_app.entity.Room;
import com.pravin.maintenance_app.mapper.RoomMapper;
import com.pravin.maintenance_app.service.RoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class RoomControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private RoomService roomService;

    @MockitoBean
    private RoomMapper roomMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void testGetAllRooms_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/rooms"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @WithMockUser(username = "residentUser", roles = "USER")
    void testGetAllRooms_Authenticated_Returns200() throws Exception {
        Room r = Room.builder().id(1L).roomNumber("101").status(RoomStatus.ACTIVE).build();
        when(roomService.getAllRooms()).thenReturn(List.of(r));

        mockMvc.perform(get("/api/rooms"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "residentUser", roles = "USER")
    void testCreateRoom_ResidentForbidden_Returns403() throws Exception {
        CreateRoomRequest request = CreateRoomRequest.builder()
                .roomNumber("501")
                .maintenanceExempt(false)
                .build();

        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "adminUser", roles = "ADMIN")
    void testCreateRoom_AdminAccess_Returns201() throws Exception {
        CreateRoomRequest request = CreateRoomRequest.builder()
                .roomNumber("501")
                .maintenanceExempt(false)
                .build();

        Room room = Room.builder().id(41L).roomNumber("501").status(RoomStatus.ACTIVE).build();
        when(roomService.createRoom(any(CreateRoomRequest.class))).thenReturn(room);

        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "residentUser", roles = "USER")
    void testUpdateRoomExemption_ResidentForbidden_Returns403() throws Exception {
        mockMvc.perform(patch("/api/rooms/10/exemption")
                        .param("maintenanceExempt", "true"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "adminUser", roles = "ADMIN")
    void testUpdateRoomExemption_AdminAccess_Returns200() throws Exception {
        Room room = Room.builder().id(10L).roomNumber("205").maintenanceExempt(true).build();
        when(roomService.updateRoomExemption(eq(10L), eq(true))).thenReturn(room);

        mockMvc.perform(patch("/api/rooms/10/exemption")
                        .param("maintenanceExempt", "true"))
                .andExpect(status().isOk());
    }
}
