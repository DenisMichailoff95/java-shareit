package ru.practicum.shareit.item.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.storage.BookingRepository;
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

        if (itemDto.getName() == null || itemDto.getName().isBlank()) throw new ValidationException("Название не может быть пустым");
        if (itemDto.getDescription() == null || itemDto.getDescription().isBlank()) throw new ValidationException("Описание не может быть пустым");
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
            throw new NotFoundException("Редактировать может только владелец");
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
        if (userId == null) throw new ValidationException("User ID cannot be null");
        if (itemId == null) throw new ValidationException("Item ID cannot be null");

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь не найдена"));

        ItemDto itemDto = ItemMapper.toItemDto(item);

        if (item.getOwner().getId().equals(userId)) {
            LocalDateTime now = LocalDateTime.now();
            itemDto.setLastBooking(getLastBookingForItem(itemId, now));
            itemDto.setNextBooking(getNextBookingForItem(itemId, now));
        }

        itemDto.setComments(getCommentsForItem(itemId));

        return itemDto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemDto> getAllByOwner(Long userId) {
        if (userId == null) throw new ValidationException("User ID cannot be null");

        List<Item> items = itemRepository.findAllByOwnerIdOrderById(userId);
        LocalDateTime now = LocalDateTime.now();

        // Получаем все комментарии для всех items одним запросом
        List<Long> itemIds = items.stream().map(Item::getId).collect(Collectors.toList());
        Map<Long, List<CommentDto>> commentsMap = commentRepository.findAllByItemIdIn(itemIds)
                .stream()
                .collect(Collectors.groupingBy(
                        comment -> comment.getItem().getId(),
                        Collectors.mapping(CommentMapper::toCommentDto, Collectors.toList())
                ));

        return items.stream()
                .map(item -> {
                    ItemDto itemDto = ItemMapper.toItemDto(item);
                    itemDto.setLastBooking(getLastBookingForItem(item.getId(), now));
                    itemDto.setNextBooking(getNextBookingForItem(item.getId(), now));
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
        if (commentDto == null || commentDto.getText() == null || commentDto.getText().isBlank()) throw new ValidationException("Текст комментария не может быть пустым");

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

    private ItemDto.BookingInfo getLastBookingForItem(Long itemId, LocalDateTime now) {
        return bookingRepository.findFirstByItemIdAndEndBeforeOrderByEndDesc(itemId, now)
                .map(booking -> new ItemDto.BookingInfo(booking.getId(), booking.getBooker().getId()))
                .orElse(null);
    }

    private ItemDto.BookingInfo getNextBookingForItem(Long itemId, LocalDateTime now) {
        return bookingRepository.findFirstByItemIdAndStartAfterOrderByStartAsc(itemId, now)
                .map(booking -> new ItemDto.BookingInfo(booking.getId(), booking.getBooker().getId()))
                .orElse(null);
    }

    private List<CommentDto> getCommentsForItem(Long itemId) {
        return commentRepository.findAllByItemId(itemId).stream()
                .map(CommentMapper::toCommentDto)
                .collect(Collectors.toList());
    }
}