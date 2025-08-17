package ru.practicum.shareit.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.storage.UserRepository;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.mapper.UserMapper;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    public List<UserDto> getAll() {
        return userRepository.findAll().stream()
                .map(UserMapper::toUserDto)
                .collect(Collectors.toList());
    }

    @Override
    public UserDto getById(Long id) {
        return UserMapper.toUserDto(userRepository.getById(id));
    }

    @Override
    public UserDto create(UserDto userDto) {
        validateUserDto(userDto);
        User user = UserMapper.toUser(userDto);
        return UserMapper.toUserDto(userRepository.save(user));
    }

    @Override
    public UserDto update(Long id, UserDto userDto) {
        if (userDto == null || (userDto.getName() == null && userDto.getEmail() == null)) {
            throw new ValidationException("Не указаны поля для обновления");
        }

        User existingUser = userRepository.getById(id);

        if (userDto.getName() != null) {
            existingUser.setName(userDto.getName());
        }

        if (userDto.getEmail() != null) {
            existingUser.setEmail(userDto.getEmail());
        }

        return UserMapper.toUserDto(userRepository.save(existingUser));
    }

    private void validateUserDto(UserDto userDto) {
        if (userDto.getEmail() == null || userDto.getEmail().isBlank()) {
            throw new ValidationException("Email не может быть пустым");
        }
        if (userDto.getName() == null || userDto.getName().isBlank()) {
            throw new ValidationException("Имя не может быть пустым");
        }
    }

    @Override
    public void delete(Long id) {
        userRepository.deleteById(id);
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.getById(id);
    }
}