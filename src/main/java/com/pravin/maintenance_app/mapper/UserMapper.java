package com.pravin.maintenance_app.mapper;

import com.pravin.maintenance_app.dto.UserRegistrationRequest;
import com.pravin.maintenance_app.dto.UserResponse;
import com.pravin.maintenance_app.entity.User;
import jakarta.validation.Valid;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "room", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toEntity(@Valid UserRegistrationRequest request);

    @Mapping(target = "roomNumber", source = "room.roomNumber")
    UserResponse toResponse(User user);
}