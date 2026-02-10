package com.blog.dao;

import com.blog.model.Comment;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CommentRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Comment> commentRowMapper = (rs, rowNum) -> Comment.builder()
            .id(rs.getLong("id"))
            .text(rs.getString("text"))
            .postId(rs.getLong("post_id"))
            .createdAt(rs.getObject("created_at", LocalDateTime.class))
            .updatedAt(rs.getObject("updated_at", LocalDateTime.class))
            .build();

    public Optional<Comment> findById(Long id) {
        final String sql = """
            SELECT * FROM comments WHERE id = ?
            """;
        try {
            Comment comment = jdbcTemplate.queryForObject(sql, commentRowMapper, id);
            return Optional.of(comment);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<Comment> findByIdAndPostId(Long id, Long postId) {
        final String sql = """
            SELECT * FROM comments WHERE id = ? AND post_id = ?
            """;
        try {
            Comment comment = jdbcTemplate.queryForObject(sql, commentRowMapper, id, postId);
            return Optional.of(comment);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public List<Comment> findByPostId(Long postId) {
        final String sql = """
            SELECT * FROM comments 
            WHERE post_id = ? 
            ORDER BY created_at DESC
            """;
        return jdbcTemplate.query(sql, commentRowMapper, postId);
    }

    public int countByPostId(Long postId) {
        final String sql = """
            SELECT COUNT(*) 
            FROM comments 
            WHERE post_id = ?
            """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, postId);
        return count != null ? count : 0;
    }

    public Comment save(Comment comment) {
        if (comment.getId() == null) {
            final String sql = """
                INSERT INTO comments (text, post_id, created_at, updated_at) 
                VALUES (?, ?, ?, ?) 
                RETURNING id
                """;
            Long id = jdbcTemplate.queryForObject(sql, Long.class,
                    comment.getText(),
                    comment.getPostId(),
                    comment.getCreatedAt(),
                    comment.getUpdatedAt());
            comment.setId(id);
        } else {
            final String sql = """
                UPDATE comments 
                SET text = ?, updated_at = ? 
                WHERE id = ?
                """;
            jdbcTemplate.update(sql, comment.getText(), comment.getUpdatedAt(), comment.getId());
        }
        return comment;
    }

    public void deleteById(Long id) {
        final String sql = """
            DELETE FROM comments 
            WHERE id = ?
            """;
        jdbcTemplate.update(sql, id);
    }

    public void deleteAllByPostId(Long postId) {
        final String sql = """
            DELETE FROM comments 
            WHERE post_id = ?
            """;
        jdbcTemplate.update(sql, postId);
    }
}