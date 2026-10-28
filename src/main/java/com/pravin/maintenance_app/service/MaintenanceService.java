package com.pravin.maintenance_app.service;

import com.pravin.maintenance_app.exception.ResourceNotFoundException;
import com.pravin.maintenance_app.exception.BusinessValidationException;
import com.pravin.maintenance_app.ENUM.MaintenanceStatus;
import com.pravin.maintenance_app.config.MaintenanceProperties;
import com.pravin.maintenance_app.dto.CreateMaintenanceRequest;
import com.pravin.maintenance_app.dto.MaintenanceResponse;
import com.pravin.maintenance_app.entity.Maintenance;
import com.pravin.maintenance_app.entity.Room;
import com.pravin.maintenance_app.entity.User;
import com.pravin.maintenance_app.mapper.MaintenanceMapper;
import com.pravin.maintenance_app.repository.MaintenanceRepository;
import com.pravin.maintenance_app.repository.PaymentAllocationRepository;
import com.pravin.maintenance_app.security.CurrentUserService;
import org.springframework.security.access.AccessDeniedException;
import com.pravin.maintenance_app.ENUM.Role;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MaintenanceService {

        private final MaintenanceProperties maintenanceProperties;

        private final MaintenanceRepository maintenanceRepository;
        private final PaymentAllocationRepository paymentAllocationRepository;
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
                User currentUser = currentUserService.getCurrentUser();
                if (currentUser.getRole() != Role.ADMIN) {
                        throw new AccessDeniedException("Only ADMIN can create maintenance records");
                }

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

        public List<Maintenance> getMaintenanceByRoom(Long roomId) {
                User currentUser = currentUserService.getCurrentUser();

                if (currentUser.getRole() != Role.ADMIN
                                && !currentUser.getRoom().getId().equals(roomId)) {
                        throw new AccessDeniedException(
                                        "You are not allowed to access maintenance for this room");
                }

                return maintenanceRepository.findByRoomId(roomId);
        }

        public MaintenanceResponse toResponse(Maintenance maintenance) {
                MaintenanceResponse response = maintenanceMapper.toResponse(maintenance);
                BigDecimal paid = paymentAllocationRepository.getVerifiedAmountByMaintenanceId(maintenance.getId());
                if (paid == null) {
                        paid = BigDecimal.ZERO;
                }
                if (maintenance.getStatus() == MaintenanceStatus.PAID && paid.compareTo(BigDecimal.ZERO) == 0) {
                        paid = maintenance.getAmount() != null ? maintenance.getAmount() : BigDecimal.ZERO;
                }
                BigDecimal total = maintenance.getAmount() != null ? maintenance.getAmount() : BigDecimal.ZERO;
                BigDecimal remaining = maintenance.getStatus() == MaintenanceStatus.PAID
                                ? BigDecimal.ZERO
                                : total.subtract(paid).max(BigDecimal.ZERO);

                response.setPaidAmount(paid);
                response.setRemainingAmount(remaining);
                response.setTotalAmount(total);
                response.setRemainingBalance(remaining);
                return response;
        }

        public List<MaintenanceResponse> getMaintenanceResponsesByRoom(Long roomId) {
                User currentUser = currentUserService.getCurrentUser();

                if (currentUser.getRole() != Role.ADMIN
                                && !currentUser.getRoom().getId().equals(roomId)) {
                        throw new AccessDeniedException(
                                        "You are not allowed to access maintenance for this room");
                }

                List<Maintenance> maintenanceList = maintenanceRepository.findByRoomId(roomId);
                if (maintenanceList.isEmpty()) {
                        return List.of();
                }

                Map<Long, BigDecimal> allocatedMap = new HashMap<>();
                List<Object[]> allocationRows = paymentAllocationRepository
                                .getVerifiedAmountByRoomIdGroupedByMaintenanceId(roomId);
                if (allocationRows != null && !allocationRows.isEmpty()) {
                        for (Object[] row : allocationRows) {
                                Long maintenanceId = (Long) row[0];
                                BigDecimal allocatedAmount = row[1] instanceof BigDecimal b ? b
                                                : new BigDecimal(row[1].toString());
                                allocatedMap.put(maintenanceId, allocatedAmount);
                        }
                }

                return maintenanceList.stream().map(m -> {
                        MaintenanceResponse response = maintenanceMapper.toResponse(m);
                        BigDecimal paid = allocatedMap.getOrDefault(m.getId(), BigDecimal.ZERO);
                        if (m.getStatus() == MaintenanceStatus.PAID && paid.compareTo(BigDecimal.ZERO) == 0) {
                                paid = m.getAmount() != null ? m.getAmount() : BigDecimal.ZERO;
                        }
                        BigDecimal total = m.getAmount() != null ? m.getAmount() : BigDecimal.ZERO;
                        BigDecimal remaining = m.getStatus() == MaintenanceStatus.PAID
                                        ? BigDecimal.ZERO
                                        : total.subtract(paid).max(BigDecimal.ZERO);

                        response.setPaidAmount(paid);
                        response.setRemainingAmount(remaining);
                        response.setTotalAmount(total);
                        response.setRemainingBalance(remaining);
                        return response;
                }).toList();
        }

        public List<MaintenanceResponse> getMyMaintenanceResponses() {
                User user = currentUserService.getCurrentUser();
                Long roomId = user.getRoom().getId();
                return getMaintenanceResponsesByRoom(roomId);
        }
}
