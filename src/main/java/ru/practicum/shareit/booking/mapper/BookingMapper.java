package ru.practicum.shareit.booking.mapper;

import ru.practicum.shareit.booking.dto.*;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.model.User;

import java.util.List;
import java.util.stream.Collectors;

public class BookingMapper {
    public static Booking toBooking(BookingDto bookingDto, Item item, User booker) {
        if (bookingDto == null) {
            throw new ValidationException("BookingDto cannot be null");
        }

        return Booking.builder()
                .id(bookingDto.getId())
                .start(bookingDto.getStart())
                .end(bookingDto.getEnd())
                .item(item)
                .booker(booker)
                .status(bookingDto.getStatus() != null ? bookingDto.getStatus() : ru.practicum.shareit.booking.model.BookingStatus.WAITING)
                .build();
    }

    public static BookingResponseDto toBookingResponseDto(Booking booking) {
        if (booking == null) {
            throw new ValidationException("Booking cannot be null");
        }

        return BookingResponseDto.builder()
                .id(booking.getId())
                .start(booking.getStart())
                .end(booking.getEnd())
                .item(BookingItemDto.builder()
                        .id(booking.getItem().getId())
                        .name(booking.getItem().getName())
                        .build())
                .booker(BookerDto.builder()
                        .id(booking.getBooker().getId())
                        .name(booking.getBooker().getName())
                        .build())
                .status(booking.getStatus())
                .build();
    }

    public static List<BookingResponseDto> toBookingResponseDtoList(List<Booking> bookings) {
        if (bookings == null) {
            throw new ValidationException("Bookings list cannot be null");
        }

        return bookings.stream()
                .map(BookingMapper::toBookingResponseDto)
                .collect(Collectors.toList());
    }
}