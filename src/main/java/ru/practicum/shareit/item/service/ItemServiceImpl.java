package ru.practicum.shareit.item.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.storage.BookingRepository;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.mapper.CommentMapper;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.storage.CommentRepository;
import ru.practicum.shareit.item.storage.ItemRepository;
import ru.practicum.shareit.user.service.UserService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final UserService userService;
    private final BookingRepository bookingRepository;
    private final CommentRepository commentRepository;

    @Override
    @Transactional
    public ItemDto create(Long userId, ItemDto itemDto) {
        if (userId == null) throw new ValidationException("User ID cannot be null");
        if (itemDto == null) throw new ValidationException("ItemDto cannot be null");

        userService.getById(userId);

        if (itemDto.getName() == null || itemDto.getName().isBlank())
            throw new ValidationException("Название не может быть пустым");
        if (itemDto.getDescription() == null || itemDto.getDescription().isBlank())
            throw new ValidationException("Описание не может быть пустым");
        if (itemDto.getAvailable() == null) throw new ValidationException("Статус доступности не может быть null");

        Item item = ItemMapper.toItem(itemDto);
        item.setOwner(userService.getUserById(userId));
        Item savedItem = itemRepository.save(item);
        return ItemMapper.toItemDto(savedItem);
    }

    @Override
    @Transactional
    public ItemDto update(Long userId, Long itemId, ItemDto itemDto) {
        if (userId == null) throw new ValidationException("User ID cannot be null");
        if (itemId == null) throw new ValidationException("Item ID cannot be null");
        if (itemDto == null) throw new ValidationException("ItemDto cannot be null");

        Item existingItem = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь не найдена"));

        if (!existingItem.getOwner().getId().equals(userId)) {
            throw new ForbiddenException("Редактировать может только владелец");
        }

        if (itemDto.getName() != null) existingItem.setName(itemDto.getName());
        if (itemDto.getDescription() != null) existingItem.setDescription(itemDto.getDescription());
        if (itemDto.getAvailable() != null) existingItem.setAvailable(itemDto.getAvailable());

        Item updatedItem = itemRepository.save(existingItem);
        return ItemMapper.toItemDto(updatedItem);
    }

    @Override
    @Transactional(readOnly = true)
    public ItemDto getById(Long userId, Long itemId) {
        try {
            if (userId == null) throw new ValidationException("User ID cannot be null");
            if (itemId == null) throw new ValidationException("Item ID cannot be null");

            Item item = itemRepository.findById(itemId)
                    .orElseThrow(() -> new NotFoundException("Вещь не найдена"));

            ItemDto itemDto = ItemMapper.toItemDto(item);

            // Загружаем бронирования только для владельца
            if (item.getOwner().getId().equals(userId)) {
                LocalDateTime now = LocalDateTime.now();

                Booking lastBooking = bookingRepository
                        .findFirstByItemIdAndEndBeforeOrderByEndDesc(itemId, now)
                        .orElse(null);

                Booking nextBooking = bookingRepository
                        .findFirstByItemIdAndStartAfterOrderByStartAsc(itemId, now)
                        .orElse(null);

                if (lastBooking != null) {
                    itemDto.setLastBooking(new ItemDto.BookingInfo(lastBooking.getId(), lastBooking.getBooker().getId()));
                }

                if (nextBooking != null) {
                    itemDto.setNextBooking(new ItemDto.BookingInfo(nextBooking.getId(), nextBooking.getBooker().getId()));
                }
            }

            // Загружаем комментарии с авторами
            List<CommentDto> comments = commentRepository.findAllByItemIdWithAuthor(itemId).stream()
                    .map(comment -> CommentDto.builder()
                            .id(comment.getId())
                            .text(comment.getText())
                            .authorName(comment.getAuthor().getName())
                            .created(comment.getCreated())
                            .build())
                    .collect(Collectors.toList());

            itemDto.setComments(comments);

            return itemDto;
        } catch (Exception e) {
            log.error("Error in getById method: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemDto> getAllByOwner(Long userId) {
        if (userId == null) throw new ValidationException("User ID cannot be null");

        List<Item> items = itemRepository.findAllByOwnerIdOrderById(userId);
        LocalDateTime now = LocalDateTime.now();

        // Получаем ID всех items для batch запросов
        List<Long> itemIds = items.stream().map(Item::getId).collect(Collectors.toList());

        // Получаем все бронирования для items
        Map<Long, Booking> lastBookingsMap = bookingRepository.findLastBookingsForItems(itemIds, now).stream()
                .collect(Collectors.toMap(booking -> booking.getItem().getId(), booking -> booking));

        Map<Long, Booking> nextBookingsMap = bookingRepository.findNextBookingsForItems(itemIds, now).stream()
                .collect(Collectors.toMap(booking -> booking.getItem().getId(), booking -> booking));

        // Получаем все комментарии для items с авторами
        Map<Long, List<CommentDto>> commentsMap = commentRepository.findAllByItemIdInWithAuthors(itemIds).stream()
                .collect(Collectors.groupingBy(
                        comment -> comment.getItem().getId(),
                        Collectors.mapping(comment -> CommentDto.builder()
                                .id(comment.getId())
                                .text(comment.getText())
                                .authorName(comment.getAuthor().getName())
                                .created(comment.getCreated())
                                .build(), Collectors.toList())
                ));

        return items.stream()
                .map(item -> {
                    ItemDto itemDto = ItemMapper.toItemDto(item);

                    // Устанавливаем бронирования
                    Booking lastBooking = lastBookingsMap.get(item.getId());
                    Booking nextBooking = nextBookingsMap.get(item.getId());

                    if (lastBooking != null) {
                        itemDto.setLastBooking(new ItemDto.BookingInfo(lastBooking.getId(), lastBooking.getBooker().getId()));
                    }

                    if (nextBooking != null) {
                        itemDto.setNextBooking(new ItemDto.BookingInfo(nextBooking.getId(), nextBooking.getBooker().getId()));
                    }

                    // Устанавливаем комментарии
                    itemDto.setComments(commentsMap.getOrDefault(item.getId(), List.of()));

                    return itemDto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemDto> search(String text) {
        if (text == null || text.isBlank()) return List.of();

        List<Item> items = itemRepository.search(text);
        return ItemMapper.toItemDtoList(items);
    }

    @Override
    @Transactional(readOnly = true)
    public Item getItemById(Long itemId) {
        if (itemId == null) throw new ValidationException("Item ID cannot be null");

        return itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь не найдена"));
    }

    @Override
    @Transactional
    public CommentDto addComment(Long userId, Long itemId, CommentDto commentDto) {
        if (userId == null) throw new ValidationException("User ID cannot be null");
        if (itemId == null) throw new ValidationException("Item ID cannot be null");
        if (commentDto == null || commentDto.getText() == null || commentDto.getText().isBlank())
            throw new ValidationException("Текст комментария не может быть пустым");

        if (!hasUserBookedItem(itemId, userId, LocalDateTime.now())) {
            throw new ValidationException("Пользователь не брал эту вещь в аренду");
        }

        Comment comment = CommentMapper.toComment(commentDto);
        comment.setItem(getItemById(itemId));
        comment.setAuthor(userService.getUserById(userId));
        comment.setCreated(LocalDateTime.now());

        Comment savedComment = commentRepository.save(comment);
        return CommentMapper.toCommentDto(savedComment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> getLastBookingsForItem(Long itemId, LocalDateTime now) {
        return bookingRepository.findLastBookingsForItem(itemId, now);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> getNextBookingsForItem(Long itemId, LocalDateTime now) {
        return bookingRepository.findNextBookingsForItem(itemId, now);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasUserBookedItem(Long itemId, Long userId, LocalDateTime now) {
        return bookingRepository.existsByItemIdAndBookerIdAndEndBefore(itemId, userId, now);
    }
}