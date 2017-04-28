import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{HttpOrigin, HttpOriginRange}
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route}
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings

trait MovieResource extends BaconResource {
    val movieService: MovieService

    def movieRoutes: Route = pathPrefix("v1") {

        val corsSettings = CorsSettings.defaultSettings.copy(
            allowedOrigins = HttpOriginRange(HttpOrigin("http://cs.oswego.edu"))
        )

        val rejectionHandler = corsRejectionHandler withFallback RejectionHandler.default

        val exceptionHandler = ExceptionHandler {
            case e: NoSuchElementException => complete(StatusCodes.NotFound -> e.getMessage)
        }

        val handleErrors = handleRejections(rejectionHandler) & handleExceptions(exceptionHandler)

        handleErrors {
            cors(corsSettings) {
                handleErrors {
                    pathPrefix("movies") {
                        path(Segment) { title =>
                            complete(movieService.findMovie(title))
                        }
                    }
                }
            }
        }
    }
}