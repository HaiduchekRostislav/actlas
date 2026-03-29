package org.example.actlas.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovieDetailDto {
    private String id;
    private String primaryTitle;
    private String originalTitle;
    private String plot;
    private Integer startYear;
    private String type;
    private Double aggregateRating;
    private String imageUrl;
    private List<String> genres;
    private List<ActorDto> stars;
    private List<ActorDto> directors;
}
