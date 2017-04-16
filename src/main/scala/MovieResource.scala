import akka.http.scaladsl.server.Route

trait MovieResource extends BaconResource {
    val movieService: MovieService

    def movieRoutes: Route = pathPrefix("v1") {
        pathPrefix("movies") {
            path(Segment) { title =>
                complete(movieService.findMovie(title))
            }
        }
    }
}
