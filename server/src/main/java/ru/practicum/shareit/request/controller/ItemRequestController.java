package ru.practicum.shareit.request.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.service.ItemRequestService;

import java.util.List;

@RestController
@RequestMapping(path = "/requests")
@RequiredArgsConstructor
public class ItemRequestController {
    private final ItemRequestService itemRequestService;

    @PostMapping
    public ItemRequestDto create(@RequestHeader("X-Sharer-User-Id") Long userId,
                                 @RequestBody ItemRequestDto itemRequestDto) {
        return itemRequestService.create(userId, itemRequestDto);
    }

    @GetMapping
    public List<ItemRequestDto> getOwnRequests(@RequestHeader("X-Sharer-User-Id") Long userId) {
        return itemRequestService.getOwnRequests(userId);
    }

    @GetMapping("/all")
    public List<ItemRequestDto> getAllRequests(@RequestHeader("X-Sharer-User-Id") Long userId,
                                               @RequestParam(defaultValue = "0") int from,
                                               @RequestParam(defaultValue = "10") int size) {
        return itemRequestService.getAllRequests(userId, from, size);
    }

    @GetMapping("/{requestId}")
    public ItemRequestDto getById(@RequestHeader("X-Sharer-User-Id") Long userId,
                                  @PathVariable Long requestId) {
        return itemRequestService.getById(userId, requestId);
    }
}