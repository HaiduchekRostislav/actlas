package org.example.actlas.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovieSavedDto {
    private Long id;
    private String imdbId;
    private String title;
    private String originalTitle;
    private Integer year;
    private String imageUrl;
    private String plot;
    private String plotUkr;
    private Double rating;
    private LocalDateTime viewedAt;
    private List<ActorDto> actors;
}
