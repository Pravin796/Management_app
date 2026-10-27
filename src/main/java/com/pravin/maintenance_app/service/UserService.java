package com.pravin.maintenance_app.service;

import com.pravin.maintenance_app.exception.ResourceNotFoundException;
import com.pravin.maintenance_app.exception.BusinessValidationException;
import com.pravin.maintenance_app.ENUM.MaintenanceStatus;
import com.pravin.maintenance_app.ENUM.Role;
import com.pravin.maintenance_app.ENUM.UserStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.pravin.maintenance_app.config.MaintenanceProperties;
import com.pravin.maintenance_app.dto.AdminResetPasswordResponse;
import com.pravin.maintenance_app.dto.ChangePasswordRequest;
import com.pravin.maintenance_app.dto.ChangePasswordResponse;
import com.pravin.maintenance_app.dto.UserDashboardResponse;
import com.pravin.maintenance_app.dto.UserRegistrationRequest;
import com.pravin.maintenance_app.dto.UserResponse;
import com.pravin.maintenance_app.entity.Maintenance;
import com.pravin.maintenance_app.entity.Payment;
import com.pravin.maintenance_app.entity.Room;
import com.pravin.maintenance_app.entity.User;
import com.pravin.maintenance_app.mapper.UserMapper;
import com.pravin.maintenance_app.repository.MaintenanceRepository;
import com.pravin.maintenance_app.repository.PaymentAllocationRepository;
import com.pravin.maintenance_app.repository.PaymentRepository;
import com.pravin.maintenance_app.repository.UserRepository;
import com.pravin.maintenance_app.security.CurrentUserService;
import org.springframework.security.access.AccessDeniedException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoomService roomService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;
    private final MaintenanceRepository maintenanceRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;
    private final MaintenanceProperties maintenanceProperties;

    public UserResponse registerUser(@Valid UserRegistrationRequest request) {

        // 1. Check duplicate mobile number
        if (userRepository.existsByMobileNumber(
                request.getMobileNumber())) {

            throw new BusinessValidationException(
                    "Mobile number is already registered"
            );
        }

        // 2. Find room
        Room room = roomService.getRoomByRoomNumber(
                request.getRoomNumber()
        );

        // 3. Check whether room already has an account
        if (userRepository.existsByRoomId(room.getId())) {

            throw new BusinessValidationException(
                    "An account already exists for this room"
            );
        }

        // 4. Map DTO → Entity
        User user = userMapper.toEntity(request);

        // 5. Set business/security-sensitive fields
        user.setRoom(room);
        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );
        user.setRole(Role.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setMustChangePassword(false);

        // 6. Save user
        User savedUser = userRepository.save(user);

        // 7. Entity → Response DTO
        return userMapper.toResponse(savedUser);
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        
        User currentUser = currentUserService.getCurrentUser();
        if (currentUser.getRole() != Role.ADMIN && !currentUser.getId().equals(user.getId())) {
            throw new AccessDeniedException("You are not allowed to access this user");
        }
        
        return userMapper.toResponse(user);
    }

    public List<UserResponse> getAllUsers() {
        User currentUser = currentUserService.getCurrentUser();
        if (currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only admins can view all users");
        }
        return userRepository.findAll().stream()
                .map(userMapper::toResponse)
                .toList();
    }

    public UserResponse getUserByRoomId(Long roomId) {
        User currentUser = currentUserService.getCurrentUser();
        if (currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only admins can view resident details by room");
        }
        return userRepository.findByRoomId(roomId)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("No resident found for room ID: " + roomId));
    }

    public AdminResetPasswordResponse adminResetPassword(Long userId) {
        User currentUser = currentUserService.getCurrentUser();
        if (currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only admins can reset user passwords");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (user.getRole() == Role.ADMIN) {
            throw new BusinessValidationException("Cannot reset password for ADMIN accounts");
        }

        LocalDateTime now = LocalDateTime.now();
        if (user.getPasswordResetAt() != null) {
            long minutesSinceReset = Duration.between(user.getPasswordResetAt(), now).toMinutes();
            if (minutesSinceReset >= 0 && minutesSinceReset < 60) {
                long remainingMinutes = 60 - minutesSinceReset;
                throw new BusinessValidationException(
                        "A temporary password was recently generated for this resident. Please wait "
                                + remainingMinutes + " minute" + (remainingMinutes > 1 ? "s" : "")
                                + " before regenerating to ensure consistent verification."
                );
            }
        }

        String tempPassword = generateTemporaryPassword(10);

        user.setPassword(passwordEncoder.encode(tempPassword));
        user.setMustChangePassword(true);
        user.setPasswordResetAt(now);
        userRepository.save(user);

        return AdminResetPasswordResponse.builder()
                .userId(user.getId())
                .roomNumber(user.getRoom() != null ? user.getRoom().getRoomNumber() : null)
                .temporaryPassword(tempPassword)
                .passwordResetAt(now)
                .message("Password reset successfully. User must change the password after login.")
                .build();
    }

    private String generateTemporaryPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public ChangePasswordResponse changePassword(@Valid ChangePasswordRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessValidationException("Incorrect current password");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessValidationException("New password and confirm password do not match");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BusinessValidationException("New password cannot be the same as the current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        user.setPasswordResetAt(null);
        userRepository.save(user);

        return new ChangePasswordResponse("Password changed successfully.");
    }

    public UserDashboardResponse getUserDashboard() {
        User currentUser = currentUserService.getCurrentUser();
        Room room = currentUser.getRoom();
        if (room == null) {
            throw new BusinessValidationException("User does not have an assigned room");
        }

        BigDecimal configuredRate = maintenanceProperties.getMonthlyAmount() != null
                ? maintenanceProperties.getMonthlyAmount().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2);

        BigDecimal outstandingBalance = BigDecimal.ZERO.setScale(2);
        UserDashboardResponse.CurrentMaintenanceDto currentMaintenanceDto = null;

        if (!room.isMaintenanceExempt()) {
            List<Maintenance> maintenanceList = maintenanceRepository.findByRoomId(room.getId());
            if (!maintenanceList.isEmpty()) {
                Map<Long, BigDecimal> allocatedMap = Collections.emptyMap();
                List<Object[]> allocationRows = paymentAllocationRepository
                        .getVerifiedAmountByRoomIdGroupedByMaintenanceId(room.getId());
                if (allocationRows != null && !allocationRows.isEmpty()) {
                    allocatedMap = new HashMap<>();
                    for (Object[] row : allocationRows) {
                        Long maintenanceId = (Long) row[0];
                        BigDecimal allocatedAmount = row[1] instanceof BigDecimal b ? b : new BigDecimal(row[1].toString());
                        allocatedMap.put(maintenanceId, allocatedAmount);
                    }
                }

                BigDecimal totalOutstanding = BigDecimal.ZERO;
                YearMonth currentMonth = YearMonth.now();

                for (Maintenance m : maintenanceList) {
                    BigDecimal allocated = allocatedMap.getOrDefault(m.getId(), BigDecimal.ZERO);

                    if (m.getStatus() != MaintenanceStatus.PAID) {
                        BigDecimal remaining = m.getAmount().subtract(allocated);
                        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                            totalOutstanding = totalOutstanding.add(remaining);
                        }
                    }

                    if (currentMonth.equals(m.getBillingMonth())) {
                        MaintenanceStatus status = m.getStatus();
                        if (status != MaintenanceStatus.PAID) {
                            if (allocated.compareTo(m.getAmount()) >= 0) {
                                status = MaintenanceStatus.PAID;
                            } else if (allocated.compareTo(BigDecimal.ZERO) > 0) {
                                status = MaintenanceStatus.PARTIALLY_PAID;
                            }
                        }
                        currentMaintenanceDto = new UserDashboardResponse.CurrentMaintenanceDto(
                                m.getBillingMonth(),
                                m.getAmount().setScale(2, RoundingMode.HALF_UP),
                                status
                        );
                    }
                }
                outstandingBalance = totalOutstanding.setScale(2, RoundingMode.HALF_UP);
            }
        }

        UserDashboardResponse.RecentPaymentDto recentPaymentDto = null;
        Optional<Payment> recentPaymentOpt = paymentRepository
                .findFirstByRoomIdOrderByCreatedAtDescIdDesc(room.getId());
        if (recentPaymentOpt.isPresent()) {
            Payment payment = recentPaymentOpt.get();
            BigDecimal paymentAmount = payment.getAmount() != null
                    ? payment.getAmount().setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2);
            recentPaymentDto = new UserDashboardResponse.RecentPaymentDto(
                    payment.getId(),
                    paymentAmount,
                    payment.getMethod(),
                    payment.getStatus(),
                    payment.getCreatedAt()
            );
        }

        return new UserDashboardResponse(
                currentUser.getName(),
                room.getRoomNumber(),
                configuredRate,
                outstandingBalance,
                currentMaintenanceDto,
                recentPaymentDto
        );
    }
}
