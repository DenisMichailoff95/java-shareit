package ru.practicum.shareit.booking.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookItemRequestDto {
    private Long id;

    @NotNull(message = "Дата начала не может быть null")
    @FutureOrPresent(message = "Дата начала должна быть в настоящем или будущем")
    private LocalDateTime start;

    @NotNull(message = "Дата окончания не может быть null")
    @Future(message = "Дата окончания должна быть в будущем")
    private LocalDateTime end;

    @NotNull(message = "ID предмета не может быть null")
    private Long itemId;
}