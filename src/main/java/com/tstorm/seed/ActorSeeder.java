package com.tstorm.seed;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

public abstract class ActorSeeder {
    /**
     * Creates an actor object which is basically a name and a list of movies
     *
     * @param line String of the form 'optional nickname' lastname, firstname more names...
     * @return A new Actor
     * @throws IOException
     */
    public abstract Actor createActor(String line);

    /**
     * Specifies how you would like to insert the actor to your database. Typically this is just a database
     * INSERT statement
     *
     * @param a The Actor to insert
     * @throws IOException
     */
    public abstract void insertActor(Actor a) throws IOException;

    /**
     * Specifies how you would like to insert the {@link Movie} for which this {@link Actor} was cast. Typically this is
     * just a database INSERT statement
     *
     * @param a The Actor who was cast
     * @param m The Movie Actor a was cast
     */
    public abstract void insertMovieByActor(Actor a, Movie m);

    /**
     * Determines if the Movie/TV Show/Video Game should be inserted
     *
     * @param line The title, which is enclosed in double quotes if it is a TV episode
     * @return true if it should NOT be inserted
     */
    public abstract boolean shouldExclude(String line);

    private final Semaphore semaphore;

    public ActorSeeder(int numberOfThreads) {
        semaphore = new Semaphore(numberOfThreads);
    }

    public void parseActors(FileInputStream file, Optional<SeedLogger> logger) throws FileNotFoundException {
        try {
            parse(new InputStreamReader(file, Charset.forName("ISO-8859-1")), logger);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void parse(InputStreamReader file, Optional<SeedLogger> logger) throws IOException {
        final BufferedReader reader = new BufferedReader(file);
        String line;
        do {
            line = reader.readLine();
            if (logger.isPresent())
                logger.get().log(line);
            ActorThread actorThread = new ActorThread(extractActorData(reader, line));
            if (semaphore.tryAcquire()) {
                // start a new thread
                actorThread.run();
            } else {
                // do it yourself
                actorThread.createAndInsert();
                afterActorInsertHook();
            }
        } while (line != null);
        if (logger.isPresent())
            logger.get().close();
    }

    private String extractActorData(BufferedReader reader, String line) throws IOException {
        String actorData = "";
        do {
            actorData += line + "\n";
            line = reader.readLine();
        } while (line != null && !line.isEmpty());
        return actorData;
    }

    public Name getName(String line) {
        return new Name(line);
    }

    /**
     * Hook called after each iteration of inserting an actor
     */
    public void afterActorInsertHook() {
    }

    private class ActorThread implements Runnable {
        private final Scanner scanner;

        public ActorThread(String actorData) {
            this.scanner = new Scanner(actorData);
        }

        @Override
        public void run() {
            try {
                createAndInsert();
            } catch (IOException e) {
                e.printStackTrace();
            }
            afterActorInsertHook();
            semaphore.release();
        }

        public void createAndInsert() throws IOException {
            String line = scanner.nextLine();
            Actor a = createActor(line);
            do {
                String moviePart = line.substring(line.indexOf("\t")).replaceAll("\t", "");
                if (!shouldExclude(moviePart))
                    a.addTitle(moviePart);
                line = scanner.hasNext() ? scanner.nextLine() : "";
            } while (!line.isEmpty());
            insertActor(a);
            for (Movie m : a.getTitles()) {
                insertMovieByActor(a, m);
            }
        }

    }

}