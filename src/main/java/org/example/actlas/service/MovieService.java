package org.example.actlas.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.actlas.config.ActlasProperties;
import org.example.actlas.dto.*;
import org.example.actlas.entity.Actor;
import org.example.actlas.entity.Movie;
import org.example.actlas.entity.MovieActor;
import org.example.actlas.repository.ActorRepository;
import org.example.actlas.repository.MovieActorRepository;
import org.example.actlas.repository.MovieRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;
    private final ActorRepository actorRepository;
    private final MovieActorRepository movieActorRepository;
    private final ImdbApiService imdbApiService;
    private final KinobazaParserService kinobazaParserService;
    private final ActlasProperties actlasProperties;

    @Transactional
    public MovieSavedDto fetchAndSaveMovie(String imdbId) {

        Optional<Movie> existing = movieRepository.findByImdbId(imdbId);
        if (existing.isPresent()) {
            Movie movie = existing.get();
            movie.setViewedAt(LocalDateTime.now());
            movieRepository.save(movie);
            log.info("Movie {} already exists, updated viewedAt", imdbId);
            return toSavedDto(movie);
        }

        MovieDetailDto detail = imdbApiService.getMovieDetail(imdbId)
                .orElseThrow(() -> new RuntimeException("Movie not found: " + imdbId));

        String originalTitle = detail.getOriginalTitle();
        if (originalTitle == null || originalTitle.isBlank()) {
            originalTitle = detail.getPrimaryTitle();
        }

        String plotUkr = kinobazaParserService
                .findMovie(detail.getPrimaryTitle(), detail.getStartYear())
                .map(KinobazaMovieDto::getPlotUkr)
                .orElse(null);

        Movie movie = Movie.builder()
                .imdbId(detail.getId())
                .title(detail.getPrimaryTitle())
                .originalTitle(originalTitle)
                .year(detail.getStartYear())
                .imageUrl(detail.getImageUrl())
                .plot(detail.getPlot())
                .plotUkr(plotUkr)
                .rating(detail.getAggregateRating())
                .viewedAt(LocalDateTime.now())
                .build();

        movieRepository.save(movie);

        saveActorsForMovie(movie, detail.getStars());

        log.info("Saved movie: {} ({})", movie.getTitle(), movie.getImdbId());
        return toSavedDto(movie);
    }

    @Transactional
    protected void saveActorsForMovie(Movie movie, List<ActorDto> stars) {
        if (stars == null || stars.isEmpty()) return;

        for (ActorDto starDto : stars) {
            if (starDto.getId() == null || starDto.getId().isBlank()) continue;

            Actor actor = actorRepository.findByImdbId(starDto.getId())
                    .orElseGet(() -> {
                        ActorDto fullData = imdbApiService.getActorDetail(starDto.getId())
                                .orElse(starDto);


                        String bioUkr = kinobazaParserService
                                .findPerson(fullData.getDisplayName())
                                .map(KinobazaPersonDto::getBiography)
                                .orElse(null);

                        return actorRepository.save(Actor.builder()
                                .imdbId(fullData.getId())
                                .name(fullData.getDisplayName())
                                .imageUrl(fullData.getImageUrl())
                                .biography(bioUkr != null ? bioUkr : fullData.getBiography())
                                .birthDate(fullData.getBirthDate())
                                .birthLocation(fullData.getBirthLocation())
                                .build());
                    });

            if (!movieActorRepository.existsByMovieIdAndActorId(movie.getId(), actor.getId())) {
                movieActorRepository.save(MovieActor.builder()
                        .movie(movie)
                        .actor(actor)
                        .build());
            }
        }
    }

    public List<SearchHistoryDto> getHistory() {
        Pageable pageable = PageRequest.of(0, actlasProperties.getHistory().getMaxSize());
        return movieRepository.findRecentMovies(pageable)
                .stream()
                .map(this::toHistoryDto)
                .collect(Collectors.toList());
    }

    public Optional<MovieSavedDto> getMovieWithActors(String imdbId) {
        return movieRepository.findByImdbId(imdbId)
                .map(this::toSavedDto);
    }

    @Transactional
    public void deleteMovie(String imdbId) {
        movieRepository.findByImdbId(imdbId).ifPresent(movie -> {
            movieRepository.delete(movie);
            log.info("Deleted movie: {}", imdbId);
        });
    }


    private MovieSavedDto toSavedDto(Movie movie) {
        List<ActorDto> actors = movieActorRepository
                .findByMovieImdbId(movie.getImdbId())
                .stream()
                .map(ma -> ActorDto.builder()
                        .id(ma.getActor().getImdbId())
                        .displayName(ma.getActor().getName())
                        .imageUrl(ma.getActor().getImageUrl())
                        .birthDate(ma.getActor().getBirthDate())
                        .birthLocation(ma.getActor().getBirthLocation())
                        .biography(ma.getActor().getBiography())
                        .build())
                .collect(Collectors.toList());

        return MovieSavedDto.builder()
                .id(movie.getId())
                .imdbId(movie.getImdbId())
                .title(movie.getTitle())
                .originalTitle(movie.getOriginalTitle())
                .year(movie.getYear())
                .imageUrl(movie.getImageUrl())
                .plot(movie.getPlot())
                .plotUkr(movie.getPlotUkr())
                .rating(movie.getRating())
                .viewedAt(movie.getViewedAt())
                .actors(actors)
                .build();
    }

    private SearchHistoryDto toHistoryDto(Movie movie) {
        return SearchHistoryDto.builder()
                .id(movie.getId())
                .imdbId(movie.getImdbId())
                .title(movie.getTitle())
                .year(movie.getYear())
                .imageUrl(movie.getImageUrl())
                .rating(movie.getRating())
                .viewedAt(movie.getViewedAt())
                .build();
    }
}