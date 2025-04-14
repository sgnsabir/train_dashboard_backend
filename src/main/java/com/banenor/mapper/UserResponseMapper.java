package com.banenor.mapper;

import com.banenor.model.User;
import com.banenor.dto.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import java.util.Collections;

@Mapper(componentModel = "spring")
public interface UserResponseMapper {

    UserResponseMapper INSTANCE = Mappers.getMapper(UserResponseMapper.class);

    /**
     * Maps a User entity to a UserResponse DTO.
     *
     * @param user the User entity to map
     * @return the corresponding UserResponse DTO, or null if user is null
     */
    default UserResponse mapToUserResponse(User user) {
        if (user == null) {
            return null;
        }
        UserResponse response = new UserResponse();
        response.setId(user.getUserId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        // Wrap role in a list for consistency. Adjust if multiple roles are used.
        response.setRoles(Collections.singletonList(user.getRole()));
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        response.setAvatar(user.getAvatar());
        response.setTwoFactorEnabled(user.getTwoFactorEnabled());
        response.setPhone(user.getPhone());
        return response;
    }
}
