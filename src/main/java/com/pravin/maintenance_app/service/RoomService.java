package com.pravin.maintenance_app.service;

import com.pravin.maintenance_app.ENUM.Role;
import com.pravin.maintenance_app.ENUM.RoomStatus;
import com.pravin.maintenance_app.dto.CreateRoomRequest;
import com.pravin.maintenance_app.entity.Room;
import com.pravin.maintenance_app.entity.User;
import com.pravin.maintenance_app.exception.BusinessValidationException;
import com.pravin.maintenance_app.exception.ResourceNotFoundException;
import com.pravin.maintenance_app.repository.RoomRepository;
import com.pravin.maintenance_app.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final CurrentUserService currentUserService;

    public Room getRoomById(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Room not found with id: " + roomId)
                );
    }

    public Room getRoomByRoomNumber(String roomNumber) {
        return roomRepository.findByRoomNumber(roomNumber)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Room not found: " + roomNumber)
                );
    }

    public List<Room> getAllRooms() {
        return roomRepository.findAll(Sort.by(Sort.Direction.ASC, "roomNumber"));
    }

    @Transactional
    public Room createRoom(CreateRoomRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        if (currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only admins can create rooms");
        }

        String roomNumber = request.getRoomNumber().trim();
        if (roomRepository.existsByRoomNumber(roomNumber)) {
            throw new BusinessValidationException("Room " + roomNumber + " already exists");
        }

        Room room = Room.builder()
                .roomNumber(roomNumber)
                .status(RoomStatus.ACTIVE)
                .maintenanceExempt(request.isMaintenanceExempt())
                .build();

        return roomRepository.save(room);
    }

    @Transactional
    public Room updateRoomExemption(Long roomId, boolean maintenanceExempt) {
        User currentUser = currentUserService.getCurrentUser();
        if (currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only admins can update room exemption");
        }

        Room room = getRoomById(roomId);
        room.setMaintenanceExempt(maintenanceExempt);
        return roomRepository.save(room);
    }
}
