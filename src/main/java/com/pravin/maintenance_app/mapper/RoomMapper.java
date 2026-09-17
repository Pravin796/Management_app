package com.pravin.maintenance_app.mapper;

import com.pravin.maintenance_app.dto.RoomResponse;
import com.pravin.maintenance_app.entity.Room;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoomMapper {
    RoomResponse toResponse(Room room);
}
