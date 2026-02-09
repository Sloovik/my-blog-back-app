package com.blog.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException postNotFound(Long id) {
        return new ResourceNotFoundException("Post with id " + id + " not found");
    }

    public static ResourceNotFoundException commentNotFoundInPost(Long postId, Long commentId) {
        return new ResourceNotFoundException(
                "Comment with id " + commentId + " not found in post " + postId
        );
    }
}