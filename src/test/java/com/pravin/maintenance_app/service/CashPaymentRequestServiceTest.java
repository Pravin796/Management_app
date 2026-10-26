package com.pravin.maintenance_app.service;

import com.pravin.maintenance_app.ENUM.CashPaymentRequestStatus;
import com.pravin.maintenance_app.ENUM.Role;
import com.pravin.maintenance_app.dto.CashPaymentAllocationRequest;
import com.pravin.maintenance_app.dto.CreatePaymentAllocationRequest;
import com.pravin.maintenance_app.dto.VerifyCashPaymentRequest;
import com.pravin.maintenance_app.entity.CashPaymentRequest;
import com.pravin.maintenance_app.entity.Payment;
import com.pravin.maintenance_app.entity.Room;
import com.pravin.maintenance_app.entity.User;
import com.pravin.maintenance_app.mapper.CashPaymentRequestMapper;
import com.pravin.maintenance_app.repository.CashPaymentRequestRepository;
import com.pravin.maintenance_app.repository.PaymentRepository;
import com.pravin.maintenance_app.security.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CashPaymentRequestServiceTest {

    @Mock
    private CashPaymentRequestRepository cashPaymentRequestRepository;

    @Mock
    private CashPaymentRequestMapper cashPaymentRequestMapper;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentAllocationService paymentAllocationService;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private CashPaymentRequestService cashPaymentRequestService;

    @Test
    void testGetAllPendingRequests_AdminAllowed() {
        User admin = new User();
        admin.setRole(Role.ADMIN);

        CashPaymentRequest req = new CashPaymentRequest();
        req.setId(1L);
        req.setStatus(CashPaymentRequestStatus.PENDING);

        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(cashPaymentRequestRepository.findByStatus(CashPaymentRequestStatus.PENDING)).thenReturn(List.of(req));

        List<CashPaymentRequest> results = cashPaymentRequestService.getAllPendingRequests();

        assertNotNull(results);
        assertEquals(1, results.size());
    }

    @Test
    void testGetAllPendingRequests_ResidentForbidden_ThrowsAccessDeniedException() {
        User resident = new User();
        resident.setRole(Role.USER);

        when(currentUserService.getCurrentUser()).thenReturn(resident);

        AccessDeniedException ex = assertThrows(
                AccessDeniedException.class,
                () -> cashPaymentRequestService.getAllPendingRequests()
        );

        assertEquals("Only ADMIN can access all pending cash requests", ex.getMessage());
        verify(cashPaymentRequestRepository, never()).findByStatus(any());
    }

    @Test
    void testVerifyRequest_AdminAllowed_SuccessfullyVerifiesAndAllocates() {
        User admin = new User();
        admin.setRole(Role.ADMIN);

        Room room = new Room();
        room.setId(10L);

        CashPaymentRequest cashReq = new CashPaymentRequest();
        cashReq.setId(1L);
        cashReq.setStatus(CashPaymentRequestStatus.PENDING);
        cashReq.setAmount(new BigDecimal("1000.00"));
        cashReq.setRoom(room);

        CashPaymentAllocationRequest allocReq = new CashPaymentAllocationRequest();
        allocReq.setMaintenanceId(100L);
        allocReq.setAmount(new BigDecimal("1000.00"));

        VerifyCashPaymentRequest verifyRequest = new VerifyCashPaymentRequest();
        verifyRequest.setAllocations(List.of(allocReq));

        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(cashPaymentRequestRepository.findById(1L)).thenReturn(Optional.of(cashReq));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> {
            Payment p = i.getArgument(0);
            p.setId(555L);
            return p;
        });
        when(cashPaymentRequestRepository.save(any(CashPaymentRequest.class))).thenAnswer(i -> i.getArgument(0));

        CashPaymentRequest result = cashPaymentRequestService.verifyRequest(1L, verifyRequest);

        assertNotNull(result);
        assertEquals(CashPaymentRequestStatus.VERIFIED, result.getStatus());
        verify(paymentAllocationService).createAllocation(any(CreatePaymentAllocationRequest.class));
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void testVerifyRequest_ResidentForbidden_ThrowsAccessDeniedException() {
        Room room = new Room();
        room.setId(10L);

        User resident = new User();
        resident.setRole(Role.USER);
        resident.setRoom(room);

        CashPaymentRequest cashReq = new CashPaymentRequest();
        cashReq.setId(1L);
        cashReq.setStatus(CashPaymentRequestStatus.PENDING);
        cashReq.setAmount(new BigDecimal("1000.00"));
        cashReq.setRoom(room);

        when(currentUserService.getCurrentUser()).thenReturn(resident);
        when(cashPaymentRequestRepository.findById(1L)).thenReturn(Optional.of(cashReq));

        VerifyCashPaymentRequest verifyRequest = new VerifyCashPaymentRequest();

        AccessDeniedException ex = assertThrows(
                AccessDeniedException.class,
                () -> cashPaymentRequestService.verifyRequest(1L, verifyRequest)
        );

        assertEquals("Only ADMIN can verify cash requests", ex.getMessage());
        verify(paymentRepository, never()).save(any());
        verify(paymentAllocationService, never()).createAllocation(any());
    }

    @Test
    void testRejectRequest_AdminAllowed() {
        User admin = new User();
        admin.setRole(Role.ADMIN);

        Room room = new Room();
        room.setId(10L);

        CashPaymentRequest cashReq = new CashPaymentRequest();
        cashReq.setId(1L);
        cashReq.setStatus(CashPaymentRequestStatus.PENDING);
        cashReq.setRoom(room);

        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(cashPaymentRequestRepository.findById(1L)).thenReturn(Optional.of(cashReq));
        when(cashPaymentRequestRepository.save(any(CashPaymentRequest.class))).thenAnswer(i -> i.getArgument(0));

        CashPaymentRequest rejected = cashPaymentRequestService.rejectRequest(1L, "Receipt invalid");

        assertNotNull(rejected);
        assertEquals(CashPaymentRequestStatus.REJECTED, rejected.getStatus());
        assertEquals("Receipt invalid", rejected.getAdminNote());
    }

    @Test
    void testRejectRequest_ResidentForbidden_ThrowsAccessDeniedException() {
        Room room = new Room();
        room.setId(10L);

        User resident = new User();
        resident.setRole(Role.USER);
        resident.setRoom(room);

        CashPaymentRequest cashReq = new CashPaymentRequest();
        cashReq.setId(1L);
        cashReq.setStatus(CashPaymentRequestStatus.PENDING);
        cashReq.setRoom(room);

        when(currentUserService.getCurrentUser()).thenReturn(resident);
        when(cashPaymentRequestRepository.findById(1L)).thenReturn(Optional.of(cashReq));

        AccessDeniedException ex = assertThrows(
                AccessDeniedException.class,
                () -> cashPaymentRequestService.rejectRequest(1L, "Note")
        );

        assertEquals("Only ADMIN can reject cash requests", ex.getMessage());
        verify(cashPaymentRequestRepository, never()).save(any());
    }

    @Test
    void testGetRequestById_ResidentOwnRoomAllowed() {
        Room room = new Room();
        room.setId(10L);

        User resident = new User();
        resident.setRole(Role.USER);
        resident.setRoom(room);

        CashPaymentRequest cashReq = new CashPaymentRequest();
        cashReq.setId(1L);
        cashReq.setRoom(room);

        when(currentUserService.getCurrentUser()).thenReturn(resident);
        when(cashPaymentRequestRepository.findById(1L)).thenReturn(Optional.of(cashReq));

        CashPaymentRequest result = cashPaymentRequestService.getRequestById(1L);
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void testGetRequestById_ResidentOtherRoomForbidden_ThrowsAccessDeniedException() {
        Room room = new Room();
        room.setId(10L);

        Room otherRoom = new Room();
        otherRoom.setId(99L);

        User resident = new User();
        resident.setRole(Role.USER);
        resident.setRoom(otherRoom);

        CashPaymentRequest cashReq = new CashPaymentRequest();
        cashReq.setId(1L);
        cashReq.setRoom(room);

        when(currentUserService.getCurrentUser()).thenReturn(resident);
        when(cashPaymentRequestRepository.findById(1L)).thenReturn(Optional.of(cashReq));

        AccessDeniedException ex = assertThrows(
                AccessDeniedException.class,
                () -> cashPaymentRequestService.getRequestById(1L)
        );

        assertEquals("You are not allowed to access this cash payment request", ex.getMessage());
    }

    @Test
    void testGetRequestsByRoom_ResidentOtherRoomForbidden_ThrowsAccessDeniedException() {
        Room myRoom = new Room();
        myRoom.setId(10L);

        User resident = new User();
        resident.setRole(Role.USER);
        resident.setRoom(myRoom);

        when(currentUserService.getCurrentUser()).thenReturn(resident);

        AccessDeniedException ex = assertThrows(
                AccessDeniedException.class,
                () -> cashPaymentRequestService.getRequestsByRoom(99L)
        );

        assertEquals("You are not allowed to access cash payment requests for this room", ex.getMessage());
        verify(cashPaymentRequestRepository, never()).findByRoomId(any());
    }

    @Test
    void testGetRequestsByRoom_AdminAllowed() {
        User admin = new User();
        admin.setRole(Role.ADMIN);

        CashPaymentRequest req = new CashPaymentRequest();
        req.setId(1L);

        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(cashPaymentRequestRepository.findByRoomId(10L)).thenReturn(List.of(req));

        List<CashPaymentRequest> results = cashPaymentRequestService.getRequestsByRoom(10L);

        assertNotNull(results);
        assertEquals(1, results.size());
        verify(cashPaymentRequestRepository).findByRoomId(10L);
    }

    @Test
    void testGetRequestsByRoom_ResidentOwnRoomAllowed() {
        Room myRoom = new Room();
        myRoom.setId(10L);

        User resident = new User();
        resident.setRole(Role.USER);
        resident.setRoom(myRoom);

        CashPaymentRequest req = new CashPaymentRequest();
        req.setId(1L);

        when(currentUserService.getCurrentUser()).thenReturn(resident);
        when(cashPaymentRequestRepository.findByRoomId(10L)).thenReturn(List.of(req));

        List<CashPaymentRequest> results = cashPaymentRequestService.getRequestsByRoom(10L);

        assertNotNull(results);
        assertEquals(1, results.size());
        verify(cashPaymentRequestRepository).findByRoomId(10L);
    }

    @Test
    void testGetRequestById_AdminAllowed() {
        User admin = new User();
        admin.setRole(Role.ADMIN);

        Room room = new Room();
        room.setId(10L);

        CashPaymentRequest cashReq = new CashPaymentRequest();
        cashReq.setId(1L);
        cashReq.setRoom(room);

        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(cashPaymentRequestRepository.findById(1L)).thenReturn(Optional.of(cashReq));

        CashPaymentRequest result = cashPaymentRequestService.getRequestById(1L);
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }
}
