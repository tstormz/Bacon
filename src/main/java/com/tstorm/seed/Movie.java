package com.tstorm.seed;

/**
 * Movie container for (title, year) pair
 */
class Movie {
    final String title;
    final String year;

    public Movie(String title, String year) {
        this.title = title;
        this.year = year;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return String.format("%s (%s)", title, year);
    }
}

