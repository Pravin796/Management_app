package com.pravin.maintenance_app.mapper;

import com.pravin.maintenance_app.dto.CreatePaymentRequest;
import com.pravin.maintenance_app.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "room", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "verifiedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Payment toEntity(CreatePaymentRequest request);
}