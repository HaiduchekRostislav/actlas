package org.example.actlas.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovieSearchResultDto {
    private String imdbId;
    private String title;
    private String year;
    private String imageUrl;
    private String type;
}
