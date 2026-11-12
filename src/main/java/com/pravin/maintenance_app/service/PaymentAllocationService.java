package com.pravin.maintenance_app.service;

import com.pravin.maintenance_app.exception.ResourceNotFoundException;
import com.pravin.maintenance_app.exception.BusinessValidationException;
import com.pravin.maintenance_app.ENUM.MaintenanceStatus;
import com.pravin.maintenance_app.ENUM.PaymentStatus;
import com.pravin.maintenance_app.dto.CreatePaymentAllocationRequest;
import com.pravin.maintenance_app.entity.Maintenance;
import com.pravin.maintenance_app.entity.Payment;
import com.pravin.maintenance_app.entity.PaymentAllocation;
import com.pravin.maintenance_app.mapper.PaymentAllocationMapper;
import com.pravin.maintenance_app.repository.MaintenanceRepository;
import com.pravin.maintenance_app.repository.PaymentAllocationRepository;
import com.pravin.maintenance_app.repository.PaymentRepository;
import com.pravin.maintenance_app.security.CurrentUserService;
import com.pravin.maintenance_app.entity.User;
import com.pravin.maintenance_app.ENUM.Role;
import org.springframework.security.access.AccessDeniedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentAllocationService {

    private final PaymentAllocationRepository paymentAllocationRepository;
    private final PaymentRepository paymentRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final MaintenanceService maintenanceService;
    private final PaymentAllocationMapper paymentAllocationMapper;
    private final CurrentUserService currentUserService;

    @Transactional
    public PaymentAllocation createAllocation(
            CreatePaymentAllocationRequest request
    ) {
        User currentUser = currentUserService.getCurrentUser();
        if (currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only ADMIN can create payment allocations");
        }

        Payment payment = paymentRepository.findById(request.getPaymentId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Payment not found with id: "
                                        + request.getPaymentId()
                        )
                );

        Maintenance maintenance =
                maintenanceService.getMaintenanceById(
                        request.getMaintenanceId()
                );

        if (payment.getStatus() != PaymentStatus.VERIFIED) {
            throw new BusinessValidationException(
                    "Only verified payments can be allocated"
            );
        }

        if (!payment.getRoom().getId()
                .equals(maintenance.getRoom().getId())) {

            throw new BusinessValidationException(
                    "Payment and maintenance belong to different rooms"
            );
        }

        BigDecimal allocatedPaymentAmount =
                getVerifiedAllocatedAmount(payment.getId());

        BigDecimal remainingPaymentAmount =
                payment.getAmount().subtract(allocatedPaymentAmount);

        if (request.getAmount().compareTo(remainingPaymentAmount) > 0) {
            throw new BusinessValidationException(
                    "Allocation amount exceeds remaining payment amount"
            );
        }

        BigDecimal allocatedMaintenanceAmount =
                paymentAllocationRepository
                        .getVerifiedAmountByMaintenanceId(
                                maintenance.getId()
                        );

        BigDecimal remainingMaintenanceAmount =
                maintenance.getAmount()
                        .subtract(allocatedMaintenanceAmount);

        if (request.getAmount().compareTo(remainingMaintenanceAmount) > 0) {
            throw new BusinessValidationException(
                    "Allocation amount exceeds remaining maintenance amount"
            );
        }

        PaymentAllocation allocation =
                paymentAllocationMapper.toEntity(request);

        allocation.setPayment(payment);
        allocation.setMaintenance(maintenance);

        PaymentAllocation savedAllocation =
                paymentAllocationRepository.save(allocation);

        updateMaintenanceStatus(maintenance);

        return savedAllocation;
    }

    public List<PaymentAllocation> getAllocationsByPayment(
            Long paymentId
    ) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));
        User currentUser = currentUserService.getCurrentUser();
        if (currentUser.getRole() != Role.ADMIN
                && (currentUser.getRoom() == null || !currentUser.getRoom().getId().equals(payment.getRoom().getId()))) {
            throw new AccessDeniedException("You are not allowed to access allocations for this payment");
        }

        return paymentAllocationRepository.findByPaymentId(paymentId);
    }

    public List<PaymentAllocation> getAllocationsByMaintenance(
            Long maintenanceId
    ) {
        maintenanceService.getMaintenanceById(maintenanceId);

        return paymentAllocationRepository
                .findByMaintenanceId(maintenanceId);
    }

    @Transactional
    public void autoAllocatePaymentToOldestMaintenance(Payment payment) {
        if (payment == null || payment.getStatus() != PaymentStatus.VERIFIED) {
            return;
        }

        BigDecimal allocatedPaymentAmount = getVerifiedAllocatedAmount(payment.getId());
        BigDecimal remainingPaymentAmount = payment.getAmount().subtract(allocatedPaymentAmount);

        if (remainingPaymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        List<Maintenance> maintenanceList = maintenanceRepository
                .findByRoomIdOrderByBillingMonthAsc(payment.getRoom().getId());

        for (Maintenance maintenance : maintenanceList) {
            if (maintenance.getStatus() == MaintenanceStatus.PAID) {
                continue;
            }

            BigDecimal allocatedMaintenanceAmount = paymentAllocationRepository
                    .getVerifiedAmountByMaintenanceId(maintenance.getId());
            BigDecimal remainingMaintenanceAmount = maintenance.getAmount().subtract(allocatedMaintenanceAmount);

            if (remainingMaintenanceAmount.compareTo(BigDecimal.ZERO) <= 0) {
                updateMaintenanceStatus(maintenance);
                continue;
            }

            BigDecimal allocationAmount = remainingPaymentAmount.min(remainingMaintenanceAmount);

            Optional<PaymentAllocation> existingAllocOpt = paymentAllocationRepository
                    .findByPaymentIdAndMaintenanceId(payment.getId(), maintenance.getId());

            PaymentAllocation allocation;
            if (existingAllocOpt.isPresent()) {
                allocation = existingAllocOpt.get();
                allocation.setAmount(allocation.getAmount().add(allocationAmount));
            } else {
                allocation = PaymentAllocation.builder()
                        .payment(payment)
                        .maintenance(maintenance)
                        .amount(allocationAmount)
                        .build();
            }

            paymentAllocationRepository.save(allocation);
            updateMaintenanceStatus(maintenance);

            remainingPaymentAmount = remainingPaymentAmount.subtract(allocationAmount);
            if (remainingPaymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
        }
    }

    private BigDecimal getVerifiedAllocatedAmount(Long paymentId) {

        return paymentAllocationRepository
                .getVerifiedAmountByPaymentId(paymentId);
    }

    private void updateMaintenanceStatus(
            Maintenance maintenance
    ) {

        BigDecimal paidAmount =
                paymentAllocationRepository
                        .getVerifiedAmountByMaintenanceId(
                                maintenance.getId()
                        );

        int comparison =
                paidAmount.compareTo(maintenance.getAmount());

        if (comparison >= 0) {
            maintenance.setStatus(MaintenanceStatus.PAID);
        } else if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            maintenance.setStatus(
                    MaintenanceStatus.PARTIALLY_PAID
            );
        } else {
            maintenance.setStatus(MaintenanceStatus.PENDING);
        }
        maintenanceRepository.save(maintenance);
    }
}
