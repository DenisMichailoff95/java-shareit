package ru.practicum.shareit.item.mapper;

import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.exception.ValidationException;

import java.util.List;
import java.util.stream.Collectors;

public class CommentMapper {
    public static CommentDto toCommentDto(Comment comment) {
        if (comment == null) {
            throw new ValidationException("Comment cannot be null");
        }

        return CommentDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .authorName(comment.getAuthor().getName())
                .created(comment.getCreated())
                .build();
    }

    public static Comment toComment(CommentDto commentDto) {
        if (commentDto == null) {
            throw new ValidationException("CommentDto cannot be null");
        }

        return Comment.builder()
                .id(commentDto.getId())
                .text(commentDto.getText())
                .created(commentDto.getCreated())
                .build();
    }

    public static List<CommentDto> toCommentDtoList(List<Comment> comments) {
        if (comments == null) {
            throw new ValidationException("Comments list cannot be null");
        }

        return comments.stream()
                .map(CommentMapper::toCommentDto)
                .collect(Collectors.toList());
    }
}