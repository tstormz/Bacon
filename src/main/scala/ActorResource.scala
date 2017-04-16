import akka.http.scaladsl.server.Route

trait ActorResource extends BaconResource {
    val actorService: ActorService

    def actorRoutes: Route = pathPrefix("v1") {
        pathPrefix("actors") {
            path(Segment) { name =>
                complete(actorService.findActor(name))
            }
        }
    }

}
