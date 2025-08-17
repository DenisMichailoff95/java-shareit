package ru.practicum.shareit.user.storage;

import org.springframework.stereotype.Repository;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.model.User;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class UserRepositoryImpl implements UserRepository {
    private final Map<Long, User> users = new HashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Override
    public User save(User user) {
        if (user.getId() == null) {
            return createUser(user);
        }
        return updateUser(user);
    }

    private User createUser(User user) {
        if (findByEmail(user.getEmail()).isPresent()) {
            throw new ConflictException("Email уже используется другим пользователем");
        }
        user.setId(idCounter.getAndIncrement());
        users.put(user.getId(), user);
        return user;
    }

    private User updateUser(User user) {
        User existingUser = users.get(user.getId());
        if (existingUser == null) {
            throw new NotFoundException("Пользователь не найден");
        }

        if (!existingUser.getEmail().equals(user.getEmail())) {
            checkEmailUniqueness(user.getEmail());
        }

        users.put(user.getId(), user);
        return user;
    }

    private void checkEmailUniqueness(String email) {
        findByEmail(email).ifPresent(u -> {
            throw new ConflictException("Email уже используется другим пользователем");
        });
    }

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return users.values().stream()
                .filter(user -> user.getEmail().equals(email))
                .findFirst();
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }

    @Override
    public void deleteById(Long id) {
        users.remove(id);
    }

    @Override
    public User getById(Long id) {
        return findById(id).orElseThrow(() ->
                new NotFoundException("Пользователь с ID " + id + " не найден"));
    }
}