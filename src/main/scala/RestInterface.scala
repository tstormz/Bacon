import akka.http.scaladsl.server.Route
import com.datastax.driver.core.{Session, Cluster}

import scala.concurrent.ExecutionContext

trait Resources extends UserResource
    with MovieResource
    with ActorResource

trait RestInterface extends Resources {

    implicit def executionContext: ExecutionContext

    val cluster: Cluster = Cluster.builder().addContactPoint("52.14.185.37").build()
    cluster.init()
    val session: Session = cluster.connect("gameplay")

    lazy val userService = new UserService
    lazy val movieService = new MovieService(session, executionContext)
    lazy val actorService = new ActorService(session, executionContext)

    val routes: Route = userRoutes ~ movieRoutes ~ actorRoutes

}
