package org.example.actlas.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.actlas.config.ImdbProperties;
import org.example.actlas.config.KinobazaProperties;
import org.example.actlas.dto.KinobazaMovieDto;
import org.example.actlas.dto.KinobazaPersonDto;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class KinobazaParserService {

    private final KinobazaProperties kinobazaProperties;
    private final ImdbProperties imdbProperties;


    public Optional<KinobazaMovieDto> findMovie(String title, Integer year) {
        try {
            String searchUrl = kinobazaProperties.getSearchUrl()
                    + "?q=" + URLEncoder.encode(title, StandardCharsets.UTF_8);

            log.info("Searching kinobaza movie: {} ({})", title, year);

            Document doc = Jsoup.connect(searchUrl)
                    .userAgent(imdbProperties.getJsoup().getUserAgent())
                    .timeout(imdbProperties.getJsoup().getTimeout())
                    .header("Accept-Language", "uk-UA,uk;q=0.9")
                    .get();

            String movieUrl = findMovieUrl(doc, year);
            if (movieUrl == null) {
                log.warn("Movie not found on kinobaza: {}", title);
                return Optional.empty();
            }

            log.info("Found movie page: {}", movieUrl);
            return parseMovie(movieUrl);

        } catch (Exception e) {
            log.error("Error searching kinobaza movie: {}", title, e);
            return Optional.empty();
        }
    }

    private String findMovieUrl(Document doc, Integer year) {
        Elements links = doc.select("a[href^='/titles/']");

        if (links.isEmpty()) {
            log.warn("No title links found on kinobaza search page");
            return null;
        }

        if (year != null) {
            for (Element link : links) {
                Element parent = link.parent();
                if (parent != null) {
                    String parentText = parent.text();
                    if (parentText.contains(String.valueOf(year))) {
                        String href = link.attr("href");
                        return "https://kinobaza.com.ua" + href;
                    }
                }
            }
        }

        return "https://kinobaza.com.ua" + Objects.requireNonNull(links.first()).attr("href");
    }

    private Optional<KinobazaMovieDto> parseMovie(String movieUrl) {
        try {
            Document page = Jsoup.connect(movieUrl)
                    .userAgent(imdbProperties.getJsoup().getUserAgent())
                    .timeout(imdbProperties.getJsoup().getTimeout())
                    .header("Accept-Language", "uk-UA,uk;q=0.9")
                    .get();

            String title = null;
            Element titleEl = page.selectFirst("h1[itemprop='name'], h1");
            if (titleEl != null) title = titleEl.text().trim();

            String originalTitle = null;
            Element origEl = page.selectFirst("h4.text-muted, h2.text-muted");
            if (origEl != null) originalTitle = origEl.text().trim();

            String plotUkr = parseDescription(page);
            log.info("Parsed movie '{}', plotUkr length: {}",
                    title, plotUkr != null ? plotUkr.length() : 0);

            return Optional.of(KinobazaMovieDto.builder()
                    .title(title)
                    .originalTitle(originalTitle)
                    .plotUkr(plotUkr)
                    .profileUrl(movieUrl)
                    .build());

        } catch (Exception e) {
            log.error("Error parsing kinobaza movie: {}", movieUrl, e);
            return Optional.empty();
        }
    }


    public Optional<KinobazaPersonDto> findPerson(String actorName) {
        try {
            String searchUrl = kinobazaProperties.getSearchUrl()
                    + "?q=" + URLEncoder.encode(actorName, StandardCharsets.UTF_8);

            log.info("Searching kinobaza person: {}", actorName);

            Document doc = Jsoup.connect(searchUrl)
                    .userAgent(imdbProperties.getJsoup().getUserAgent())
                    .timeout(imdbProperties.getJsoup().getTimeout())
                    .header("Accept-Language", "uk-UA,uk;q=0.9")
                    .get();

            String personUrl = findPersonUrl(doc);
            if (personUrl == null) {
                log.warn("Person not found on kinobaza: {}", actorName);
                return Optional.empty();
            }

            log.info("Found person page: {}", personUrl);
            return parsePerson(personUrl);

        } catch (Exception e) {
            log.error("Error searching kinobaza person: {}", actorName, e);
            return Optional.empty();
        }
    }

    private String findPersonUrl(Document doc) {
        Element link = doc.selectFirst("a[href^='/persons/']");
        if (link == null) return null;
        return "https://kinobaza.com.ua" + link.attr("href");
    }

    private Optional<KinobazaPersonDto> parsePerson(String personUrl) {
        try {
            Document page = Jsoup.connect(personUrl)
                    .userAgent(imdbProperties.getJsoup().getUserAgent())
                    .timeout(imdbProperties.getJsoup().getTimeout())
                    .header("Accept-Language", "uk-UA,uk;q=0.9")
                    .get();

            String name = null;
            Element nameEl = page.selectFirst("h1");
            if (nameEl != null) name = nameEl.text().trim();

            String originalName = null;
            Element origEl = page.selectFirst("h4.text-muted");
            if (origEl != null) originalName = origEl.text().trim();

            String birthDate = null;
            String height = null;
            for (Element p : page.select("p")) {
                String text = p.text();
                if (text.contains("Дата народження") && birthDate == null) {
                    birthDate = text.replace("Дата народження:", "").trim();
                }
                if (text.contains("Зріст") && height == null) {
                    height = text.replace("Зріст:", "").trim();
                }
            }

            String biography = parseDescription(page);
            log.info("Parsed person '{}', bio length: {}",
                    name, biography != null ? biography.length() : 0);

            return Optional.of(KinobazaPersonDto.builder()
                    .name(name)
                    .originalName(originalName)
                    .biography(biography)
                    .profileUrl(personUrl)
                    .birthDate(birthDate)
                    .height(height)
                    .build());

        } catch (Exception e) {
            log.error("Error parsing kinobaza person: {}", personUrl, e);
            return Optional.empty();
        }
    }


    private String parseDescription(Document page) {
        Element wrap = page.selectFirst(".read-more-wrap");
        if (wrap == null) {
            wrap = page.selectFirst("[itemprop='description'], .description, .plot");
        }

        if (wrap == null) {
            log.warn("No description block found on page: {}", page.baseUri());
            return null;
        }

        for (Element span : wrap.select("span.read-more-target")) {
            span.unwrap();
        }

        wrap.select(".read-more-dots, .read-more-trigger, input[type=checkbox], label").remove();

        StringBuilder result = new StringBuilder();
        wrap.select("p").forEach(p -> {
            String text = p.text().trim();
            if (!text.isBlank()) result.append(text).append("\n\n");
        });


        if (result.isEmpty()) {
            String text = wrap.text().trim();
            if (!text.isBlank()) return text;
        }

        String text = result.toString().trim();
        return text.isBlank() ? null : text;
    }
}