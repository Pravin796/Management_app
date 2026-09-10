package com.pravin.maintenance_app.mapper;

import com.pravin.maintenance_app.dto.CreateCashPaymentRequest;
import com.pravin.maintenance_app.entity.CashPaymentRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CashPaymentRequestMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "room", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "adminNote", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "verifiedAt", ignore = true)
    CashPaymentRequest toEntity(CreateCashPaymentRequest request);
}