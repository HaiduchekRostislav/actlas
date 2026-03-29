package org.example.actlas.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.actlas.dto.ActorDto;
import org.example.actlas.dto.MovieDetailDto;
import org.example.actlas.dto.MovieSearchResultDto;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ImdbApiService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public List<MovieSearchResultDto> searchTitles(String query) {
        try {
            log.info("Searching titles: {}", query);

            String response = restClient.get()
                    .uri(uri -> uri
                            .path("/search/titles")
                            .queryParam("query", query)
                            .build())
                    .retrieve()
                    .body(String.class);

            return parseSearchResults(response);

        } catch (Exception e) {
            log.error("Error searching titles: {}", query, e);
            return Collections.emptyList();
        }
    }

    public Optional<MovieDetailDto> getMovieDetail(String imdbId) {
        try {
            log.info("Fetching movie detail: {}", imdbId);

            String response = restClient.get()
                    .uri("/titles/{id}", imdbId)
                    .retrieve()
                    .body(String.class);

            return Optional.of(parseMovieDetail(response));

        } catch (Exception e) {
            log.error("Error fetching movie detail: {}", imdbId, e);
            return Optional.empty();
        }
    }


    public Optional<ActorDto> getActorDetail(String nameId) {
        try {
            log.info("Fetching actor detail: {}", nameId);

            String response = restClient.get()
                    .uri("/names/{id}", nameId)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            return Optional.of(parsePerson(root));

        } catch (Exception e) {
            log.error("Error fetching actor detail: {}", nameId, e);
            return Optional.empty();
        }
    }

    private List<MovieSearchResultDto> parseSearchResults(String json) {
        JsonNode root = objectMapper.readTree(json);
        JsonNode titles = root.path("titles");

        List<MovieSearchResultDto> results = new ArrayList<>();

        titles.forEach(node -> {
            String imageUrl = textOrNull(node.path("primaryImage").path("url"));
            String year     = node.path("startYear").isInt()
                    ? String.valueOf(node.path("startYear").asInt())
                    : null;

            results.add(MovieSearchResultDto.builder()
                    .imdbId(textOrNull(node.path("id")))
                    .title(textOrEmpty(node.path("primaryTitle")))
                    .year(year)
                    .type(textOrNull(node.path("type")))
                    .imageUrl(imageUrl)
                    .build());
        });

        return results;
    }

    private MovieDetailDto parseMovieDetail(String json) {
        JsonNode root = objectMapper.readTree(json);

        List<ActorDto> stars     = parsePersonList(root.path("stars"));
        List<ActorDto> directors = parsePersonList(root.path("directors"));

        List<String> genres = new ArrayList<>();
        root.path("genres").forEach(g -> genres.add(g.asText()));

        String imageUrl = textOrNull(root.path("primaryImage").path("url"));

        JsonNode ratingNode = root.path("rating");
        Double rating = null;
        if (!ratingNode.isMissingNode() && !ratingNode.isNull()) {
            double val = ratingNode.path("aggregateRating").asDouble(0);
            if (val > 0) rating = val;
        }

        return MovieDetailDto.builder()
                .id(textOrNull(root.path("id")))
                .primaryTitle(textOrEmpty(root.path("primaryTitle")))
                .originalTitle(textOrNull(root.path("originalTitle")))
                .plot(textOrNull(root.path("plot")))
                .startYear(root.path("startYear").isInt() ? root.path("startYear").asInt() : null)
                .type(textOrNull(root.path("type")))
                .aggregateRating(rating)
                .imageUrl(imageUrl)
                .genres(genres)
                .stars(stars)
                .directors(directors)
                .build();
    }

    private List<ActorDto> parsePersonList(JsonNode arrayNode) {
        List<ActorDto> list = new ArrayList<>();
        if (arrayNode.isMissingNode() || arrayNode.isNull()) return list;
        arrayNode.forEach(node -> list.add(parsePerson(node)));
        return list;
    }

    private ActorDto parsePerson(JsonNode node) {
        List<String> professions = new ArrayList<>();
        node.path("primaryProfessions").forEach(p -> professions.add(p.asText()));

        return ActorDto.builder()
                .id(textOrNull(node.path("id")))
                .displayName(textOrEmpty(node.path("displayName")))
                .imageUrl(textOrNull(node.path("primaryImage").path("url")))
                .biography(textOrNull(node.path("biography")))
                .birthLocation(textOrNull(node.path("birthLocation")))
                .birthDate(formatBirthDate(node.path("birthDate")))
                .primaryProfessions(professions)
                .build();
    }


    private String textOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return null;
        String text = node.asText();
        return text.isBlank() ? null : text;
    }

    private String textOrEmpty(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return "";
        return node.asText();
    }

    private String formatBirthDate(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return null;
        int year  = node.path("year").asInt(0);
        int month = node.path("month").asInt(0);
        int day   = node.path("day").asInt(0);
        if (year == 0) return null;
        return String.format("%d-%02d-%02d", year, month, day);
    }
}