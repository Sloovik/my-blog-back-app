package com.blog.service;

import com.blog.dao.CommentRepository;
import com.blog.dao.PostRepository;
import com.blog.dto.PostDto;
import com.blog.dto.PostListResponseDto;
import com.blog.exception.ResourceNotFoundException;
import com.blog.model.Post;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public PostListResponseDto getPostsWithPagination(String search, int pageNumber, int pageSize) {
        log.debug("Getting posts with search: '{}', page: {}, pageSize: {}", search, pageNumber, pageSize);

        SearchParams params = parseSearchParams(search);

        List<Post> posts = getPostsBySearchParams(params, pageNumber, pageSize);
        int totalCount = getTotalCountBySearchParams(params);

        enrichPosts(posts);

        List<PostDto> postDtos = posts.stream()
                .map(this::convertToDto)
                .map(PostDto::truncateText)
                .collect(Collectors.toList());

        int totalPages = totalCount == 0 ? 1 : (totalCount + pageSize - 1) / pageSize;
        boolean hasPrev = pageNumber > 1;
        boolean hasNext = pageNumber < totalPages;

        return PostListResponseDto.builder()
                .posts(postDtos)
                .hasPrev(hasPrev)
                .hasNext(hasNext)
                .lastPage(totalPages)
                .build();
    }

    @Transactional(readOnly = true)
    public PostDto getPostById(Long id) {
        log.debug("Getting post with id: {}", id);
        Post post = postRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.postNotFound(id));
        enrichPost(post);
        return convertToDto(post);
    }

    @Transactional
    public PostDto createPost(PostDto postDto) {
        log.debug("Creating post with title: {}", postDto.getTitle());

        Post post = Post.builder()
                .title(postDto.getTitle())
                .text(postDto.getText())
                .tags(postDto.getTags() != null ? postDto.getTags() : Set.of())
                .build();

        post.initializeCreatedAt();
        post.initializeLikesCount();
        post.updateTimestamp();

        Post savedPost = postRepository.save(post);
        saveTags(savedPost.getId(), post.getTags());

        enrichPost(savedPost);
        return convertToDto(savedPost);
    }

    @Transactional
    public PostDto updatePost(Long id, PostDto postDto) {
        log.debug("Updating post with id: {}", id);
        Post post = postRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.postNotFound(id));

        post.setTitle(postDto.getTitle());
        post.setText(postDto.getText());
        post.updateTimestamp();

        Post updatedPost = postRepository.save(post);

        deleteTags(id);
        saveTags(id, postDto.getTags() != null ? postDto.getTags() : Set.of());

        enrichPost(updatedPost);
        return convertToDto(updatedPost);
    }

    @Transactional
    public void deletePost(Long id) {
        log.debug("Deleting post with id: {}", id);
        if (!postRepository.existsById(id)) {
            throw ResourceNotFoundException.postNotFound(id);
        }
        commentRepository.deleteAllByPostId(id);
        deleteTags(id);
        postRepository.deleteById(id);
    }

    @Transactional
    public Integer addLike(Long id) {
        log.debug("Adding like to post with id: {}", id);
        Post post = postRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.postNotFound(id));
        post.setLikesCount(post.getLikesCount() + 1);
        post.updateTimestamp();
        postRepository.save(post);
        return post.getLikesCount();
    }

    @Transactional
    public void updatePostImage(Long id, byte[] imageData) {
        log.debug("Updating image for post with id: {}", id);
        Post post = postRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.postNotFound(id));
        post.setImage(imageData);
        post.updateTimestamp();
        postRepository.save(post);
    }

    @Transactional(readOnly = true)
    public byte[] getPostImage(Long id) {
        log.debug("Getting image for post with id: {}", id);
        Post post = postRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.postNotFound(id));
        return post.getImage();
    }

    private SearchParams parseSearchParams(String search) {
        if (search == null || search.trim().isEmpty()) {
            return new SearchParams(null, List.of());
        }
        List<String> tags = new ArrayList<>();
        List<String> searchWords = new ArrayList<>();

        for (String word : search.trim().split("\\s+")) {
            if (word.startsWith("#")) {
                tags.add(word.substring(1));
            } else {
                searchWords.add(word);
            }
        }
        String searchText = String.join(" ", searchWords).trim();
        if (searchText.isEmpty()) searchText = null;
        return new SearchParams(searchText, tags);
    }

    private List<Post> getPostsBySearchParams(SearchParams params, int pageNumber, int pageSize) {
        int offset = (pageNumber - 1) * pageSize;
        if (params.searchText != null && !params.tags.isEmpty()) {
            return postRepository.findByTitleAndTagsPaginated(params.searchText, params.tags,
                    params.tags.size(), pageSize, offset);
        } else if (params.searchText != null) {
            return postRepository.findByTitleContainingPaginated(params.searchText, pageSize, offset);
        } else if (!params.tags.isEmpty()) {
            return postRepository.findByTagsPaginated(params.tags, params.tags.size(), pageSize, offset);
        } else {
            return postRepository.findByTitleContainingPaginated("", pageSize, offset);
        }
    }

    private int getTotalCountBySearchParams(SearchParams params) {
        if (params.searchText != null && !params.tags.isEmpty()) {
            return postRepository.countByTitleAndTags(params.searchText, params.tags, params.tags.size());
        } else if (params.searchText != null) {
            return postRepository.countByTitleContaining(params.searchText);
        } else if (!params.tags.isEmpty()) {
            return postRepository.countByTags(params.tags, params.tags.size());
        } else {
            return postRepository.countByTitleContaining("");
        }
    }

    private void enrichPost(Post post) {
        List<String> tags = postRepository.findTagsByPostId(post.getId());
        post.setTags(Set.copyOf(tags));
    }

    private void enrichPosts(List<Post> posts) {
        posts.forEach(this::enrichPost);
    }

    private PostDto convertToDto(Post post) {
        int commentsCount = commentRepository.countByPostId(post.getId());
        return PostDto.builder()
                .id(post.getId())
                .title(post.getTitle())
                .text(post.getText())
                .tags(post.getTags())
                .likesCount(post.getLikesCount())
                .commentsCount(commentsCount)
                .build();
    }

    private void saveTags(Long postId, Set<String> tags) {
        if (tags == null || tags.isEmpty()) return;
        String sql = "INSERT INTO post_tags (post_id, tag) VALUES (?, ?)";
        for (String tag : tags) {
            jdbcTemplate.update(sql, postId, tag);
        }
    }

    private void deleteTags(Long postId) {
        jdbcTemplate.update("DELETE FROM post_tags WHERE post_id = ?", postId);
    }

    private static class SearchParams {
        String searchText;
        List<String> tags;

        SearchParams(String searchText, List<String> tags) {
            this.searchText = searchText;
            this.tags = tags;
        }
    }
}