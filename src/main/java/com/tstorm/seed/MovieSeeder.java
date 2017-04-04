package com.tstorm.seed;

import com.datastax.driver.core.utils.UUIDs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MovieSeeder {
    static final Pattern YEAR_PATTERN = Pattern.compile("\\([0-9|\\?]{4}+(.[IVXLCDM]*)?\\)");
    private static final String INSERT_MOVIE = "INSERT INTO movies (title, movie_id, year) VALUES (\'%s\', %s, " +
            "\'%s\')";
/**
 * Parses the IMDb database dump line by line adding movie records (title and year) to the database
 *
 * @param inputStreamReader the text input following IMDb format
 * @param logger            SeedLogger used to monitor progress
 * @throws IOException
 */
    private static void parseMovies(InputStreamReader inputStreamReader, SeedLogger logger) throws IOException {
        final BufferedReader reader = new BufferedReader(inputStreamReader);
        String line = reader.readLine();
        while (line != null) {
            if (shouldExclude(line)) {
                line = reader.readLine();
                continue; // skip TV shows and TV movies
            }
            String title = line.substring(0, line.indexOf(" ("))
                .replaceAll(" ", "_")
                .replaceAll("'", "''")
                .toLowerCase();
            Matcher m = YEAR_PATTERN.matcher(line);
            if (m.find()) {
                String year = line.substring(m.start() + 1, m.end() - 1);
                // Insert movie record into the database
                System.out.println(String.format(INSERT_MOVIE, title, UUIDs.random().toString(), year));
            } else {
                throw new IOException("Didn't find the year for line: " + line);
            }
            logger.log(line);
            line = reader.readLine();
        }
        logger.close();
    }

    private static boolean shouldExclude(String line) {
        return false;
    }

}
