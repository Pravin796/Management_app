package com.pravin.maintenance_app.service;

import com.pravin.maintenance_app.ENUM.MaintenanceStatus;
import com.pravin.maintenance_app.config.MaintenanceProperties;
import com.pravin.maintenance_app.dto.CreateMaintenanceRequest;
import com.pravin.maintenance_app.entity.Maintenance;
import com.pravin.maintenance_app.entity.Room;
import com.pravin.maintenance_app.mapper.MaintenanceMapper;
import com.pravin.maintenance_app.repository.MaintenanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.YearMonth;

@Service
@RequiredArgsConstructor
public class MaintenanceService {

        private final MaintenanceProperties maintenanceProperties;

        private final MaintenanceRepository maintenanceRepository;
        private final RoomService roomService;
        private final MaintenanceMapper maintenanceMapper;

        public Maintenance getMaintenanceById(Long maintenanceId) {
                return maintenanceRepository.findById(maintenanceId)
                                .orElseThrow(() -> new com.pravin.maintenance_app.exception.ResourceNotFoundException(
                                                "Maintenance not found with id: " + maintenanceId));
        }

        public Maintenance getMaintenanceByRoomAndMonth(
                        Long roomId,
                        YearMonth billingMonth) {
                return maintenanceRepository
                                .findByRoomIdAndBillingMonth(roomId, billingMonth)
                                .orElseThrow(() -> new com.pravin.maintenance_app.exception.ResourceNotFoundException(
                                                "Maintenance not found for room: "
                                                                + roomId
                                                                + " and month: "
                                                                + billingMonth));
        }

        public Maintenance createMaintenance(
                        CreateMaintenanceRequest request) {
                Room room = roomService.getRoomById(request.getRoomId());

                if (room.isMaintenanceExempt()) {
                        throw new com.pravin.maintenance_app.exception.BusinessValidationException(
                                        "Maintenance is exempt for room: "
                                                        + room.getRoomNumber());
                }

                if (maintenanceRepository.existsByRoomIdAndBillingMonth(
                                room.getId(),
                                request.getBillingMonth())) {
                        throw new com.pravin.maintenance_app.exception.BusinessValidationException(
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
}