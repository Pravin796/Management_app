package com.pravin.maintenance_app.mapper;

import com.pravin.maintenance_app.dto.CreatePaymentAllocationRequest;
import com.pravin.maintenance_app.entity.PaymentAllocation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentAllocationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "payment", ignore = true)
    @Mapping(target = "maintenance", ignore = true)
    PaymentAllocation toEntity(CreatePaymentAllocationRequest request);
}