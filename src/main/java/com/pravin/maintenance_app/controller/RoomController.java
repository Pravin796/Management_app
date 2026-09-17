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

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private final RoomMapper roomMapper;

    @GetMapping("/{roomNumber}")
    public ResponseEntity<RoomResponse> getRoomByNumber(@PathVariable String roomNumber) {
        Room room = roomService.getRoomByRoomNumber(roomNumber);
        RoomResponse response = roomMapper.toResponse(room);
        return ResponseEntity.ok(response);
    }
}
