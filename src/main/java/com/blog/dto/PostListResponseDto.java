package com.blog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostListResponseDto {
    private List<PostDto> posts;
    private Boolean hasPrev;
    private Boolean hasNext;
    private Integer lastPage;
}