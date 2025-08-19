package ru.practicum.shareit.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.mapper.UserMapper;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.storage.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    public List<UserDto> getAll() {
        return UserMapper.toUserDtoList(userRepository.findAll());
    }

    @Override
    public UserDto getById(Long id) {
        if (id == null) {
            throw new ValidationException("User ID cannot be null");
        }

        return UserMapper.toUserDto(userRepository.getById(id));
    }

    @Override
    @Transactional
    public UserDto create(UserDto userDto) {
        if (userDto == null) {
            throw new ValidationException("UserDto cannot be null");
        }

        validateUserDto(userDto);
        User user = UserMapper.toUser(userDto);
        return UserMapper.toUserDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserDto update(Long id, UserDto userDto) {
        if (id == null) {
            throw new ValidationException("User ID cannot be null");
        }

        if (userDto == null) {
            throw new ValidationException("UserDto cannot be null");
        }

        if (userDto.getName() == null && userDto.getEmail() == null) {
            throw new ValidationException("Не указаны поля для обновления");
        }

        User existingUser = userRepository.getById(id);

        if (userDto.getName() != null) {
            existingUser.setName(userDto.getName());
        }

        if (userDto.getEmail() != null && !existingUser.getEmail().equals(userDto.getEmail())) {
            if (userRepository.findByEmail(userDto.getEmail()).isPresent()) {
                throw new ConflictException("Email уже используется другим пользователем");
            }
            existingUser.setEmail(userDto.getEmail());
        }

        return UserMapper.toUserDto(userRepository.save(existingUser));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (id == null) {
            throw new ValidationException("User ID cannot be null");
        }

        userRepository.deleteById(id);
    }

    @Override
    public User getUserById(Long id) {
        if (id == null) {
            throw new ValidationException("User ID cannot be null");
        }

        return userRepository.getById(id);
    }

    private void validateUserDto(UserDto userDto) {
        if (userDto.getEmail() == null || userDto.getEmail().isBlank()) {
            throw new ValidationException("Email не может быть пустым");
        }
        if (userDto.getName() == null || userDto.getName().isBlank()) {
            throw new ValidationException("Имя не может быть пустым");
        }
    }
}