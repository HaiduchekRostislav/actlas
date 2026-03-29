package org.example.actlas.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.actlas.dto.ActorDto;
import org.example.actlas.dto.SearchHistoryDto;
import org.example.actlas.entity.Actor;
import org.example.actlas.repository.ActorRepository;
import org.example.actlas.repository.MovieActorRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ActorService {

    private final ActorRepository actorRepository;
    private final MovieActorRepository movieActorRepository;
    private final ImdbApiService imdbApiService;
    private final KinobazaParserService kinobazaParserService;

    public Optional<ActorDto> getActorWithUkrBio(String imdbId) {
        Optional<Actor> actorInDb = actorRepository.findByImdbId(imdbId);

        if (actorInDb.isPresent()) {
            Actor actor = actorInDb.get();

            // Якщо біографії ще немає — спробуємо підтягнути
            if (actor.getBiography() == null || actor.getBiography().isBlank()) {
                log.info("Biography missing for {}, fetching from kinobaza", actor.getName());
                kinobazaParserService.findPerson(actor.getName())
                        .ifPresent(kino -> {
                            if (kino.getBiography() != null) {
                                actor.setBiography(kino.getBiography());
                                actorRepository.save(actor); // зберігаємо в БД
                                log.info("Saved biography for {}", actor.getName());
                            }
                        });
            }

            return Optional.of(toDto(actor));
        }

        return imdbApiService.getActorDetail(imdbId);
    }


    public List<SearchHistoryDto> getMoviesForActor(String actorImdbId) {
        return movieActorRepository.findByActorImdbId(actorImdbId)
                .stream()
                .map(ma -> SearchHistoryDto.builder()
                        .id(ma.getMovie().getId())
                        .imdbId(ma.getMovie().getImdbId())
                        .title(ma.getMovie().getTitle())
                        .year(ma.getMovie().getYear())
                        .imageUrl(ma.getMovie().getImageUrl())
                        .rating(ma.getMovie().getRating())
                        .viewedAt(ma.getMovie().getViewedAt())
                        .build())
                .collect(Collectors.toList());
    }

    public List<SearchHistoryDto> findCommonMovies(List<String> actorImdbIds) {
        return movieActorRepository
                .findMoviesWithAllActors(actorImdbIds, actorImdbIds.size())
                .stream()
                .map(movie -> SearchHistoryDto.builder()
                        .id(movie.getId())
                        .imdbId(movie.getImdbId())
                        .title(movie.getTitle())
                        .year(movie.getYear())
                        .imageUrl(movie.getImageUrl())
                        .rating(movie.getRating())
                        .viewedAt(movie.getViewedAt())
                        .build())
                .collect(Collectors.toList());
    }

    private ActorDto toDto(Actor actor) {
        return ActorDto.builder()
                .id(actor.getImdbId())
                .displayName(actor.getName())
                .imageUrl(actor.getImageUrl())
                .biography(actor.getBiography())
                .birthDate(actor.getBirthDate())
                .birthLocation(actor.getBirthLocation())
                .build();
    }
}