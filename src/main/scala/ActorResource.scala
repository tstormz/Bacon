import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{HttpOrigin, HttpOriginRange}
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route}
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings

trait ActorResource extends BaconResource {
    val actorService: ActorService

    def actorRoutes: Route = pathPrefix("v1") {

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
                    pathPrefix("actors") {
                        path(Segment) { name =>
                            complete(actorService.findActor(name))
                        }
                    }
                }
            }
        }
    }

}
