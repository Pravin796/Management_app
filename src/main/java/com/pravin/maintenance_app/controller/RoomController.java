package com.pravin.maintenance_app.controller;

import com.pravin.maintenance_app.dto.RoomResponse;
import com.pravin.maintenance_app.entity.Room;
import com.pravin.maintenance_app.mapper.RoomMapper;
import com.pravin.maintenance_app.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import com.pravin.maintenance_app.dto.ApiErrorResponse;

@Tag(name = "Rooms", description = "Endpoints for room management")
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private final RoomMapper roomMapper;

    @Operation(summary = "Get room by number", description = "Retrieves room details by room number")
    @ApiResponse(responseCode = "200", description = "Returned successfully")
    @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @GetMapping("/{roomNumber}")
    public ResponseEntity<RoomResponse> getRoomByNumber(@PathVariable String roomNumber) {
        Room room = roomService.getRoomByRoomNumber(roomNumber);
        RoomResponse response = roomMapper.toResponse(room);
        return ResponseEntity.ok(response);
    }
}
