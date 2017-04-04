package com.tstorm.seed;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Actor {

    public abstract UUID getUUID();
    public abstract Pattern getPattern();

    private final Name name;
    private final UUID id;
    private final List<Movie> titles = new ArrayList<>();

    public Actor(Name n) {
        name = n;
        id = getUUID();
    }

    public String getId() {
        return id.toString();
    }

    /**
     * Returns a "formatted" in lowercase and seperated by underscores.
     *   e.g. john_doe
     *
     * @return the formatted name
     */
    public String getFormattedName() {
        StringBuilder builder = new StringBuilder();
        builder.append(name.getFirstName().toLowerCase());
        if (name.getLastName().isPresent()) {
            builder.append("_" + name.getLastName().get().toLowerCase());
        }
        return builder.toString().replaceAll(" ", "_");
    }

    public Optional<String> getNickName() {
        return name.getNickName();
    }

    public Optional<String> getSuffix() {
        return name.getSuffix();
    }

    public String getDisplayName() {
        StringBuilder builder = new StringBuilder();
        builder.append(name.getFirstName());
        if (name.getNickName().isPresent())
            builder.append(" " + name.getNickName().get());
        if (name.getLastName().isPresent())
            builder.append(" " + name.getLastName().get());
        return builder.toString();
    }

    /**
     * Add a a movie to this actor's list of movies
     *
     * @param title The movie title from the IMDb .list file
     */
    public void addTitle(String title) {
        titles.add(createMovie(title));
    }

    public List<Movie> getTitles() {
        return titles;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(name.getFirstName());
        name.getNickName().ifPresent(s -> builder.append(" " + s));
        name.getLastName().ifPresent(s -> builder.append(" " + s));
        name.getSuffix().ifPresent(s -> builder.append(" " + s));
        return builder.toString();
    }

    /**
     * Creates a {@link Movie} object given a line containing a title and year
     *
     * @param movieTitle the line from the IMDb data
     * @return a new {@link Movie} object
     * @throws RuntimeException
     */
    private Movie createMovie(String movieTitle) {
        final String ANY_CHARACTER = ".*";
        if (movieTitle.matches(ANY_CHARACTER + getPattern().pattern() + ANY_CHARACTER)) {
            Matcher m = getPattern().matcher(movieTitle);
            if (m.find()) {
                String title = movieTitle.substring(0, m.start() - 1).replaceAll(" ", "_");
                String year = movieTitle.substring(m.start() + 1, m.end() - 1);
                return new Movie(title.contains(" ") ? formatTitle(title.toLowerCase()) : title.toLowerCase(), year);
            } else {
                throw new RuntimeException();
            }
        } else {
            System.err.println("Error: syntax expected: title (year)\n but found " + movieTitle);
            throw new RuntimeException();
        }
    }

    // TODO remove this function, it shouldn't be needed and is buggy
    private String formatTitle(String title) {
        final Pattern SINGLE_SPACE_BETWEEN_WORDS = Pattern.compile("\\S \\S");
        Matcher m = SINGLE_SPACE_BETWEEN_WORDS.matcher(title);
        StringBuilder formattedString = new StringBuilder();
        int i = 0;
        while (m.find()) {
            formattedString.append(title.substring(i, m.start() + 1));
            formattedString.append(title.substring(m.start() + 1, m.end() - 1).replaceAll(" ", "_"));
            i = m.end() - 1;
        }
        formattedString.append(title.substring(i));
        return formattedString.toString().replaceAll("\\s", "");
    }

}