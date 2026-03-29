package org.example.actlas.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.actlas.dto.ActorDto;
import org.example.actlas.dto.MovieSavedDto;
import org.example.actlas.dto.MovieSearchResultDto;
import org.example.actlas.dto.SearchHistoryDto;
import org.example.actlas.service.ActorService;
import org.example.actlas.service.ImdbApiService;
import org.example.actlas.service.MovieService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class ApiController {

    private final ImdbApiService imdbApiService;
    private final MovieService movieService;
    private final ActorService actorService;

    @GetMapping("/search")
    public ResponseEntity<List<MovieSearchResultDto>> search(@RequestParam String query) {
        if (query == null || query.trim().length() < 2) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        return ResponseEntity.ok(imdbApiService.searchTitles(query.trim()));
    }


    @PostMapping("/movies/{imdbId}")
    public ResponseEntity<MovieSavedDto> fetchMovie(@PathVariable String imdbId) {
        try {
            return ResponseEntity.ok(movieService.fetchAndSaveMovie(imdbId));
        } catch (RuntimeException e) {
            log.error("Error fetching movie: {}", imdbId, e);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/movies/{imdbId}")
    public ResponseEntity<MovieSavedDto> getMovie(@PathVariable String imdbId) {
        return movieService.getMovieWithActors(imdbId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/movies/{imdbId}")
    public ResponseEntity<Void> deleteMovie(@PathVariable String imdbId) {
        movieService.deleteMovie(imdbId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/history")
    public ResponseEntity<List<SearchHistoryDto>> getHistory() {
        return ResponseEntity.ok(movieService.getHistory());
    }


    @GetMapping("/actors/{imdbId}")
    public ResponseEntity<ActorDto> getActor(@PathVariable String imdbId) {
        return actorService.getActorWithUkrBio(imdbId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/actors/{imdbId}/movies")
    public ResponseEntity<List<SearchHistoryDto>> getActorMovies(@PathVariable String imdbId) {
        return ResponseEntity.ok(actorService.getMoviesForActor(imdbId));
    }

    @GetMapping("/actors/common-movies")
    public ResponseEntity<List<SearchHistoryDto>> getCommonMovies(
            @RequestParam List<String> actorIds) {
        if (actorIds == null || actorIds.size() < 2) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(actorService.findCommonMovies(actorIds));
    }
}