import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer

import scala.io.StdIn

object BaconServer extends App with RestInterface {
    val host: String = "localhost"
    val port: Int = 8080

    implicit val system = ActorSystem("bacon")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    val api = routes

    val bindingFuture = Http().bindAndHandle(api, host, port)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine()

    bindingFuture
        .flatMap(_.unbind())
        .onComplete(_ => system.terminate())
}