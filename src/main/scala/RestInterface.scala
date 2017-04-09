import akka.http.scaladsl.server.Route

import scala.concurrent.ExecutionContext

trait Resources extends UserResource

trait RestInterface extends Resources {

    implicit def executionContext: ExecutionContext
    lazy val userService = new UserService
    val routes: Route = userRoutes

}
