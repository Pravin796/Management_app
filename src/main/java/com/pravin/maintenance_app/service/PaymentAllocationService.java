package com.pravin.maintenance_app.service;

import com.pravin.maintenance_app.ENUM.MaintenanceStatus;
import com.pravin.maintenance_app.ENUM.PaymentStatus;
import com.pravin.maintenance_app.dto.CreatePaymentAllocationRequest;
import com.pravin.maintenance_app.entity.Maintenance;
import com.pravin.maintenance_app.entity.Payment;
import com.pravin.maintenance_app.entity.PaymentAllocation;
import com.pravin.maintenance_app.mapper.PaymentAllocationMapper;
import com.pravin.maintenance_app.repository.PaymentAllocationRepository;
import com.pravin.maintenance_app.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentAllocationService {

    private final PaymentAllocationRepository paymentAllocationRepository;
    private final PaymentRepository paymentRepository;
    private final MaintenanceService maintenanceService;
    private final PaymentAllocationMapper paymentAllocationMapper;

    @Transactional
    public PaymentAllocation createAllocation(
            CreatePaymentAllocationRequest request
    ) {

        Payment payment = paymentRepository.findById(request.getPaymentId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Payment not found with id: "
                                        + request.getPaymentId()
                        )
                );

        Maintenance maintenance =
                maintenanceService.getMaintenanceById(
                        request.getMaintenanceId()
                );

        if (payment.getStatus() != PaymentStatus.VERIFIED) {
            throw new RuntimeException(
                    "Only verified payments can be allocated"
            );
        }

        if (!payment.getRoom().getId()
                .equals(maintenance.getRoom().getId())) {

            throw new RuntimeException(
                    "Payment and maintenance belong to different rooms"
            );
        }

        BigDecimal allocatedPaymentAmount =
                getVerifiedAllocatedAmount(payment.getId());

        BigDecimal remainingPaymentAmount =
                payment.getAmount().subtract(allocatedPaymentAmount);

        if (request.getAmount().compareTo(remainingPaymentAmount) > 0) {
            throw new RuntimeException(
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
            throw new RuntimeException(
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
        paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Payment not found with id: " + paymentId
                        )
                );

        return paymentAllocationRepository.findByPaymentId(paymentId);
    }

    public List<PaymentAllocation> getAllocationsByMaintenance(
            Long maintenanceId
    ) {
        maintenanceService.getMaintenanceById(maintenanceId);

        return paymentAllocationRepository
                .findByMaintenanceId(maintenanceId);
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
    }
}