import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer

import scala.io.StdIn

object BaconServer {
    def main(args: Array[String]): Unit = {
        implicit val system = ActorSystem("bacon")
        implicit val materializer = ActorMaterializer()
        implicit val executionContext = system.dispatcher
        val route =
            path("hello") {
                get {
                    complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "hello, world"))
                }
            } ~
            pathPrefix("hello") {
                path(Segment) { s: String =>
                    complete(s"hello, $s")
                }
            }
        val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)
        println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
        StdIn.readLine()
        bindingFuture
            .flatMap(_.unbind())
            .onComplete(_ => system.terminate())
    }
}
