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
public class ActorDto {
    private String id;
    private String displayName;
    private String imageUrl;
    private String biography;
    private String birthLocation;
    private List<String> primaryProfessions;
    private String birthDate;
}
