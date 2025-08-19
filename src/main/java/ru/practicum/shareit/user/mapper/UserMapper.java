package ru.practicum.shareit.user.mapper;

import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.exception.ValidationException;

import java.util.List;
import java.util.stream.Collectors;

public class UserMapper {
    public static UserDto toUserDto(User user) {
        if (user == null) {
            throw new ValidationException("User cannot be null");
        }

        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

    public static User toUser(UserDto userDto) {
        if (userDto == null) {
            throw new ValidationException("UserDto cannot be null");
        }

        return User.builder()
                .id(userDto.getId())
                .name(userDto.getName())
                .email(userDto.getEmail())
                .build();
    }

    public static List<UserDto> toUserDtoList(List<User> users) {
        if (users == null) {
            throw new ValidationException("Users list cannot be null");
        }

        return users.stream()
                .map(UserMapper::toUserDto)
                .collect(Collectors.toList());
    }

    public static List<User> toUserList(List<UserDto> userDtos) {
        if (userDtos == null) {
            throw new ValidationException("UserDtos list cannot be null");
        }

        return userDtos.stream()
                .map(UserMapper::toUser)
                .collect(Collectors.toList());
    }
}