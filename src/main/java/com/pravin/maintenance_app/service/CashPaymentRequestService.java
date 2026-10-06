package com.pravin.maintenance_app.service;

import com.pravin.maintenance_app.exception.ResourceNotFoundException;
import com.pravin.maintenance_app.exception.BusinessValidationException;
import com.pravin.maintenance_app.ENUM.CashPaymentRequestStatus;
import com.pravin.maintenance_app.ENUM.PaymentMethod;
import com.pravin.maintenance_app.ENUM.PaymentStatus;
import com.pravin.maintenance_app.ENUM.Role;
import com.pravin.maintenance_app.dto.CashPaymentAllocationRequest;
import com.pravin.maintenance_app.dto.CreateCashPaymentRequest;
import com.pravin.maintenance_app.dto.CreatePaymentAllocationRequest;
import com.pravin.maintenance_app.dto.VerifyCashPaymentRequest;
import com.pravin.maintenance_app.entity.CashPaymentRequest;
import com.pravin.maintenance_app.entity.Payment;
import com.pravin.maintenance_app.entity.Room;
import com.pravin.maintenance_app.mapper.CashPaymentRequestMapper;
import com.pravin.maintenance_app.repository.CashPaymentRequestRepository;
import com.pravin.maintenance_app.repository.PaymentRepository;
import com.pravin.maintenance_app.security.CurrentUserService;
import com.pravin.maintenance_app.entity.User;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CashPaymentRequestService {

        private final CashPaymentRequestRepository cashPaymentRequestRepository;
        private final CashPaymentRequestMapper cashPaymentRequestMapper;
        private final PaymentRepository paymentRepository;
        private final PaymentAllocationService paymentAllocationService;
        private final CurrentUserService currentUserService;

        @Transactional
        public CashPaymentRequest createRequest(CreateCashPaymentRequest request) {

                User currentUser = currentUserService.getCurrentUser();

                Room room = currentUser.getRoom();

                List<CashPaymentRequest> pendingRequests = cashPaymentRequestRepository.findByRoomIdAndStatus(
                                room.getId(),
                                CashPaymentRequestStatus.PENDING);

                if (!pendingRequests.isEmpty()) {
                        throw new BusinessValidationException(
                                        "A cash payment request is already pending for this room");
                }

                CashPaymentRequest cashPaymentRequest = cashPaymentRequestMapper.toEntity(request);

                cashPaymentRequest.setRoom(room);
                cashPaymentRequest.setStatus(CashPaymentRequestStatus.PENDING);
                cashPaymentRequest.setCreatedAt(LocalDateTime.now());

                return cashPaymentRequestRepository.save(cashPaymentRequest);
        }

        public CashPaymentRequest getRequestById(Long requestId) {

                CashPaymentRequest cashPaymentRequest = cashPaymentRequestRepository.findById(requestId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Cash payment request not found with id: "
                                                                + requestId));

                User currentUser = currentUserService.getCurrentUser();

                if (currentUser.getRole() != Role.ADMIN
                                && !currentUser.getRoom().getId()
                                                .equals(cashPaymentRequest.getRoom().getId())) {

                        throw new AccessDeniedException(
                                        "You are not allowed to access this cash payment request");
                }

                return cashPaymentRequest;
        }

        public List<CashPaymentRequest> getRequestsByRoom(Long roomId) {

                User currentUser = currentUserService.getCurrentUser();

                if (currentUser.getRole() != Role.ADMIN
                                && !currentUser.getRoom().getId().equals(roomId)) {

                        throw new AccessDeniedException(
                                        "You are not allowed to access cash payment requests for this room");
                }

                return cashPaymentRequestRepository.findByRoomId(roomId);
        }

        public List<CashPaymentRequest> getPendingRequestsByRoom(Long roomId) {

                User currentUser = currentUserService.getCurrentUser();

                if (currentUser.getRole() != Role.ADMIN
                                && !currentUser.getRoom().getId().equals(roomId)) {

                        throw new AccessDeniedException(
                                        "You are not allowed to access cash payment requests for this room");
                }

                return cashPaymentRequestRepository.findByRoomIdAndStatus(
                                roomId,
                                CashPaymentRequestStatus.PENDING);
        }

        public List<CashPaymentRequest> getAllPendingRequests() {

                return cashPaymentRequestRepository.findByStatus(
                                CashPaymentRequestStatus.PENDING);
        }

        @Transactional
        public CashPaymentRequest verifyRequest(
                        Long requestId,
                        VerifyCashPaymentRequest request) {

                CashPaymentRequest cashPaymentRequest = getRequestById(requestId);

                if (cashPaymentRequest.getStatus() != CashPaymentRequestStatus.PENDING) {
                        throw new BusinessValidationException(
                                        "Only pending cash payment requests can be verified");
                }

                validateAllocationTotal(
                                cashPaymentRequest.getAmount(),
                                request.getAllocations());

                LocalDateTime now = LocalDateTime.now();

                /*
                 * Create the actual Payment record.
                 */
                Payment payment = new Payment();

                payment.setRoom(cashPaymentRequest.getRoom());
                payment.setAmount(cashPaymentRequest.getAmount());
                payment.setMethod(PaymentMethod.CASH);
                payment.setStatus(PaymentStatus.VERIFIED);
                payment.setCreatedAt(now);
                payment.setUpdatedAt(now);
                payment.setVerifiedAt(now);

                payment = paymentRepository.save(payment);

                /*
                 * Create allocations.
                 */
                for (CashPaymentAllocationRequest allocationRequest : request.getAllocations()) {

                        CreatePaymentAllocationRequest allocation = new CreatePaymentAllocationRequest();

                        allocation.setPaymentId(payment.getId());
                        allocation.setMaintenanceId(
                                        allocationRequest.getMaintenanceId());
                        allocation.setAmount(
                                        allocationRequest.getAmount());

                        paymentAllocationService.createAllocation(allocation);
                }

                /*
                 * Mark cash request as verified.
                 */
                cashPaymentRequest.setStatus(
                                CashPaymentRequestStatus.VERIFIED);

                cashPaymentRequest.setAdminNote(
                                request.getAdminNote());

                cashPaymentRequest.setVerifiedAt(now);

                return cashPaymentRequestRepository.save(cashPaymentRequest);
        }

        @Transactional
        public CashPaymentRequest rejectRequest(
                        Long requestId,
                        String adminNote) {

                CashPaymentRequest cashPaymentRequest = getRequestById(requestId);

                if (cashPaymentRequest.getStatus() != CashPaymentRequestStatus.PENDING) {
                        throw new BusinessValidationException(
                                        "Only pending cash payment requests can be rejected");
                }

                cashPaymentRequest.setStatus(
                                CashPaymentRequestStatus.REJECTED);

                cashPaymentRequest.setAdminNote(adminNote);

                /*
                 * Do not set verifiedAt because the request was rejected.
                 */
                return cashPaymentRequestRepository.save(cashPaymentRequest);
        }

        private void validateAllocationTotal(
                        BigDecimal requestAmount,
                        List<CashPaymentAllocationRequest> allocations) {

                BigDecimal allocationTotal = allocations.stream()
                                .map(CashPaymentAllocationRequest::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                if (allocationTotal.compareTo(requestAmount) != 0) {
                        throw new BusinessValidationException(
                                        "Allocation total must exactly match cash payment amount");
                }
        }
}
