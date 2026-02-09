package com.blog.integration;

import com.blog.dto.CommentDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CommentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createCommentWithValidDataReturnsCreatedComment() throws Exception {
        Long postId = insertTestPost("Test Post", "Content");

        CommentDto commentDto = CommentDto.builder()
                .text("This is a test comment")
                .build();

        mockMvc.perform(post("/api/posts/{postId}/comments", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.text").value("This is a test comment"))
                .andExpect(jsonPath("$.postId").value(postId));
    }

    @Test
    void createCommentForNonExistentPostReturnsNotFound() throws Exception {
        CommentDto commentDto = CommentDto.builder()
                .build();

        mockMvc.perform(post("/api/posts/{postId}/comments", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCommentsForExistingPostReturnsAllComments() throws Exception {
        Long postId = insertTestPost("Test Post", "Content");
        insertTestComment(postId, "Comment 1");
        insertTestComment(postId, "Comment 2");
        insertTestComment(postId, "Comment 3");

        mockMvc.perform(get("/api/posts/{postId}/comments", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void getCommentByIdWhenCommentExistsReturnsComment() throws Exception {
        Long postId = insertTestPost("Test Post", "Content");
        Long commentId = insertTestComment(postId, "Test Comment");

        mockMvc.perform(get("/api/posts/{postId}/comments/{id}", postId, commentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(commentId))
                .andExpect(jsonPath("$.text").value("Test Comment"))
                .andExpect(jsonPath("$.postId").value(postId));
    }

    @Test
    void getCommentByIdWhenCommentNotExistsReturnsNotFound() throws Exception {
        Long postId = insertTestPost("Test Post", "Content");

        mockMvc.perform(get("/api/posts/{postId}/comments/{id}", postId, 999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateCommentWhenCommentExistsReturnsUpdatedComment() throws Exception {
        Long postId = insertTestPost("Test Post", "Content");
        Long commentId = insertTestComment(postId, "Original Comment");

        CommentDto updateDto = CommentDto.builder()
                .text("Updated Comment")
                .build();

        mockMvc.perform(put("/api/posts/{postId}/comments/{id}", postId, commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(commentId))
                .andExpect(jsonPath("$.text").value("Updated Comment"));
    }

    @Test
    void deleteCommentWhenCommentExistsDeletesComment() throws Exception {
        Long postId = insertTestPost("Test Post", "Content");
        Long commentId = insertTestComment(postId, "Comment to Delete");

        mockMvc.perform(delete("/api/posts/{postId}/comments/{id}", postId, commentId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/posts/{postId}/comments/{id}", postId, commentId))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletePostCascadesDeleteComments() throws Exception {
        Long postId = insertTestPost("Test Post", "Content");
        insertTestComment(postId, "Comment 1");
        insertTestComment(postId, "Comment 2");

        mockMvc.perform(delete("/api/posts/{id}", postId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/posts/{postId}/comments", postId))
                .andExpect(status().isNotFound());
    }

    private Long insertTestPost(String title, String text) {
        String sql = "INSERT INTO posts (title, text, likes_count, created_at, updated_at) " +
                "VALUES (?, ?, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) RETURNING id";
        return jdbcTemplate.queryForObject(sql, Long.class, title, text);
    }

    private Long insertTestComment(Long postId, String text) {
        String sql = "INSERT INTO comments (text, post_id, created_at, updated_at) " +
                "VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) RETURNING id";
        return jdbcTemplate.queryForObject(sql, Long.class, text, postId);
    }
}