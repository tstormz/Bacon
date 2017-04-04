package com.tstorm.seed;

import com.datastax.driver.core.*;
import com.datastax.driver.core.exceptions.OperationTimedOutException;
import io.netty.handler.timeout.TimeoutException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class BaconSeeder extends ActorSeeder {
    private static final String INSERT_ACTOR = "INSERT INTO actors (name, id, display_name) VALUES (\'%s\', %s, " +
            "\'%s\')";
    private static final String INSERT_ACTOR_WITH_NICKNAME = "INSERT INTO actors (name, id, display_name, nickname)" +
            " VALUES (\'%s\', %s, \'%s\', \'%s\')";
    private static final String INSERT_ACTOR_WITH_SUFFIX = "INSERT INTO actors (name, id, display_name, suffix) " +
            "VALUES (\'%s\', %s, \'%s\', \'%s\')";
    private static final String INSERT_ACTOR_WITH_NICKNAME_AND_SUFFIX = "INSERT INTO actors (name, id, " +
            "display_name, nickname, suffix) VALUES (\'%s\', %s, \'%s\', \'%s\', \'%s\')";
    private static final String SELECT_MOVIE = "SELECT * FROM movies WHERE title = \'%s\'";
    private static final String INSERT_MOVIE_BY_ACTOR = "INSERT INTO movies_by_actors (actor_id, movie_id, " +
            "movie_title, movie_year) VALUES (%s, %s, \'%s\', \'%s\')";
    private static final String ADD_CAST_MEMBER = "UPDATE movies SET cast[%s]=\'%s\' WHERE title=\'%s\' AND " +
            "movie_id=%s";

    private static SeedLogger logger;
    private final Cluster cluster;
    private final Session session;

    public BaconSeeder(int numberOfThreads) {
        super(numberOfThreads);
        cluster = Cluster.builder()
                .addContactPoint("52.14.185.37")
                .build();
        cluster.init();
        session = cluster.connect("gameplay");
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("BaconSeeder requires a filename as the first argument");
            return;
        }
        ActorSeeder seeder = new BaconSeeder(16);
        FileInputStream file = new FileInputStream(new File(args[0]));
        logger = new SeedLogger(args[0], 1000);
        try {
            seeder.parseActors(file, Optional.of(logger));
        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
            ((BaconSeeder) seeder).shutdown(file);
        }
        // assuming seeder is BaconSeeder because this class is BaconSeeder
        ((BaconSeeder) seeder).shutdown(file);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Actor createActor(String line) {
        return new BaconActor(getName(line));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void insertActor(Actor a) {
        String formattedName = a.getFormattedName().replaceAll("'", "''");
        String displayName = a.getDisplayName().replaceAll("'", "''");
        if (a.getNickName().isPresent() && a.getSuffix().isPresent()) {
            session.execute(String.format(BaconSeeder.INSERT_ACTOR_WITH_NICKNAME_AND_SUFFIX, formattedName, a.getId(),
                    displayName, a.getNickName().get(), a.getSuffix().get()));
        } else if (a.getNickName().isPresent()) {
            session.execute(String.format(BaconSeeder.INSERT_ACTOR_WITH_NICKNAME, formattedName, a.getId(),
                    displayName, a.getNickName().get()));
        } else if (a.getSuffix().isPresent()) {
            session.execute(String.format(BaconSeeder.INSERT_ACTOR_WITH_SUFFIX, formattedName, a.getId(),
                    displayName, a.getSuffix().get()));
        } else {
            session.execute(String.format(BaconSeeder.INSERT_ACTOR, formattedName, a.getId(), displayName));
        }
    }

    /**
     * Filters out lines containing {{SUSPENDED}} and (TV) or any item starting with
     * a double quote, which tags the title as a TV show
     *
     * @param line the line to possibly exclude
     * @return true if movie is suspended or for TV
     */
    @Override
    public boolean shouldExclude(String line) {
        final String SUSPENDED = "{{SUSPENDED}}";
        final String TV_MOVIE = "(TV)";
        return line.substring(0, 1).equals("\"") || line.contains(SUSPENDED) || line.contains(TV_MOVIE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void insertMovieByActor(Actor a, Movie m) {
        String query = String.format(BaconSeeder.SELECT_MOVIE, m.title.replaceAll("'", "''"));
        boolean foundMovieYearMatch = false;
        ResultSetFuture future = session.executeAsync(query);
        try {
            ResultSet results = future.get(15, TimeUnit.SECONDS); // blocking call
            for (Row row : results.all()) {
                if (row.getString("year").equals(m.year)) {
                    try {
                        session.execute(String.format(BaconSeeder.INSERT_MOVIE_BY_ACTOR, a.getId(), row.getUUID("movie_id")
                                .toString(), m.title.replaceAll("'", "''"), m.year));
                        String formattedName = a.getFormattedName().replaceAll("'", "''");
                        String formattedTitle = row.getString("title").replaceAll("'", "''");
                        session.execute(String.format(BaconSeeder.ADD_CAST_MEMBER, a.getId(), formattedName,
                                formattedTitle, row.getUUID("movie_id").toString()));
                    } catch (OperationTimedOutException e) {
                        try {
                            logger.error("error occured while seeding " + a.getDisplayName());
                        } catch (IOException io) {
                            throw new RuntimeException(io);
                        }
                        throw new RuntimeException(e);
                    }
                    foundMovieYearMatch = true;
                    break;
                }
            }
        } catch (TimeoutException e) {
            future.cancel(true);
            try {
                logger.error("Query timed out. Query: " + query);
            } catch (IOException io) {
                throw new RuntimeException(io);
            }
        } catch (InterruptedException | ExecutionException | java.util.concurrent.TimeoutException e) {
            throw new RuntimeException(e);
        }
        if (!foundMovieYearMatch) {
            try {
                logger.error("Could not find a movie year match for " + m.title);
            } catch (IOException io) {
                throw new RuntimeException(io);
            }
        }
    }

    /**
     * Convienence method to close file streams and open cluster connection
     *
     * @param file The open file stream
     * @throws IOException
     */
    private void shutdown(FileInputStream file) throws IOException {
        file.close();
        session.close();
        cluster.close();
    }
}

