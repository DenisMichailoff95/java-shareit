package ru.practicum.shareit.item.mapper;

import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.exception.ValidationException;

import java.util.List;
import java.util.stream.Collectors;

public class ItemMapper {
    public static ItemDto toItemDto(Item item) {
        if (item == null) {
            throw new ValidationException("Item cannot be null");
        }

        return ItemDto.builder()
                .id(item.getId())
                .name(item.getName())
                .description(item.getDescription())
                .available(item.getAvailable())
                .requestId(item.getRequestId())
                .build();
    }

    public static Item toItem(ItemDto itemDto) {
        if (itemDto == null) {
            throw new ValidationException("ItemDto cannot be null");
        }

        return Item.builder()
                .id(itemDto.getId())
                .name(itemDto.getName())
                .description(itemDto.getDescription())
                .available(itemDto.getAvailable())
                .requestId(itemDto.getRequestId())
                .build();
    }

    public static List<ItemDto> toItemDtoList(List<Item> items) {
        if (items == null) {
            throw new ValidationException("Items list cannot be null");
        }

        return items.stream()
                .map(ItemMapper::toItemDto)
                .collect(Collectors.toList());
    }
}