package com.blog.service;

import com.blog.dao.CommentRepository;
import com.blog.dao.PostRepository;
import com.blog.dto.CommentDto;
import com.blog.exception.ResourceNotFoundException;
import com.blog.model.Comment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    @Transactional(readOnly = true)
    public List<CommentDto> getCommentsByPostId(Long postId) {
        log.debug("Getting comments for post {}", postId);
        if (!postRepository.existsById(postId)) {
            throw ResourceNotFoundException.postNotFound(postId);
        }
        return commentRepository.findByPostId(postId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CommentDto getCommentById(Long postId, Long commentId) {
        log.debug("Getting comment {} for post {}", commentId, postId);
        Comment comment = commentRepository.findByIdAndPostId(commentId, postId)
                .orElseThrow(() -> ResourceNotFoundException.commentNotFoundInPost(postId, commentId));
        return toDto(comment);
    }

    @Transactional
    public CommentDto createComment(Long postId, CommentDto dto) {
        log.debug("Creating comment for post {}", postId);
        if (!postRepository.existsById(postId)) {
            throw ResourceNotFoundException.postNotFound(postId);
        }
        Comment comment = Comment.builder()
                .postId(postId)
                .text(dto.getText())
                .build();
        comment.initializeCreatedAt();
        comment.updateTimestamp();
        Comment saved = commentRepository.save(comment);
        return toDto(saved);
    }

    @Transactional
    public CommentDto updateComment(Long postId, Long commentId, CommentDto dto) {
        log.debug("Updating comment {} for post {}", commentId, postId);
        Comment comment = commentRepository.findByIdAndPostId(commentId, postId)
                .orElseThrow(() -> ResourceNotFoundException.commentNotFoundInPost(postId, commentId));
        comment.setText(dto.getText());
        comment.updateTimestamp();
        Comment updated = commentRepository.save(comment);
        return toDto(updated);
    }

    @Transactional
    public void deleteComment(Long postId, Long commentId) {
        log.debug("Deleting comment {} for post {}", commentId, postId);
        Comment comment = commentRepository.findByIdAndPostId(commentId, postId)
                .orElseThrow(() -> ResourceNotFoundException.commentNotFoundInPost(postId, commentId));
        commentRepository.deleteById(comment.getId());
    }

    private CommentDto toDto(Comment c) {
        return CommentDto.builder()
                .id(c.getId())
                .text(c.getText())
                .postId(c.getPostId())
                .build();
    }
}