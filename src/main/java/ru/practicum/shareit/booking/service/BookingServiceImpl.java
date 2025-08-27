package ru.practicum.shareit.booking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingResponseDto;
import ru.practicum.shareit.booking.mapper.BookingMapper;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.storage.BookingRepository;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.service.UserService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // Все методы по умолчанию read-only
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final UserService userService;
    private final ItemService itemService;

    @Override
    @Transactional // Только этот метод требует записи
    public BookingResponseDto create(Long userId, BookingDto bookingDto) {
        validateBookingDto(bookingDto);

        // Получаем пользователя и предмет
        User user = userService.getUserById(userId);
        Item item = itemService.getItemById(bookingDto.getItemId());

        // Владелец не может бронировать свою вещь - 404
        if (item.getOwner().getId().equals(userId)) {
            throw new NotFoundException("Владелец не может бронировать свою вещь");
        }

        // Предмет недоступен - 400
        if (!item.getAvailable()) {
            throw new ValidationException("Предмет недоступен для бронирования");
        }

        // Создаем бронирование с принудительным статусом WAITING
        Booking booking = Booking.builder()
                .start(bookingDto.getStart())
                .end(bookingDto.getEnd())
                .item(item)
                .booker(user)
                .status(BookingStatus.WAITING) // Всегда WAITING при создании
                .build();

        Booking savedBooking = bookingRepository.save(booking);
        return BookingMapper.toBookingResponseDto(savedBooking);
    }

    @Override
    @Transactional // Только этот метод требует записи
    public BookingResponseDto updateStatus(Long userId, Long bookingId, Boolean approved) {
        if (userId == null) {
            throw new ValidationException("User ID cannot be null");
        }
        if (bookingId == null) {
            throw new ValidationException("Booking ID cannot be null");
        }
        if (approved == null) {
            throw new ValidationException("Approved status cannot be null");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Бронирование не найдено"));

        if (!booking.getItem().getOwner().getId().equals(userId)) {
            throw new ForbiddenException("Только владелец может изменять статус бронирования");
        }

        if (booking.getStatus() != BookingStatus.WAITING) {
            throw new ValidationException("Статус бронирования уже изменен");
        }

        booking.setStatus(approved ? BookingStatus.APPROVED : BookingStatus.REJECTED);
        // Не вызываем save - изменения автоматически сохранятся при коммите транзакции
        return BookingMapper.toBookingResponseDto(booking);
    }

    @Override
    public BookingResponseDto getById(Long userId, Long bookingId) {
        if (userId == null) {
            throw new ValidationException("User ID cannot be null");
        }
        if (bookingId == null) {
            throw new ValidationException("Booking ID cannot be null");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Бронирование не найдено"));

        if (!booking.getBooker().getId().equals(userId) &&
                !booking.getItem().getOwner().getId().equals(userId)) {
            throw new NotFoundException("Бронирование не найдено");
        }

        return BookingMapper.toBookingResponseDto(booking);
    }

    @Override
    public List<BookingResponseDto> getAllByBooker(Long userId, String state, int from, int size) {
        if (userId == null) {
            throw new ValidationException("User ID cannot be null");
        }
        if (from < 0) {
            throw new ValidationException("From must be positive");
        }
        if (size <= 0) {
            throw new ValidationException("Size must be positive");
        }

        userService.getById(userId);

        Pageable pageable = PageRequest.of(from / size, size, Sort.by(Sort.Direction.DESC, "start"));
        List<Booking> bookings;
        LocalDateTime now = LocalDateTime.now();

        switch (state.toUpperCase()) {
            case "ALL":
                bookings = bookingRepository.findByBookerIdOrderByStartDesc(userId, pageable);
                break;
            case "CURRENT":
                bookings = bookingRepository.findCurrentByBookerId(userId, now, pageable);
                break;
            case "PAST":
                bookings = bookingRepository.findByBookerIdAndEndBeforeOrderByStartDesc(userId, now, pageable);
                break;
            case "FUTURE":
                bookings = bookingRepository.findByBookerIdAndStartAfterOrderByStartDesc(userId, now, pageable);
                break;
            case "WAITING":
                bookings = bookingRepository.findByBookerIdAndStatusOrderByStartDesc(userId, BookingStatus.WAITING, pageable);
                break;
            case "REJECTED":
                bookings = bookingRepository.findByBookerIdAndStatusOrderByStartDesc(userId, BookingStatus.REJECTED, pageable);
                break;
            default:
                throw new ValidationException("Unknown state: " + state);
        }

        return BookingMapper.toBookingResponseDtoList(bookings);
    }

    @Override
    public List<BookingResponseDto> getAllByOwner(Long userId, String state, int from, int size) {
        if (userId == null) {
            throw new ValidationException("User ID cannot be null");
        }
        if (from < 0) {
            throw new ValidationException("From must be positive");
        }
        if (size <= 0) {
            throw new ValidationException("Size must be positive");
        }

        userService.getById(userId);

        Pageable pageable = PageRequest.of(from / size, size, Sort.by(Sort.Direction.DESC, "start"));
        List<Booking> bookings;
        LocalDateTime now = LocalDateTime.now();

        switch (state.toUpperCase()) {
            case "ALL":
                bookings = bookingRepository.findByItemOwnerIdOrderByStartDesc(userId, pageable);
                break;
            case "CURRENT":
                bookings = bookingRepository.findCurrentByOwnerId(userId, now, pageable);
                break;
            case "PAST":
                bookings = bookingRepository.findByItemOwnerIdAndEndBeforeOrderByStartDesc(userId, now, pageable);
                break;
            case "FUTURE":
                bookings = bookingRepository.findByItemOwnerIdAndStartAfterOrderByStartDesc(userId, now, pageable);
                break;
            case "WAITING":
                bookings = bookingRepository.findByItemOwnerIdAndStatusOrderByStartDesc(userId, BookingStatus.WAITING, pageable);
                break;
            case "REJECTED":
                bookings = bookingRepository.findByItemOwnerIdAndStatusOrderByStartDesc(userId, BookingStatus.REJECTED, pageable);
                break;
            default:
                throw new ValidationException("Unknown state: " + state);
        }

        return BookingMapper.toBookingResponseDtoList(bookings);
    }

    private void validateBookingDto(BookingDto bookingDto) {
        if (bookingDto == null) {
            throw new ValidationException("BookingDto cannot be null");
        }
        if (bookingDto.getStart() == null) {
            throw new ValidationException("Дата начала не может быть null");
        }
        if (bookingDto.getEnd() == null) {
            throw new ValidationException("Дата окончания не может быть null");
        }
        if (bookingDto.getItemId() == null) {
            throw new ValidationException("ID предмета не может быть null");
        }
        if (bookingDto.getEnd().isBefore(bookingDto.getStart())) {
            throw new ValidationException("Дата окончания не может быть раньше даты начала");
        }
        if (bookingDto.getEnd().equals(bookingDto.getStart())) {
            throw new ValidationException("Дата окончания не может совпадать с датой начала");
        }
    }
}