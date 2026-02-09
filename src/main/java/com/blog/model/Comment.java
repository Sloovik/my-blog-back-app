package com.blog.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {
    private Long id;
    private String text;
    private Long postId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void initializeCreatedAt() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
}