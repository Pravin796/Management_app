package com.pravin.maintenance_app.controller;

import com.pravin.maintenance_app.dto.ApiErrorResponse;
import com.pravin.maintenance_app.dto.CreateRoomRequest;
import com.pravin.maintenance_app.dto.RoomResponse;
import com.pravin.maintenance_app.entity.Room;
import com.pravin.maintenance_app.mapper.RoomMapper;
import com.pravin.maintenance_app.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Rooms", description = "Endpoints for room management")
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private final RoomMapper roomMapper;

    @Operation(summary = "Get all rooms", description = "Retrieves list of all rooms")
    @ApiResponse(responseCode = "200", description = "Returned successfully")
    @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @GetMapping
    public ResponseEntity<List<RoomResponse>> getAllRooms() {
        List<RoomResponse> rooms = roomService.getAllRooms()
                .stream()
                .map(roomMapper::toResponse)
                .toList();
        return ResponseEntity.ok(rooms);
    }

    @Operation(summary = "Get room by id", description = "Retrieves room details by room id")
    @ApiResponse(responseCode = "200", description = "Returned successfully")
    @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @GetMapping("/id/{roomId}")
    public ResponseEntity<RoomResponse> getRoomById(@PathVariable Long roomId) {
        Room room = roomService.getRoomById(roomId);
        return ResponseEntity.ok(roomMapper.toResponse(room));
    }

    @Operation(summary = "Get room by number", description = "Retrieves room details by room number")
    @ApiResponse(responseCode = "200", description = "Returned successfully")
    @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @GetMapping("/{roomNumber}")
    public ResponseEntity<RoomResponse> getRoomByNumber(@PathVariable String roomNumber) {
        Room room = roomService.getRoomByRoomNumber(roomNumber);
        return ResponseEntity.ok(roomMapper.toResponse(room));
    }

    @Operation(summary = "Create room", description = "Admin creates a new room")
    @ApiResponse(responseCode = "201", description = "Room created successfully")
    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(@Valid @RequestBody CreateRoomRequest request) {
        Room room = roomService.createRoom(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(roomMapper.toResponse(room));
    }

    @Operation(summary = "Update room maintenance exemption", description = "Admin updates maintenance exemption for a room")
    @ApiResponse(responseCode = "200", description = "Room exemption updated")
    @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @PatchMapping("/{roomId}/exemption")
    public ResponseEntity<RoomResponse> updateRoomExemption(
            @PathVariable Long roomId,
            @RequestParam boolean maintenanceExempt
    ) {
        Room room = roomService.updateRoomExemption(roomId, maintenanceExempt);
        return ResponseEntity.ok(roomMapper.toResponse(room));
    }
}

