package org.example.actlas.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchHistoryDto {
    private Long id;
    private String imdbId;
    private String title;
    private Integer year;
    private String imageUrl;
    private Double rating;
    private LocalDateTime viewedAt;
}