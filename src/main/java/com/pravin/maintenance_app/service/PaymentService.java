package com.pravin.maintenance_app.service;

import com.pravin.maintenance_app.exception.ResourceNotFoundException;
import com.pravin.maintenance_app.exception.BusinessValidationException;
import com.pravin.maintenance_app.ENUM.PaymentStatus;
import com.pravin.maintenance_app.ENUM.CashPaymentRequestStatus;
import com.pravin.maintenance_app.repository.CashPaymentRequestRepository;
import com.pravin.maintenance_app.repository.RoomRepository;
import com.pravin.maintenance_app.dto.CreatePaymentRequest;
import com.pravin.maintenance_app.entity.Payment;
import com.pravin.maintenance_app.entity.Room;
import com.pravin.maintenance_app.mapper.PaymentMapper;
import com.pravin.maintenance_app.repository.PaymentRepository;
import com.pravin.maintenance_app.security.CurrentUserService;
import com.pravin.maintenance_app.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.pravin.maintenance_app.dto.UpdatePaymentProofRequest;
import com.pravin.maintenance_app.ENUM.Role;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final CashPaymentRequestRepository cashPaymentRequestRepository;
    private final RoomRepository roomRepository;
    private final RoomService roomService;
    private final PaymentMapper paymentMapper;
    private final CurrentUserService currentUserService;
    private final CloudinaryService cloudinaryService;

    @Transactional
    public Payment createPayment(CreatePaymentRequest request) {

        User currentUser = currentUserService.getCurrentUser();

        Room userRoom = currentUser.getRoom();
        if (userRoom == null) {
            throw new BusinessValidationException("User does not have an assigned room");
        }

        // Pessimistic lock on Room to prevent concurrent payment/cash creation race conditions
        Room room = roomRepository.findByIdForUpdate(userRoom.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + userRoom.getId()));

        if (cashPaymentRequestRepository.existsByRoomIdAndStatus(
                room.getId(),
                CashPaymentRequestStatus.PENDING)) {

            throw new BusinessValidationException(
                    "You have a cash payment request awaiting admin verification. You cannot initiate another payment until it is reviewed.");
        }

        if (paymentRepository.existsSubmittedPaymentByRoomId(room.getId())) {
            throw new BusinessValidationException(
                    "You have an online payment awaiting admin verification. You cannot initiate another payment until it is reviewed.");
        }

        // If there is already an incomplete payment intent (proof not yet submitted), reuse/update it
        List<Payment> incompletePayments = paymentRepository.findIncompletePaymentsByRoomId(room.getId());
        if (!incompletePayments.isEmpty()) {
            Payment existingPayment = incompletePayments.get(0);
            existingPayment.setAmount(request.getAmount());
            if (request.getMethod() != null) {
                existingPayment.setMethod(request.getMethod());
            }
            existingPayment.setUpdatedAt(LocalDateTime.now());
            return paymentRepository.save(existingPayment);
        }

        Payment payment = paymentMapper.toEntity(request);

        payment.setRoom(room);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());

        return paymentRepository.save(payment);
    }

    public Payment getPaymentById(Long paymentId) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Payment not found with id: " + paymentId
                        )
                );

        User currentUser = currentUserService.getCurrentUser();

        if (currentUser.getRole() != Role.ADMIN
                && !currentUser.getRoom().getId()
                        .equals(payment.getRoom().getId())) {

            throw new AccessDeniedException(
                    "You are not allowed to access this payment"
            );
        }

        return payment;
    }

    public List<Payment> getPaymentsByRoom(Long roomId) {
        roomService.getRoomById(roomId);

        User currentUser = currentUserService.getCurrentUser();

        if (currentUser.getRole() != Role.ADMIN
                && !currentUser.getRoom().getId().equals(roomId)) {
            throw new AccessDeniedException(
                    "You are not allowed to access payments for this room");
        }

        return paymentRepository.findByRoomId(roomId);
    }

    public List<Payment> getPendingPaymentsByRoom(Long roomId) {

        roomService.getRoomById(roomId);

        return paymentRepository.findByRoomIdAndStatus(
                roomId,
                PaymentStatus.PENDING);
    }

    @Transactional
    public Payment updatePaymentProof(
            Long paymentId,
            UpdatePaymentProofRequest request) {

        Payment payment = getPaymentById(paymentId);

        // Pessimistic lock on Room to prevent concurrency race conditions
        Room room = roomRepository.findByIdForUpdate(payment.getRoom().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + payment.getRoom().getId()));

        if (cashPaymentRequestRepository.existsByRoomIdAndStatus(room.getId(), CashPaymentRequestStatus.PENDING)) {
            throw new BusinessValidationException("You have a cash payment request awaiting admin verification. You cannot submit online payment proof.");
        }

        List<Payment> submitted = paymentRepository.findSubmittedPaymentsByRoomId(room.getId());
        boolean otherSubmitted = submitted.stream().anyMatch(p -> !p.getId().equals(payment.getId()));
        if (otherSubmitted) {
            throw new BusinessValidationException("Another payment for this room is already awaiting admin verification.");
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessValidationException("Only pending payments can have payment proof updated.");
        }

        if ((request.getTransactionReference() == null || request.getTransactionReference().isBlank()) &&
                (request.getScreenshotUrl() == null || request.getScreenshotUrl().isBlank()) &&
                (request.getScreenshotPublicId() == null || request.getScreenshotPublicId().isBlank())) {
            throw new BusinessValidationException(
                    "At least one proof field (transactionReference, screenshotUrl, or screenshotPublicId) must be provided");
        }

        if (request.getTransactionReference() != null && !request.getTransactionReference().isBlank()) {
            payment.setTransactionReference(request.getTransactionReference());
        }

        if (request.getScreenshotUrl() != null && !request.getScreenshotUrl().isBlank()) {
            payment.setScreenshotUrl(request.getScreenshotUrl());
        }

        if (request.getScreenshotPublicId() != null && !request.getScreenshotPublicId().isBlank()) {
            payment.setScreenshotPublicId(request.getScreenshotPublicId());
        }

        payment.setUpdatedAt(LocalDateTime.now());

        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment submitPaymentProof(
            Long paymentId,
            String transactionReference,
            MultipartFile file) {

        if (transactionReference == null || transactionReference.trim().isEmpty()) {
            throw new BusinessValidationException("Transaction reference / UTR is required");
        }

        if (file == null || file.isEmpty()) {
            throw new BusinessValidationException("Payment screenshot file is required");
        }

        // Authenticate resident and verify payment ownership
        Payment payment = getPaymentById(paymentId);

        // Pessimistic lock on Room to serialize submissions and prevent concurrency race conditions
        Room room = roomRepository.findByIdForUpdate(payment.getRoom().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + payment.getRoom().getId()));

        if (cashPaymentRequestRepository.existsByRoomIdAndStatus(room.getId(), CashPaymentRequestStatus.PENDING)) {
            throw new BusinessValidationException("You have a cash payment request awaiting admin verification. You cannot submit online payment proof.");
        }

        List<Payment> submitted = paymentRepository.findSubmittedPaymentsByRoomId(room.getId());
        boolean otherSubmitted = submitted.stream().anyMatch(p -> !p.getId().equals(payment.getId()));
        if (otherSubmitted) {
            throw new BusinessValidationException("Another payment for this room is already awaiting admin verification.");
        }

        // Validate payment status and eligibility before uploading
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessValidationException("Payment proof can only be submitted for PENDING payments");
        }

        // Upload to Cloudinary (validates image type and size <= 10MB)
        Map<String, String> uploadResult = cloudinaryService.uploadImage(file, "payment_proofs");
        String url = uploadResult.get("url");
        String publicId = uploadResult.get("publicId");

        try {
            payment.setTransactionReference(transactionReference.trim());
            payment.setScreenshotUrl(url);
            payment.setScreenshotPublicId(publicId);
            payment.setUpdatedAt(LocalDateTime.now());
            return paymentRepository.save(payment);
        } catch (Exception e) {
            log.error("Failed to save payment proof in database. Attempting Cloudinary rollback for publicId: {}", publicId, e);
            if (publicId != null && !publicId.isBlank()) {
                try {
                    cloudinaryService.deleteImage(publicId);
                } catch (Exception deleteEx) {
                    log.warn("Failed to clean up Cloudinary asset {} after DB error: {}", publicId, deleteEx.getMessage());
                }
            }
            throw e;
        }
    }

    @Transactional
    public Payment verifyPayment(Long paymentId) {

        User currentUser = currentUserService.getCurrentUser();
        if (currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only ADMIN can verify payments");
        }

        Payment payment = getPaymentById(paymentId);

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessValidationException(
                    "Only pending payments can be verified");
        }

        payment.setStatus(PaymentStatus.VERIFIED);
        payment.setVerifiedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());

        return paymentRepository.save(payment);
    }

    public List<Payment> getMyPayments() {

        User user = currentUserService.getCurrentUser();
        Long roomId = user.getRoom().getId();
        return paymentRepository.findByRoomId(roomId);
    }

    public List<Payment> getAllPendingPayments() {
        User currentUser = currentUserService.getCurrentUser();
        if (currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only ADMIN can access all pending payments");
        }

        return paymentRepository.findByStatus(PaymentStatus.PENDING);
    }

    public List<Payment> getAllVerifiedPayments() {
        User currentUser = currentUserService.getCurrentUser();
        if (currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only ADMIN can access all verified payments");
        }

        return paymentRepository.findByStatus(PaymentStatus.VERIFIED);
    }

    @Transactional
    public Payment rejectPayment(Long paymentId) {
        User currentUser = currentUserService.getCurrentUser();
        if (currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only ADMIN can reject payments");
        }

        Payment payment = getPaymentById(paymentId);

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessValidationException(
                    "Only pending payments can be rejected");
        }

        payment.setStatus(PaymentStatus.REJECTED);
        payment.setUpdatedAt(LocalDateTime.now());

        return paymentRepository.save(payment);
    }
}
