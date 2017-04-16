import scala.concurrent.{Future, ExecutionContext}
import Array._

class UserService(implicit val executionContext: ExecutionContext) {

    var users = Vector.empty[User]
    val movies: java.util.Map[String, Array[FavoriteMovie]] = new java.util.HashMap

    def createUser(api_key: String, user: User): Future[Option[User]] = Future {
        users.find(_.email == user.email) match {
            case Some(u) => None // already exists
            case None =>
                users = users :+ user
                movies.put(user.email, Array(FavoriteMovie("nightmare_on_elm_street", 1984)))
                Some(user)
        }
    }

    def getUser(username: String): Future[Option[User]] = Future {
        users.find(_.email == username)
    }

    def updateUser(username: String, update: UserUpdate): Future[Option[User]] = {

        def updateEntity(user: User): User = {
            val name = update.name.getOrElse(user.name)
            val password = update.password.getOrElse(user.password)
            User(username, name, password)
        }

        getUser(username).flatMap {
            case None => Future {
                None
            }
            case Some(user) =>
                val updatedUser = updateEntity(user)
                deleteUser(username).flatMap { _ =>
                    createUser("1", updatedUser).map(_ => Some(updatedUser))
                }
        }

    }

    def deleteUser(username: String): Future[Unit] = Future {
        users = users.filterNot(_.email == username)
    }

    def addMovie(api_key: String, username: String, movie: FavoriteMovie): Future[Option[FavoriteMovie]] = Future {
        users.find(_.email == username) match {
            case Some(u) =>
                val userMovies = movies.get(username)
                val updatedUserMovies = concat(userMovies, Array(movie))
                movies.put(username, updatedUserMovies)
                for (m <- updatedUserMovies)
                    println(m.title)
                Some(movie)
            case None => None
        }
    }

    def getMovies(api_key: String, username: String): Future[Array[FavoriteMovie]] = Future {
        movies.get(users.find(_.email == username).get.email)
    }

}

case class User(email: String, name: String, password: String)
case class UserUpdate(name: Option[String], password: Option[String])
case class FavoriteMovie(title: String, year: Int)