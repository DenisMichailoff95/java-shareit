package ru.practicum.shareit.item.service;

import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.booking.model.Booking;

import java.time.LocalDateTime;
import java.util.List;

public interface ItemService {
    ItemDto create(Long userId, ItemDto itemDto);

    ItemDto update(Long userId, Long itemId, ItemDto itemDto);

    ItemDto getById(Long userId, Long itemId);

    List<ItemDto> getAllByOwner(Long userId);

    List<ItemDto> search(String text);

    Item getItemById(Long itemId);

    CommentDto addComment(Long userId, Long itemId, CommentDto commentDto);

    List<Booking> getLastBookingsForItem(Long itemId, LocalDateTime now);

    List<Booking> getNextBookingsForItem(Long itemId, LocalDateTime now);

    boolean hasUserBookedItem(Long itemId, Long userId, LocalDateTime now);
}