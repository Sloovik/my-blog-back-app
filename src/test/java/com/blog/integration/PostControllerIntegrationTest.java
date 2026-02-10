package com.blog.integration;

import com.blog.config.TestConfig;
import com.blog.dto.PostDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource("classpath:application-test.properties")
@Import(TestConfig.class)
class PostControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM comments");
        jdbcTemplate.execute("DELETE FROM post_tags");
        jdbcTemplate.execute("DELETE FROM posts");
        jdbcTemplate.execute("ALTER SEQUENCE posts_id_seq RESTART WITH 1");
    }

    @Test
    void createPostWithValidDataReturnsCreatedPost() throws Exception {
        PostDto postDto = PostDto.builder()
                .title("Integration Test Post")
                .text("This is an integration test")
                .tags(Set.of("test", "integration"))
                .build();

        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Integration Test Post"))
                .andExpect(jsonPath("$.text").value("This is an integration test"))
                .andExpect(jsonPath("$.likesCount").value(0))
                .andExpect(jsonPath("$.commentsCount").value(0));
    }

    @Test
    void getPostByIdWhenPostExistsReturnsPost() throws Exception {
        Long postId = insertTestPost("Test Post", "Test content");

        mockMvc.perform(get("/api/posts/{id}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(postId))
                .andExpect(jsonPath("$.title").value("Test Post"))
                .andExpect(jsonPath("$.text").value("Test content"));
    }

    @Test
    void getPostByIdWhenPostNotExistsReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/posts/{id}", 999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void updatePostWhenPostExistsReturnsUpdatedPost() throws Exception {
        Long postId = insertTestPost("Original Title", "Original content");

        PostDto updateDto = PostDto.builder()
                .title("Updated Title")
                .text("Updated content")
                .tags(Set.of("updated"))
                .build();

        mockMvc.perform(put("/api/posts/{id}", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(postId))
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.text").value("Updated content"));
    }

    @Test
    void deletePostWhenPostExistsDeletesPost() throws Exception {
        Long postId = insertTestPost("Post to Delete", "Content");

        mockMvc.perform(delete("/api/posts/{id}", postId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/posts/{id}", postId))
                .andExpect(status().isNotFound());
    }

    @Test
    void addLikeWhenPostExistsIncrementsLikes() throws Exception {
        Long postId = insertTestPost("Post with Likes", "Content");

        mockMvc.perform(post("/api/posts/{id}/likes", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(1));

        mockMvc.perform(post("/api/posts/{id}/likes", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(2));
    }

    @Test
    void getPostsWithoutFiltersReturnsAllPosts() throws Exception {
        insertTestPost("Post 1", "Content 1");
        insertTestPost("Post 2", "Content 2");
        insertTestPost("Post 3", "Content 3");

        mockMvc.perform(get("/api/posts")
                        .param("search", "")
                        .param("pageNumber", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasPrev").value(false))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.lastPage").value(1));
    }

    @Test
    void getPostsWithTitleFilterReturnsFilteredPosts() throws Exception {
        insertTestPost("Java Tutorial", "Content");
        insertTestPost("Spring Boot Guide", "Content");
        insertTestPost("Python Basics", "Content");

        mockMvc.perform(get("/api/posts")
                        .param("search", "Java")
                        .param("pageNumber", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void getPostsWithPaginationReturnsCorrectPage() throws Exception {
        for (int i = 1; i <= 15; i++) {
            insertTestPost("Post " + i, "Content " + i);
        }

        mockMvc.perform(get("/api/posts")
                        .param("search", "")
                        .param("pageNumber", "2")
                        .param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasPrev").value(true))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.lastPage").value(3));
    }

    @Test
    void getPostsWithTagFilterReturnsFilteredPosts() throws Exception {
        Long postId = insertTestPost("Tagged Post", "Content");
        insertPostTag(postId, "java");
        insertPostTag(postId, "spring");

        insertTestPost("Untagged Post", "Content");

        mockMvc.perform(get("/api/posts")
                        .param("search", "#java")
                        .param("pageNumber", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts[0].title").value("Tagged Post"));
    }

    private Long insertTestPost(String title, String text) {
        String sql = "INSERT INTO posts (title, text, likes_count, created_at, updated_at) " +
                "VALUES (?, ?, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        jdbcTemplate.update(sql, title, text);

        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM posts", Long.class);
    }

    private void insertPostTag(Long postId, String tag) {
        jdbcTemplate.update("INSERT INTO post_tags (post_id, tag) VALUES (?, ?)", postId, tag);
    }
}