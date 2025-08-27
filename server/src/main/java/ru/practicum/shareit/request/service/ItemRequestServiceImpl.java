package ru.practicum.shareit.request.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.mapper.ItemRequestMapper;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.storage.ItemRequestRepository;
import ru.practicum.shareit.user.service.UserService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemRequestServiceImpl implements ItemRequestService {
    private final ItemRequestRepository itemRequestRepository;
    private final UserService userService;
    private final ItemService itemService;

    @Override
    @Transactional
    public ItemRequestDto create(Long userId, ItemRequestDto itemRequestDto) {
        if (userId == null) throw new ValidationException("User ID cannot be null");
        if (itemRequestDto == null) throw new ValidationException("ItemRequestDto cannot be null");
        if (itemRequestDto.getDescription() == null || itemRequestDto.getDescription().isBlank()) {
            throw new ValidationException("Описание запроса не может быть пустым");
        }

        userService.getUserById(userId);

        ItemRequest itemRequest = ItemRequest.builder()
                .description(itemRequestDto.getDescription())
                .requester(userService.getUserById(userId))
                .created(LocalDateTime.now())
                .build();

        ItemRequest savedRequest = itemRequestRepository.save(itemRequest);
        return ItemRequestMapper.toItemRequestDto(savedRequest);
    }

    @Override
    public List<ItemRequestDto> getOwnRequests(Long userId) {
        if (userId == null) throw new ValidationException("User ID cannot be null");

        userService.getUserById(userId);
        List<ItemRequest> requests = itemRequestRepository.findByRequesterIdOrderByCreatedDesc(userId);
        return enrichWithItems(requests);
    }

    @Override
    public List<ItemRequestDto> getAllRequests(Long userId, int from, int size) {
        if (userId == null) throw new ValidationException("User ID cannot be null");
        if (from < 0) throw new ValidationException("From must be positive");
        if (size <= 0) throw new ValidationException("Size must be positive");

        userService.getUserById(userId);
        Pageable pageable = PageRequest.of(from / size, size);
        List<ItemRequest> requests = itemRequestRepository.findByRequesterIdNotOrderByCreatedDesc(userId, pageable);
        return enrichWithItems(requests);
    }

    @Override
    public ItemRequestDto getById(Long userId, Long requestId) {
        if (userId == null) throw new ValidationException("User ID cannot be null");
        if (requestId == null) throw new ValidationException("Request ID cannot be null");

        userService.getUserById(userId);
        ItemRequest request = itemRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Запрос не найден"));

        ItemRequestDto dto = ItemRequestMapper.toItemRequestDto(request);
        List<ItemDto> items = itemService.getItemsByRequestId(requestId);

        dto.setItems(items.stream()
                .map(item -> new ItemRequestDto.ItemResponseDto(
                        item.getId(),
                        item.getName(),
                        item.getDescription(),
                        item.getAvailable(),
                        item.getRequestId()
                ))
                .collect(Collectors.toList()));

        return dto;
    }

    private List<ItemRequestDto> enrichWithItems(List<ItemRequest> requests) {
        return requests.stream()
                .map(request -> {
                    ItemRequestDto dto = ItemRequestMapper.toItemRequestDto(request);
                    List<ItemDto> items = itemService.getItemsByRequestId(request.getId());

                    dto.setItems(items.stream()
                            .map(item -> new ItemRequestDto.ItemResponseDto(
                                    item.getId(),
                                    item.getName(),
                                    item.getDescription(),
                                    item.getAvailable(),
                                    item.getRequestId()
                            ))
                            .collect(Collectors.toList()));

                    return dto;
                })
                .collect(Collectors.toList());
    }
}