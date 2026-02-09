package com.blog.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {
    private Long id;
    private String title;
    private String text;
    private Set<String> tags;
    private Integer likesCount;
    private byte[] image;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void initializeCreatedAt() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public void initializeLikesCount() {
        if (this.likesCount == null) {
            this.likesCount = 0;
        }
    }

    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
}