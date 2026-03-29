package org.example.actlas.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.actlas.service.MovieService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
@Slf4j
public class PageController {

    private final MovieService movieService;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("history", movieService.getHistory());
        return "index";
    }

    @GetMapping("/movies/{imdbId}")
    public String moviePage(@PathVariable String imdbId, Model model) {
        movieService.getMovieWithActors(imdbId).ifPresent(movie -> {
            model.addAttribute("movie", movie);
        });
        return "movie";
    }
}