package com.pravin.maintenance_app.service;

import com.pravin.maintenance_app.ENUM.CashPaymentRequestStatus;
import com.pravin.maintenance_app.dto.CreateCashPaymentRequest;
import com.pravin.maintenance_app.entity.CashPaymentRequest;
import com.pravin.maintenance_app.entity.Room;
import com.pravin.maintenance_app.mapper.CashPaymentRequestMapper;
import com.pravin.maintenance_app.repository.CashPaymentRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CashPaymentRequestService {

    private final CashPaymentRequestRepository cashPaymentRequestRepository;
    private final RoomService roomService;
    private final CashPaymentRequestMapper cashPaymentRequestMapper;

    @Transactional
    public CashPaymentRequest createRequest(
            CreateCashPaymentRequest request
    ) {

        Room room = roomService.getRoomById(request.getRoomId());

        List<CashPaymentRequest> pendingRequests =
                cashPaymentRequestRepository.findByRoomIdAndStatus(
                        room.getId(),
                        CashPaymentRequestStatus.PENDING
                );

        if (!pendingRequests.isEmpty()) {
            throw new RuntimeException(
                    "A cash payment request is already pending for this room"
            );
        }

        CashPaymentRequest cashPaymentRequest =
                cashPaymentRequestMapper.toEntity(request);

        cashPaymentRequest.setRoom(room);
        cashPaymentRequest.setStatus(
                CashPaymentRequestStatus.PENDING
        );
        cashPaymentRequest.setCreatedAt(LocalDateTime.now());

        return cashPaymentRequestRepository.save(cashPaymentRequest);
    }

    public CashPaymentRequest getRequestById(Long requestId) {

        return cashPaymentRequestRepository.findById(requestId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Cash payment request not found with id: "
                                        + requestId
                        )
                );
    }

    public List<CashPaymentRequest> getRequestsByRoom(
            Long roomId
    ) {

        roomService.getRoomById(roomId);

        return cashPaymentRequestRepository.findByRoomId(roomId);
    }

    public List<CashPaymentRequest> getPendingRequestsByRoom(
            Long roomId
    ) {

        roomService.getRoomById(roomId);

        return cashPaymentRequestRepository.findByRoomIdAndStatus(
                roomId,
                CashPaymentRequestStatus.PENDING
        );
    }

    public List<CashPaymentRequest> getPendingRequests() {

        return cashPaymentRequestRepository.findByStatus(
                CashPaymentRequestStatus.PENDING
        );
    }
}