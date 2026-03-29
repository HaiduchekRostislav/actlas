package org.example.actlas.repository;

import org.example.actlas.entity.Movie;
import org.example.actlas.entity.MovieActor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovieActorRepository extends JpaRepository<MovieActor, Long> {


    List<MovieActor> findByMovieImdbId(String imdbId);

    List<MovieActor> findByActorImdbId(String actorImdbId);

    boolean existsByMovieIdAndActorId(Long movieId, Long actorId);


    @Query("""
        SELECT ma.movie FROM MovieActor ma
        WHERE ma.actor.imdbId IN :actorImdbIds
        GROUP BY ma.movie
        HAVING COUNT(DISTINCT ma.actor.imdbId) = :actorCount
    """)
    List<Movie> findMoviesWithAllActors(
            @Param("actorImdbIds") List<String> actorImdbIds,
            @Param("actorCount") long actorCount
    );
}
