package ru.practicum.shareit.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
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
    @Transactional(readOnly = true)
    public List<UserDto> getAll() {
        return UserMapper.toUserDtoList(userRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getById(Long id) {
        if (id == null) throw new ValidationException("User ID cannot be null");

        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с ID " + id + " не найден"));
        return UserMapper.toUserDto(user);
    }

    @Override
    @Transactional
    public UserDto create(UserDto userDto) {
        if (userDto == null) throw new ValidationException("UserDto cannot be null");

        validateUserDto(userDto);

        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new ConflictException("Email уже используется другим пользователем");
        }

        User user = UserMapper.toUser(userDto);
        User savedUser = userRepository.save(user);
        return UserMapper.toUserDto(savedUser);
    }

    @Override
    @Transactional
    public UserDto update(Long id, UserDto userDto) {
        if (id == null) throw new ValidationException("User ID cannot be null");
        if (userDto == null) throw new ValidationException("UserDto cannot be null");

        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с ID " + id + " не найден"));

        if (userDto.getName() != null) existingUser.setName(userDto.getName());

        if (userDto.getEmail() != null && !existingUser.getEmail().equals(userDto.getEmail())) {
            if (userRepository.existsByEmailAndIdNot(userDto.getEmail(), id)) {
                throw new ConflictException("Email уже используется другим пользователем");
            }
            existingUser.setEmail(userDto.getEmail());
        }

        User updatedUser = userRepository.save(existingUser);
        return UserMapper.toUserDto(updatedUser);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (id == null) throw new ValidationException("User ID cannot be null");

        if (!userRepository.existsById(id)) {
            throw new NotFoundException("Пользователь с ID " + id + " не найден");
        }

        userRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        if (id == null) throw new ValidationException("User ID cannot be null");

        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с ID " + id + " не найден"));
    }

    private void validateUserDto(UserDto userDto) {
        if (userDto.getEmail() == null || userDto.getEmail().isBlank())
            throw new ValidationException("Email не может быть пустым");
        if (userDto.getName() == null || userDto.getName().isBlank())
            throw new ValidationException("Имя не может быть пустым");
    }
}