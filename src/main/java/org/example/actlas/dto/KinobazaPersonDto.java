package org.example.actlas.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KinobazaPersonDto {
    private String name;
    private String originalName;
    private String biography;
    private String profileUrl;
    private String birthDate;
    private String height;
}
