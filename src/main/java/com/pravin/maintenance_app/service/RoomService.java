package com.pravin.maintenance_app.service;

import com.pravin.maintenance_app.entity.Room;
import com.pravin.maintenance_app.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;

    public Room getRoomById(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() ->
                        new RuntimeException("Room not found with id: " + roomId)
                );
    }

    public Room getRoomByRoomNumber(String roomNumber) {
        return roomRepository.findByRoomNumber(roomNumber)
                .orElseThrow(() ->
                        new RuntimeException("Room not found: " + roomNumber)
                );
    }

    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }
}