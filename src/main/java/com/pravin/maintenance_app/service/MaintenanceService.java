package com.pravin.maintenance_app.service;

import com.pravin.maintenance_app.exception.ResourceNotFoundException;
import com.pravin.maintenance_app.exception.BusinessValidationException;
import com.pravin.maintenance_app.ENUM.MaintenanceStatus;
import com.pravin.maintenance_app.config.MaintenanceProperties;
import com.pravin.maintenance_app.dto.CreateMaintenanceRequest;
import com.pravin.maintenance_app.entity.Maintenance;
import com.pravin.maintenance_app.entity.Room;
import com.pravin.maintenance_app.entity.User;
import com.pravin.maintenance_app.mapper.MaintenanceMapper;
import com.pravin.maintenance_app.repository.MaintenanceRepository;
import com.pravin.maintenance_app.security.CurrentUserService;
import org.springframework.security.access.AccessDeniedException;
import com.pravin.maintenance_app.ENUM.Role;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MaintenanceService {

        private final MaintenanceProperties maintenanceProperties;

        private final MaintenanceRepository maintenanceRepository;
        private final RoomService roomService;
        private final MaintenanceMapper maintenanceMapper;
        private final CurrentUserService currentUserService;

        public Maintenance getMaintenanceById(Long maintenanceId) {

                Maintenance maintenance = maintenanceRepository.findById(maintenanceId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Maintenance not found with id: " + maintenanceId));

                User currentUser = currentUserService.getCurrentUser();

                if (currentUser.getRole() != Role.ADMIN
                                && !currentUser.getRoom().getId()
                                                .equals(maintenance.getRoom().getId())) {

                        throw new AccessDeniedException(
                                        "You are not allowed to access this maintenance record");
                }

                return maintenance;
        }

        public Maintenance getMaintenanceByRoomAndMonth(
                        Long roomId,
                        YearMonth billingMonth) {

                User currentUser = currentUserService.getCurrentUser();

                if (currentUser.getRole() != Role.ADMIN
                                && !currentUser.getRoom().getId().equals(roomId)) {

                        throw new AccessDeniedException(
                                        "You are not allowed to access maintenance for this room");
                }

                return maintenanceRepository
                                .findByRoomIdAndBillingMonth(roomId, billingMonth)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Maintenance not found for room: "
                                                                + roomId
                                                                + " and month: "
                                                                + billingMonth));
        }

        public Maintenance createMaintenance(
                        CreateMaintenanceRequest request) {
                Room room = roomService.getRoomById(request.getRoomId());

                if (room.isMaintenanceExempt()) {
                        throw new BusinessValidationException(
                                        "Maintenance is exempt for room: "
                                                        + room.getRoomNumber());
                }

                if (maintenanceRepository.existsByRoomIdAndBillingMonth(
                                room.getId(),
                                request.getBillingMonth())) {
                        throw new BusinessValidationException(
                                        "Maintenance already exists for room: "
                                                        + room.getRoomNumber()
                                                        + " and month: "
                                                        + request.getBillingMonth());
                }

                Maintenance maintenance = maintenanceMapper.toEntity(request);

                maintenance.setRoom(room);
                maintenance.setAmount(
                                maintenanceProperties.getMonthlyAmount());
                maintenance.setStatus(MaintenanceStatus.PENDING);

                return maintenanceRepository.save(maintenance);
        }

        public List<Maintenance> getMyMaintenance() {

                User user = currentUserService.getCurrentUser();

                Long roomId = user.getRoom().getId();

                return maintenanceRepository.findByRoomId(roomId);
        }
}
