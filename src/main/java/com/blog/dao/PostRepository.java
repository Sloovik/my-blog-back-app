package com.blog.dao;

import com.blog.model.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PostRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Post> postRowMapper = (rs, rowNum) -> Post.builder()
            .id(rs.getLong("id"))
            .title(rs.getString("title"))
            .text(rs.getString("text"))
            .likesCount(rs.getInt("likes_count"))
            .image(rs.getBytes("image"))
            .createdAt(rs.getObject("created_at", LocalDateTime.class))
            .updatedAt(rs.getObject("updated_at", LocalDateTime.class))
            .build();

    public Optional<Post> findById(Long id) {
        String sql = "SELECT * FROM posts WHERE id = ?";
        try {
            Post post = jdbcTemplate.queryForObject(sql, postRowMapper, id);
            return Optional.of(post);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public boolean existsById(Long id) {
        String sql = "SELECT COUNT(*) FROM posts WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }

    public List<Post> findByTitleContainingPaginated(String title, int pageSize, int offset) {
        String sql = "SELECT * FROM posts WHERE LOWER(title) LIKE LOWER(?) ORDER BY created_at DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, postRowMapper, "%" + title + "%", pageSize, offset);
    }

    public List<Post> findByTagsPaginated(List<String> tags, int tagCount, int pageSize, int offset) {
        String placeholders = String.join(",", tags.stream().map(t -> "?").toList());
        String sql = String.format(
                "SELECT DISTINCT p.* FROM posts p INNER JOIN post_tags pt ON p.id = pt.post_id WHERE pt.tag IN (%s) GROUP BY p.id HAVING COUNT(DISTINCT pt.tag) = ? ORDER BY p.created_at DESC LIMIT ? OFFSET ?",
                placeholders
        );

        Object[] params = new Object[tags.size() + 3];
        for (int i = 0; i < tags.size(); i++) {
            params[i] = tags.get(i);
        }
        params[tags.size()] = tagCount;
        params[tags.size() + 1] = pageSize;
        params[tags.size() + 2] = offset;

        return jdbcTemplate.query(sql, postRowMapper, params);
    }

    public List<Post> findByTitleAndTagsPaginated(String title, List<String> tags, int tagCount, int pageSize, int offset) {
        String placeholders = String.join(",", tags.stream().map(t -> "?").toList());
        String sql = String.format(
                "SELECT DISTINCT p.* FROM posts p INNER JOIN post_tags pt ON p.id = pt.post_id WHERE LOWER(p.title) LIKE LOWER(?) AND pt.tag IN (%s) GROUP BY p.id HAVING COUNT(DISTINCT pt.tag) = ? ORDER BY p.created_at DESC LIMIT ? OFFSET ?",
                placeholders
        );

        Object[] params = new Object[tags.size() + 4];
        params[0] = "%" + title + "%";
        for (int i = 0; i < tags.size(); i++) {
            params[i + 1] = tags.get(i);
        }
        params[tags.size() + 1] = tagCount;
        params[tags.size() + 2] = pageSize;
        params[tags.size() + 3] = offset;

        return jdbcTemplate.query(sql, postRowMapper, params);
    }

    public int countByTitleContaining(String title) {
        String sql = "SELECT COUNT(*) FROM posts WHERE LOWER(title) LIKE LOWER(?)";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, "%" + title + "%");
        return count != null ? count : 0;
    }

    public int countByTags(List<String> tags, int tagCount) {
        String placeholders = String.join(",", tags.stream().map(t -> "?").toList());
        String sql = String.format(
                "SELECT COUNT(DISTINCT p.id) FROM posts p INNER JOIN post_tags pt ON p.id = pt.post_id WHERE pt.tag IN (%s) GROUP BY p.id HAVING COUNT(DISTINCT pt.tag) = ?",
                placeholders
        );

        Object[] params = new Object[tags.size() + 1];
        for (int i = 0; i < tags.size(); i++) {
            params[i] = tags.get(i);
        }
        params[tags.size()] = tagCount;

        try {
            List<Integer> results = jdbcTemplate.queryForList(sql, Integer.class, params);
            return results.size();
        } catch (Exception e) {
            return 0;
        }
    }

    public int countByTitleAndTags(String title, List<String> tags, int tagCount) {
        String placeholders = String.join(",", tags.stream().map(t -> "?").toList());
        String sql = String.format(
                "SELECT COUNT(DISTINCT p.id) FROM posts p INNER JOIN post_tags pt ON p.id = pt.post_id WHERE LOWER(p.title) LIKE LOWER(?) AND pt.tag IN (%s) GROUP BY p.id HAVING COUNT(DISTINCT pt.tag) = ?",
                placeholders
        );

        Object[] params = new Object[tags.size() + 2];
        params[0] = "%" + title + "%";
        for (int i = 0; i < tags.size(); i++) {
            params[i + 1] = tags.get(i);
        }
        params[tags.size() + 1] = tagCount;

        try {
            List<Integer> results = jdbcTemplate.queryForList(sql, Integer.class, params);
            return results.size();
        } catch (Exception e) {
            return 0;
        }
    }

    public List<String> findTagsByPostId(Long postId) {
        String sql = "SELECT tag FROM post_tags WHERE post_id = ?";
        return jdbcTemplate.queryForList(sql, String.class, postId);
    }

    public Post save(Post post) {
        if (post.getId() == null) {
            String sql = "INSERT INTO posts (title, text, likes_count, image, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?) RETURNING id";
            Long id = jdbcTemplate.queryForObject(sql, Long.class,
                    post.getTitle(), post.getText(), post.getLikesCount(), post.getImage(),
                    post.getCreatedAt(), post.getUpdatedAt());
            post.setId(id);
        } else {
            String sql = "UPDATE posts SET title = ?, text = ?, likes_count = ?, image = ?, updated_at = ? WHERE id = ?";
            jdbcTemplate.update(sql,
                    post.getTitle(), post.getText(), post.getLikesCount(), post.getImage(),
                    post.getUpdatedAt(), post.getId());
        }
        return post;
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM posts WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }
}