import java.util.UUID

import com.datastax.driver.core.{ResultSet, Row, Session}

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

/**
  * Representation of the User, used to create new users
  *
  * @param email The user's email (which is also the username)
  * @param name The user's name
  * @param password The user's password
  */
case class User(email: String, name: String, password: String)

/**
  * Possible properties of the user that can be updated
  *
  * @param name The new name
  * @param password The new password
  */
case class UserUpdate(name: Option[String], password: Option[String])

/**
  * Representation of a Movie that the user has used during the game
  *
  * @param id The movie's ID in the database
  * @param title The title of the movie
  * @param displayName The title of the movie in a displayable format
  * @param userRating Like or Dislike (1 and 0 respectively)
  */
case class FavoriteMovie(id: String, title: String, displayName: String, userRating: Int)

/**
  * A potential movie recommendation (used to indicate the movie hasn't been seen by
  * a particular user)
  *
  * @param id The movie's ID in the database
  * @param title The title of the movie
  * @param recommendation The recommendation score
  */
case class RecommendedMovie(id: String, title: String, recommendation: Double)

/**
  * Represenation of a suggested movie (basically a {@link RecommendedMovie} with a
  * high recommendation score
  *
  * @param id The movie's ID in the database
  * @param title The title of the movie
  */
case class SuggestedMovie(id: UUID, title: String)

class UserService(users: Session, executionContext: ExecutionContext) {
    val INSERT_USER = "INSERT INTO users (api_key,email,name,password) VALUES ('%s','%s','%s','%s')"
    val FIND_USER = "SELECT * from users where api_key='%s' AND email='%s'"
    val UPDATE = "UPDATE users SET %s='%s' WHERE api_key='%s' AND email='%s'"
    val DELETE = "DELETE FROM users WHERE api_key='%s' AND email='%s'"
    val ADD_MOVIE = "INSERT INTO movies_by_user (api_key,email,movie_id,movie_title,display_name, user_rating) VALUES " +
        "('%s','%s',%s,'%s','%s', %d)"
    val GET_MOVIES = "SELECT * FROM movies_by_user WHERE api_key='%s' AND email='%s'"

    // TODO remove this constant
    val API_KEY = "0"

    /**
      * Given a proper User representation, inserts the user into the database in the
      * users table (user keyspace)
      *
      * @param api_key The developers API key
      * @param user Represenatation of the user
      * @return A Future containing the new User
      */
    def createUser(api_key: String, user: User): Future[Option[User]] = Future {
        val cleanEmail = user.email.replaceAll("'", "''")
        val cleanName = user.name.replaceAll("'", "''")
        users.execute(INSERT_USER.format(api_key, cleanEmail, cleanName, user.password))
        Some(user)
    }

    /**
      * Locates a user in the database given a proper username
      *
      * @param username The username (email) of the user to find
      * @return A Future containing the representation of the found User
      */
    def getUser(username: String): Future[Option[User]] = Future {
        val results: ResultSet = users.execute(FIND_USER.format(API_KEY, username.replaceAll("'", "''")))
        val row: Row = results.one(); // there should only be one because of the clustering column
        Option(User(row.getString("email"), row.getString("name"), row.getString("password")))
    }

    /**
      * Updates a User's information in the database
      *
      * @param username The User's username (email)
      * @param update A UserUpdate wrapper containing the new updated fields
      * @return The UserUpdate
      */
    def updateUser(username: String, update: UserUpdate): Future[Option[UserUpdate]] = {
        getUser(username).flatMap {
            case None => Future {
                None
            }
            case Some(user) => Future {
                if (update.name.isDefined) {
                    val newName = update.name.get.replaceAll("'", "''")
                    users.execute(UPDATE.format("name", newName, API_KEY, username.replaceAll("'", "''")))
                }
                if (update.password.isDefined) {
                    val newPassword = update.password.get.replaceAll("'", "''")
                    users.execute(UPDATE.format("name", newPassword, API_KEY, username.replaceAll("'", "''")))
                }
                Some(update)
            }
        }
    }

    /**
      * Deletes a user from the database
      *
      * @param username The username (email) of the user to delete
      * @return
      */
    def deleteUser(username: String): Future[Unit] = Future {
        users.execute(DELETE.format(API_KEY, username.replaceAll("'", "''")))
    }

    /**
      * Add a movie to the user's favorite movies list
      *
      * @param api_key The developer's API key
      * @param username The username of the user to add the movie
      * @param movie The movie to add to the user
      * @return
      */
    def addMovie(api_key: String, username: String, movie: FavoriteMovie): Future[Option[FavoriteMovie]] = {
        getUser(username).flatMap {
            case Some(u) => Future {
                val cleanUsername = username.replaceAll("'", "''")
                val title = movie.title.replaceAll("'", "''")
                val displayName = movie.displayName.replaceAll("'", "''")
                users.execute(ADD_MOVIE.format(api_key, cleanUsername, movie.id.toString, title, displayName, movie.userRating))
                Some(movie)
            }
            case None => Future {
                None
            }
        }
    }

    /**
      * Get all movies in a given user's favorites list
      *
      * @param api_key The developer's API key
      * @param username The username (email) of the user
      * @return A Future containing a list of all the user's movies
      */
    def getMovies(api_key: String, username: String): Future[List[FavoriteMovie]] = Future {
        var movies = List[FavoriteMovie]()
        val results: ResultSet = users.execute(GET_MOVIES.format(API_KEY, username))
        for (row: Row <- results.all()) {
            movies ::= FavoriteMovie(row.getUUID("movie_id").toString, row.getString("movie_title"),
                row.getString("display_name"), row.getShort("user_rating"))
        }
        movies
    }

    /**
      * Provides recommendations for a given user using their list of favorite movies
      *
      * @param api_key The developer's API key
      * @param username The username (email) of the user
      * @return A Future containing a list of recommended movies
      */
    def recommend(api_key: String, username: String): Future[List[RecommendedMovie]] = Future {
        findRecommendations(username).sortWith(_.recommendation > _.recommendation).take(5)
    }

    /**
      * Determines the similarity between two users given their likes and dislikes of
      * each movie in the list of favorite movies
      *
      * @param user The username (email) of the first user
      * @param otherUser The username (email) of the second user
      * @return A value between -1.0 and 1.0 where -1.0 is completely dissimilar and
      *         1.0 is completely similar
      */
    def similarity(user: String, otherUser: String): Double = {
        val userMovies = getMovies(user)
        val otherUserMovies = getMovies(otherUser)
        val commonMovies = compareMovies(userMovies, otherUserMovies)
        val commonLikes = compareCommonMovies(userMovies, otherUserMovies, commonMovies, like = true)
        val commonDislikes = compareCommonMovies(userMovies, otherUserMovies, commonMovies, like = false)
        val commonLikesOrDislikes = commonLikes.size + commonDislikes.size
        val numerator: Double = commonLikesOrDislikes - (2 * (commonMovies.size - commonLikesOrDislikes))
        numerator / (userMovies.size + otherUserMovies.size)
    }

    /**
      * Convenience method to get the movies of a particular user as a map of movie_id -> like/dislike
      *
      * @param user The username (email) to get all movies
      * @return A map of all movies in a user's favorites list, along with their like/dislike value
      */
    def getMovies(user: String): mutable.Map[UUID, Boolean] = {
        val GET_FAVORITES = "SELECT movie_id,user_rating FROM movies_by_user WHERE api_key='%s' AND email='%s'"
        var userMovies = mutable.Map[UUID, Boolean]()
        for (movie: Row <- users.execute(GET_FAVORITES.format(API_KEY, user)).all()) {
            userMovies += (movie.getUUID("movie_id") -> (movie.getShort("user_rating") == 1))
        }
        userMovies
    }

    /**
      * Compares two users based on common interest in movies
      *
      * @param user The username (email) of the first user
      * @param otherUser The username (email) of the second user
      * @return A list of all the movies both users had in their favorites list
      */
    def compareMovies(user: mutable.Map[UUID, Boolean], otherUser: mutable.Map[UUID, Boolean]): List[UUID] = {
        var commonMovies = List[UUID]()
        for (movie_id: UUID <- user.keys) {
            if (otherUser contains movie_id) {
                commonMovies ::= movie_id
            }
        }
        commonMovies
    }

    /**
      * Convenience method to filter only the movies two users liked (or disliked) from their list of common movies
      *
      * @param user The username (email) of the first user
      * @param otherUser The username (email) of the second user
      * @param commonMovies The movies they both have in common
      * @param like true to filter by likes, false to filter by dislikes
      * @return
      */
    def compareCommonMovies(user: mutable.Map[UUID, Boolean], otherUser: mutable.Map[UUID, Boolean],
                            commonMovies: List[UUID], like: Boolean): List[UUID] = {
        var common = List[UUID]()
        for (movie_id: UUID <- commonMovies) {
            if ((user get movie_id).get == like && (otherUser get movie_id).get == like) {
                common ::= movie_id
            }
        }
        common
    }

    /**
      * Narrows suggested movies down to recommendations
      *
      * @param username The username (email) of the user to supply recommendations for
      * @return A list of recommended movies for the user
      */
    def findRecommendations(username: String): List[RecommendedMovie] = {
        var recommendations = List[RecommendedMovie]()
        for (movie: SuggestedMovie <- findUnwatchedMovies(username)) {
            recommendations ::= RecommendedMovie(movie.id.toString, movie.title, recommendation(username, movie))
        }
        recommendations
    }

    /**
      * Convienence method to find movies that other users have seen by this user hasn't
      *
      * @param username The username (email) of the user to find unwatched movies
      * @return A list of suggestions (basically movies other users had in their favorites but were
      *         not in the user's favorites
      */
    def findUnwatchedMovies(username: String): List[SuggestedMovie] = {
        val GET_MOVIES = "SELECT movie_id,movie_title FROM movies_by_user WHERE api_key='%s' AND email='%s'"
        var movies = List[SuggestedMovie]()
        for (user: Row <- users.execute("SELECT * FROM users").all()) {
            if (user.getString("email") != username) {
                val theirMovies: ResultSet = users.execute(GET_MOVIES.format(API_KEY, user.getString("email")))
                val myMovies: mutable.Map[UUID, Boolean] = getMovies(username)
                for (movie: Row <- theirMovies.all()) {
                    if (!(myMovies contains movie.getUUID("movie_id"))) {
                        val suggestion = SuggestedMovie(movie.getUUID("movie_id"), movie.getString("movie_title"))
                        if (!movies.contains(suggestion)) {
                            movies ::= suggestion
                        }
                    }
                }
            }
        }
        movies
    }

    /**
      * Algorithm to give a recommendation score to the user for a particular movie
      *
      * @param username The username (email) of the user to give a recommendation
      * @param movie The movie to score
      * @return A value between -1.0 (a horrible recommendation) and 1.0 (a perfect recommendation)
      */
    def recommendation(username: String, movie: SuggestedMovie): Double = {
        var z: Double = 0.0
        var m: Int = 0
        for (user: Row <- users.execute("SELECT * FROM users").all()) {
            val them = user.getString("email")
            val theirMovies = getMovies(them)
            if (them != username && (theirMovies contains movie.id)) {
                val x = similarity(username, them)
                if ((theirMovies get movie.id).get) {
                    z += x
                } else {
                    z -= x
                }
                m += 1
            }
        }
        z / m
    }
}