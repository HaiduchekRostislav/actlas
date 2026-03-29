package org.example.actlas.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KinobazaMovieDto {
    private String title;
    private String originalTitle;
    private String plotUkr;
    private String profileUrl;
    private String year;
    private Double rating;
}
