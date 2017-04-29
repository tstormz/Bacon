import akka.http.scaladsl.server.Route
import com.datastax.driver.core.{Session, Cluster}

import scala.concurrent.ExecutionContext

trait Resources extends UserResource

trait RestInterface extends Resources {

    implicit def executionContext: ExecutionContext

    val movieDatabase: Cluster = Cluster.builder().addContactPoint("52.14.185.37").build()
    val userCluster: Cluster = Cluster.builder().addContactPoint("13.58.72.18").build()
    movieDatabase.init()
    userCluster.init()
    val imdbData: Session = movieDatabase.connect("gameplay")
    val users: Session = userCluster.connect("user")

    lazy val userService = new UserService(users, executionContext)
    lazy val movieService = new MovieService(imdbData, executionContext)
    lazy val actorService = new ActorService(imdbData, executionContext)

    val routes: Route = bacon

}
