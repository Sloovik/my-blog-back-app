package com.blog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostDto {
    private Long id;
    private String title;
    private String text;
    private Set<String> tags;
    private Integer likesCount;
    private Integer commentsCount;

    public PostDto truncateText() {
        if (this.text != null && this.text.length() > 128) {
            this.text = this.text.substring(0, 128) + "…";
        }
        return this;
    }
}