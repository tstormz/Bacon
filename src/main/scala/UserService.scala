import java.util.UUID

import com.datastax.driver.core.{ResultSet, Row, Session}

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

case class User(email: String, name: String, password: String)
case class UserUpdate(name: Option[String], password: Option[String])
case class FavoriteMovie(id: String, title: String, displayName: String)

class UserService(users: Session, executionContext: ExecutionContext) {
    val INSERT_USER = "INSERT INTO users (api_key,email,name,password) VALUES ('%s','%s','%s','%s')"
    val FIND_USER = "SELECT * from users where api_key='%s' AND email='%s'"
    val UPDATE = "UPDATE users SET %s='%s' WHERE api_key='%s' AND email='%s'"
    val DELETE = "DELETE FROM users WHERE api_key='%s' AND email='%s'"
    val ADD_MOVIE = "INSERT INTO movies_by_user (api_key,email,movie_id,movie_title,display_name) VALUES " +
        "('%s','%s',%s,'%s','%s')"
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
                users.execute(ADD_MOVIE.format(api_key, cleanUsername, movie.id.toString, title, displayName))
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
            movies ::= FavoriteMovie(row.getUUID("movie_id").toString, row.getString("movie_title"), row.getString("display_name"))
        }
        movies
    }
}