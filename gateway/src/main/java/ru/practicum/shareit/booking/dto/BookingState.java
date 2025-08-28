package ru.practicum.shareit.booking.dto;

import java.util.Optional;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BookingState {
    ALL("all"),
    CURRENT("current"),
    PAST("past"),
    FUTURE("future"),
    WAITING("waiting"),
    REJECTED("rejected");

    private final String value;

    public static Optional<BookingState> from(String state) {
        for (BookingState bookingState : values()) {
            if (bookingState.value.equalsIgnoreCase(state)) {
                return Optional.of(bookingState);
            }
        }
        return Optional.empty();
    }
}