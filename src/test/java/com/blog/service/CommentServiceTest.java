package com.blog.service;

import com.blog.dao.CommentRepository;
import com.blog.dao.PostRepository;
import com.blog.dto.CommentDto;
import com.blog.exception.ResourceNotFoundException;
import com.blog.model.Comment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private CommentService commentService;

    private Comment testComment;

    @BeforeEach
    void setUp() {
        testComment = Comment.builder()
                .id(1L)
                .text("Test comment")
                .postId(1L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getCommentsByPostIdWhenPostExistsReturnsComments() {
        when(postRepository.existsById(1L)).thenReturn(true);
        when(commentRepository.findByPostId(1L)).thenReturn(List.of(testComment));

        List<CommentDto> result = commentService.getCommentsByPostId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getText()).isEqualTo("Test comment");
        assertThat(result.get(0).getPostId()).isEqualTo(1L);

        verify(postRepository).existsById(1L);
        verify(commentRepository).findByPostId(1L);
    }

    @Test
    void getCommentsByPostIdWhenPostNotExistsThrowsResourceNotFoundException() {
        when(postRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> commentService.getCommentsByPostId(999L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(postRepository).existsById(999L);
        verify(commentRepository, never()).findByPostId(anyLong());
    }

    @Test
    void getCommentByIdWhenCommentExistsReturnsComment() {
        when(commentRepository.findByIdAndPostId(1L, 1L)).thenReturn(Optional.of(testComment));

        CommentDto result = commentService.getCommentById(1L, 1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getText()).isEqualTo("Test comment");

        verify(commentRepository).findByIdAndPostId(1L, 1L);
    }

    @Test
    void getCommentByIdWhenCommentNotExistsThrowsResourceNotFoundException() {
        when(commentRepository.findByIdAndPostId(999L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.getCommentById(1L, 999L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(commentRepository).findByIdAndPostId(999L, 1L);
    }

    @Test
    void createCommentWhenPostExistsReturnsCreatedComment() {
        CommentDto inputDto = CommentDto.builder()
                .text("New comment")
                .build();

        Comment savedComment = Comment.builder()
                .id(2L)
                .text("New comment")
                .postId(1L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(postRepository.existsById(1L)).thenReturn(true);
        when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);

        CommentDto result = commentService.createComment(1L, inputDto);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getText()).isEqualTo("New comment");
        assertThat(result.getPostId()).isEqualTo(1L);

        verify(postRepository).existsById(1L);
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void createCommentWhenPostNotExistsThrowsResourceNotFoundException() {
        when(postRepository.existsById(999L)).thenReturn(false);

        CommentDto inputDto = CommentDto.builder()
                .text("New comment")
                .build();

        assertThatThrownBy(() -> commentService.createComment(999L, inputDto))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(postRepository).existsById(999L);
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    void updateCommentWhenCommentExistsReturnsUpdatedComment() {
        CommentDto updateDto = CommentDto.builder()
                .text("Updated comment")
                .build();

        when(commentRepository.findByIdAndPostId(1L, 1L)).thenReturn(Optional.of(testComment));
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);

        CommentDto result = commentService.updateComment(1L, 1L, updateDto);

        assertThat(result.getText()).isEqualTo("Updated comment");

        verify(commentRepository).findByIdAndPostId(1L, 1L);
        verify(commentRepository).save(argThat(comment ->
                comment.getText().equals("Updated comment")));
    }

    @Test
    void deleteCommentWhenCommentExistsDeletesComment() {
        when(commentRepository.findByIdAndPostId(1L, 1L)).thenReturn(Optional.of(testComment));

        commentService.deleteComment(1L, 1L);

        verify(commentRepository).findByIdAndPostId(1L, 1L);
        verify(commentRepository).deleteById(1L);
    }

    @Test
    void deleteCommentWhenCommentNotExistsThrowsResourceNotFoundException() {
        when(commentRepository.findByIdAndPostId(999L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.deleteComment(1L, 999L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(commentRepository).findByIdAndPostId(999L, 1L);
        verify(commentRepository, never()).deleteById(anyLong());
    }
}