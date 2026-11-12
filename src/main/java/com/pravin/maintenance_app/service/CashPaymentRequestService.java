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
import com.pravin.maintenance_app.repository.RoomRepository;
import com.pravin.maintenance_app.security.CurrentUserService;
import com.pravin.maintenance_app.entity.User;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
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
        private final RoomRepository roomRepository;
        private final PaymentAllocationService paymentAllocationService;
        private final MaintenanceService maintenanceService;
        private final CurrentUserService currentUserService;

        @Transactional
        public CashPaymentRequest createRequest(CreateCashPaymentRequest request) {

                User currentUser = currentUserService.getCurrentUser();

                Room userRoom = currentUser.getRoom();
                if (userRoom == null) {
                        throw new BusinessValidationException("User does not have an assigned room");
                }

                // Pessimistic lock on Room row to serialize payment/cash requests and prevent concurrency race conditions
                Room room = roomRepository.findByIdForUpdate(userRoom.getId())
                                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + userRoom.getId()));

                if (cashPaymentRequestRepository.existsByRoomIdAndStatus(
                                room.getId(),
                                CashPaymentRequestStatus.PENDING)) {
                        throw new BusinessValidationException(
                                        "You have a cash payment request awaiting admin verification. You cannot initiate another payment until it is reviewed.");
                }

                if (paymentRepository.existsByRoomIdAndStatus(room.getId(), PaymentStatus.PENDING)) {
                        throw new BusinessValidationException(
                                        "You have an online payment awaiting admin verification. You cannot initiate another payment until it is reviewed.");
                }

                if (room.isMaintenanceExempt()) {
                        throw new BusinessValidationException("Room " + room.getRoomNumber() + " is exempt from maintenance payments.");
                }

                BigDecimal outstandingBalance = maintenanceService.calculateOutstandingBalance(room.getId());
                if (outstandingBalance.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new BusinessValidationException("All maintenance dues are cleared. There are no outstanding dues to pay.");
                }

                if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                        throw new BusinessValidationException("Payment amount must be greater than zero.");
                }

                if (request.getAmount().compareTo(outstandingBalance) > 0) {
                        throw new BusinessValidationException(
                                        "Payment amount of ₹" + request.getAmount() + " exceeds total outstanding maintenance balance of ₹" + outstandingBalance);
                }

                CashPaymentRequest cashPaymentRequest = cashPaymentRequestMapper.toEntity(request);

                cashPaymentRequest.setRoom(room);
                cashPaymentRequest.setStatus(CashPaymentRequestStatus.PENDING);
                cashPaymentRequest.setCreatedAt(LocalDateTime.now());

                return cashPaymentRequestRepository.save(cashPaymentRequest);
        }

        public List<CashPaymentRequest> getMyCashRequests() {
                User currentUser = currentUserService.getCurrentUser();
                Room room = currentUser.getRoom();
                if (room == null) {
                        return List.of();
                }
                return cashPaymentRequestRepository.findByRoomId(room.getId());
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

                User currentUser = currentUserService.getCurrentUser();
                if (currentUser.getRole() != Role.ADMIN) {
                        throw new AccessDeniedException("Only ADMIN can access all pending cash requests");
                }

                return cashPaymentRequestRepository.findByStatus(
                                CashPaymentRequestStatus.PENDING);
        }

        @Transactional
        public CashPaymentRequest verifyRequest(
                        Long requestId,
                        VerifyCashPaymentRequest request) {

                CashPaymentRequest cashPaymentRequest = getRequestById(requestId);

        User currentUser = currentUserService.getCurrentUser();
        if (currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only ADMIN can verify cash requests");
        }

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
                if (request.getAllocations() != null) {
                    for (CashPaymentAllocationRequest allocationRequest : request.getAllocations()) {

                            CreatePaymentAllocationRequest allocation = new CreatePaymentAllocationRequest();

                            allocation.setPaymentId(payment.getId());
                            allocation.setMaintenanceId(
                                            allocationRequest.getMaintenanceId());
                            allocation.setAmount(
                                            allocationRequest.getAmount());

                            paymentAllocationService.createAllocation(allocation);
                    }
                }

                // Auto-allocate any remaining payment amount to oldest outstanding months
                paymentAllocationService.autoAllocatePaymentToOldestMaintenance(payment);

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

        User currentUser = currentUserService.getCurrentUser();
        if (currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only ADMIN can reject cash requests");
        }

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

        public List<CashPaymentRequest> getAllCashRequestsAdmin() {
                User currentUser = currentUserService.getCurrentUser();
                if (currentUser.getRole() != Role.ADMIN) {
                        throw new AccessDeniedException("Only ADMIN can access all cash requests");
                }

                return cashPaymentRequestRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        }

        public List<CashPaymentRequest> getAllRejectedCashRequests() {
                User currentUser = currentUserService.getCurrentUser();
                if (currentUser.getRole() != Role.ADMIN) {
                        throw new AccessDeniedException("Only ADMIN can access rejected cash requests");
                }

                return cashPaymentRequestRepository.findByStatus(CashPaymentRequestStatus.REJECTED);
        }
}
