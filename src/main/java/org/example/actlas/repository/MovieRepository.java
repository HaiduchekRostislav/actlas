package org.example.actlas.repository;

import org.example.actlas.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {

    Optional<Movie> findByImdbId(String imdbId);

    @Query("SELECT m FROM Movie m ORDER BY m.viewedAt DESC")
    List<Movie> findRecentMovies(Pageable pageable);
}
