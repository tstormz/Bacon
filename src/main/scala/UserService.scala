import java.util.UUID

import com.datastax.driver.core.{ResultSet, Row, Session}

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

case class User(email: String, name: String, password: String)
case class UserUpdate(name: Option[String], password: Option[String])
case class FavoriteMovie(id: String, title: String, displayName: String, userRating: Int)
case class RecommendedMovie(id: String, title: String, recommendation: Double)
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

    def createUser(api_key: String, user: User): Future[Option[User]] = Future {
        val cleanEmail = user.email.replaceAll("'", "''")
        val cleanName = user.name.replaceAll("'", "''")
        users.execute(INSERT_USER.format(api_key, cleanEmail, cleanName, user.password))
        Some(user)
    }

    def getUser(username: String): Future[Option[User]] = Future {
        val results: ResultSet = users.execute(FIND_USER.format(API_KEY, username.replaceAll("'", "''")))
        val row: Row = results.one(); // there should only be one because of the clustering column
        Option(User(row.getString("email"), row.getString("name"), row.getString("password")))
    }

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

    def deleteUser(username: String): Future[Unit] = Future {
        users.execute(DELETE.format(API_KEY, username.replaceAll("'", "''")))
    }

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

    def getMovies(api_key: String, username: String): Future[List[FavoriteMovie]] = Future {
        var movies = List[FavoriteMovie]()
        val results: ResultSet = users.execute(GET_MOVIES.format(API_KEY, username))
        for (row: Row <- results.all()) {
            movies ::= FavoriteMovie(row.getUUID("movie_id").toString, row.getString("movie_title"),
                row.getString("display_name"), row.getShort("user_rating"))
        }
        movies
    }

    def recommend(api_key: String, username: String): Future[List[RecommendedMovie]] = Future {
        findRecommendations(username).sortWith(_.recommendation > _.recommendation).take(5)
    }

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

    def getMovies(user: String): mutable.Map[UUID, Boolean] = {
        val GET_FAVORITES = "SELECT movie_id,user_rating FROM movies_by_user WHERE api_key='%s' AND email='%s'"
        var userMovies = mutable.Map[UUID, Boolean]()
        for (movie: Row <- users.execute(GET_FAVORITES.format(API_KEY, user)).all()) {
            userMovies += (movie.getUUID("movie_id") -> (movie.getShort("user_rating") == 1))
        }
        userMovies
    }

    def compareMovies(user: mutable.Map[UUID, Boolean], otherUser: mutable.Map[UUID, Boolean]): List[UUID] = {
        var commonMovies = List[UUID]()
        for (movie_id: UUID <- user.keys) {
            if (otherUser contains movie_id) {
                commonMovies ::= movie_id
            }
        }
        commonMovies
    }

    def compareCommonMovies(user: mutable.Map[UUID, Boolean], otherUser: mutable.Map[UUID, Boolean], commonMovies: List[UUID], like: Boolean): List[UUID] = {
        var common = List[UUID]()
        for (movie_id: UUID <- commonMovies) {
            if ((user get movie_id).get == like && (otherUser get movie_id).get == like) {
                common ::= movie_id
            }
        }
        common
    }

    def findRecommendations(username: String): List[RecommendedMovie] = {
        var recommendations = List[RecommendedMovie]()
        for (movie: SuggestedMovie <- findUnwatchedMovies(username)) {
            recommendations ::= RecommendedMovie(movie.id.toString, movie.title, recommendation(username, movie))
        }
        recommendations
    }

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