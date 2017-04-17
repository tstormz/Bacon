import akka.http.scaladsl.model.headers.HttpOriginRange
import akka.http.scaladsl.server.Route
import ch.megard.akka.http.cors.{CorsDirectives, CorsSettings}
import ch.megard.akka.http.cors.CorsDirectives._

trait MovieResource extends BaconResource {
    val movieService: MovieService

    var settings = CorsSettings.defaultSettings.copy(allowGenericHttpRequests = true, allowCredentials = false, allowedOrigins = HttpOriginRange.*)

    def movieRoutes: Route = pathPrefix("v1") {
        handleRejections(CorsDirectives.corsRejectionHandler) {
            cors(settings) {
                pathPrefix("movies") {
                    path(Segment) { title =>
                        complete(movieService.findMovie(title))
                    }
                }
            }
        }
    }
}
