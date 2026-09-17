package com.pravin.maintenance_app.mapper;

import com.pravin.maintenance_app.dto.CreateMaintenanceRequest;
import com.pravin.maintenance_app.dto.MaintenanceResponse;
import com.pravin.maintenance_app.entity.Maintenance;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MaintenanceMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "room", ignore = true)
    @Mapping(target = "amount", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Maintenance toEntity(CreateMaintenanceRequest request);

    @Mapping(target = "roomNumber", source = "room.roomNumber")
    MaintenanceResponse toResponse(Maintenance maintenance);
}