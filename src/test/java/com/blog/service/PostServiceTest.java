package com.blog.service;

import com.blog.dao.CommentRepository;
import com.blog.dao.PostRepository;
import com.blog.dto.PostDto;
import com.blog.dto.PostListResponseDto;
import com.blog.exception.ResourceNotFoundException;
import com.blog.model.Post;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private PostService postService;

    private Post testPost;
    private PostDto testPostDto;

    @BeforeEach
    void setUp() {
        testPost = Post.builder()
                .id(1L)
                .title("Test Post")
                .text("Test content")
                .likesCount(5)
                .tags(Set.of("java", "spring"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testPostDto = PostDto.builder()
                .id(1L)
                .title("Test Post")
                .text("Test content")
                .tags(Set.of("java", "spring"))
                .likesCount(5)
                .commentsCount(3)
                .build();
    }

    @Test
    void getPostByIdWhenPostExistsReturnsPostDto() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(postRepository.findTagsByPostId(1L)).thenReturn(List.of("java", "spring"));
        when(commentRepository.countByPostId(1L)).thenReturn(3);

        PostDto result = postService.getPostById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Test Post");
        assertThat(result.getTags()).containsExactlyInAnyOrder("java", "spring");
        assertThat(result.getCommentsCount()).isEqualTo(3);

        verify(postRepository).findById(1L);
        verify(postRepository).findTagsByPostId(1L);
        verify(commentRepository).countByPostId(1L);
    }

    @Test
    void getPostByIdWhenPostNotExistsThrowsResourceNotFoundException() {
        when(postRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPostById(999L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(postRepository).findById(999L);
    }

    @Test
    void createPostWithValidDataReturnsCreatedPostDto() {
        PostDto inputDto = PostDto.builder()
                .title("New Post")
                .text("New content")
                .tags(Set.of("test"))
                .build();

        Post savedPost = Post.builder()
                .id(2L)
                .title("New Post")
                .text("New content")
                .tags(Set.of("test"))
                .likesCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(postRepository.save(any(Post.class))).thenReturn(savedPost);
        when(postRepository.findTagsByPostId(2L)).thenReturn(List.of("test"));
        when(commentRepository.countByPostId(2L)).thenReturn(0);

        PostDto result = postService.createPost(inputDto);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getTitle()).isEqualTo("New Post");
        assertThat(result.getLikesCount()).isZero();

        verify(postRepository).save(any(Post.class));
        verify(jdbcTemplate, times(1)).update(anyString(), eq(2L), eq("test"));
    }

    @Test
    void updatePostWhenPostExistsReturnsUpdatedPostDto() {
        PostDto updateDto = PostDto.builder()
                .title("Updated Title")
                .text("Updated content")
                .tags(Set.of("updated"))
                .build();

        Post existingPost = Post.builder()
                .id(1L)
                .title("Old Title")
                .text("Old content")
                .likesCount(5)
                .tags(Set.of("old"))
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();

        when(postRepository.findById(1L)).thenReturn(Optional.of(existingPost));
        when(postRepository.save(any(Post.class))).thenReturn(existingPost);
        when(postRepository.findTagsByPostId(1L)).thenReturn(List.of("updated"));
        when(commentRepository.countByPostId(1L)).thenReturn(0);

        PostDto result = postService.updatePost(1L, updateDto);

        assertThat(result.getTitle()).isEqualTo("Updated Title");
        assertThat(result.getText()).isEqualTo("Updated content");

        verify(postRepository).findById(1L);
        verify(postRepository).save(any(Post.class));
        verify(jdbcTemplate).update(eq("DELETE FROM post_tags WHERE post_id = ?"), eq(1L));
    }

    @Test
    void updatePostWhenPostNotExistsThrowsResourceNotFoundException() {
        when(postRepository.findById(999L)).thenReturn(Optional.empty());

        PostDto updateDto = PostDto.builder()
                .title("Updated")
                .text("Updated")
                .build();

        assertThatThrownBy(() -> postService.updatePost(999L, updateDto))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(postRepository).findById(999L);
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void deletePostWhenPostExistsDeletesPostAndRelatedData() {
        when(postRepository.existsById(1L)).thenReturn(true);

        postService.deletePost(1L);

        verify(postRepository).existsById(1L);
        verify(commentRepository).deleteAllByPostId(1L);
        verify(jdbcTemplate).update(eq("DELETE FROM post_tags WHERE post_id = ?"), eq(1L));
        verify(postRepository).deleteById(1L);
    }

    @Test
    void deletePostWhenPostNotExistsThrowsResourceNotFoundException() {
        when(postRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> postService.deletePost(999L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(postRepository).existsById(999L);
        verify(postRepository, never()).deleteById(anyLong());
    }

    @Test
    void addLikeWhenPostExistsIncrementsLikesCount() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(postRepository.save(any(Post.class))).thenReturn(testPost);

        Integer result = postService.addLike(1L);

        assertThat(result).isEqualTo(6);
        verify(postRepository).findById(1L);
        verify(postRepository).save(argThat(post -> post.getLikesCount() == 6));
    }

    @Test
    void getPostsWithPaginationWithoutFiltersReturnsAllPosts() {
        List<Post> posts = List.of(testPost);
        when(postRepository.findByTitleContainingPaginated("", 10, 0)).thenReturn(posts);
        when(postRepository.countByTitleContaining("")).thenReturn(1);
        when(postRepository.findTagsByPostId(1L)).thenReturn(List.of("java", "spring"));
        when(commentRepository.countByPostId(1L)).thenReturn(3);

        PostListResponseDto result = postService.getPostsWithPagination("", 1, 10);

        assertThat(result.getPosts()).hasSize(1);
        assertThat(result.getLastPage()).isEqualTo(1);

        verify(postRepository).findByTitleContainingPaginated("", 10, 0);
    }

    @Test
    void getPostsWithPaginationWithTitleSearchFiltersCorrectly() {
        when(postRepository.findByTitleContainingPaginated("Test", 10, 0))
                .thenReturn(List.of(testPost));
        when(postRepository.countByTitleContaining("Test")).thenReturn(1);
        when(postRepository.findTagsByPostId(1L)).thenReturn(List.of());
        when(commentRepository.countByPostId(1L)).thenReturn(0);

        PostListResponseDto result = postService.getPostsWithPagination("Test", 1, 10);

        assertThat(result.getPosts()).hasSize(1);
        verify(postRepository).findByTitleContainingPaginated("Test", 10, 0);
    }

    @Test
    void getPostsWithPaginationWithTagsSearchFiltersCorrectly() {
        when(postRepository.findByTagsPaginated(List.of("java"), 1, 10, 0))
                .thenReturn(List.of(testPost));
        when(postRepository.countByTags(List.of("java"), 1)).thenReturn(1);
        when(postRepository.findTagsByPostId(1L)).thenReturn(List.of("java"));
        when(commentRepository.countByPostId(1L)).thenReturn(0);

        PostListResponseDto result = postService.getPostsWithPagination("#java", 1, 10);

        assertThat(result.getPosts()).hasSize(1);
        verify(postRepository).findByTagsPaginated(List.of("java"), 1, 10, 0);
    }

    @Test
    void updatePostImageWhenPostExistsUpdatesImage() {
        byte[] imageData = new byte[]{1, 2, 3, 4};
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));
        when(postRepository.save(any(Post.class))).thenReturn(testPost);

        postService.updatePostImage(1L, imageData);

        verify(postRepository).findById(1L);
        verify(postRepository).save(argThat(post -> post.getImage() == imageData));
    }

    @Test
    void getPostImageWhenPostExistsReturnsImageData() {
        byte[] imageData = new byte[]{1, 2, 3, 4};
        testPost.setImage(imageData);
        when(postRepository.findById(1L)).thenReturn(Optional.of(testPost));

        byte[] result = postService.getPostImage(1L);

        assertThat(result).isEqualTo(imageData);
        verify(postRepository).findById(1L);
    }
}