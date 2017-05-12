import akka.http.scaladsl.model.{HttpMethod, StatusCodes}
import akka.http.scaladsl.model.HttpMethods._
import scala.collection.immutable
import akka.http.scaladsl.model.headers.{HttpOrigin, HttpOriginRange}
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route}
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings

trait UserResource extends BaconResource {
    val userService: UserService
    val movieService: MovieService
    val actorService: ActorService

    def bacon: Route = {

        val corsSettings = CorsSettings.defaultSettings.copy(
            allowedOrigins = HttpOriginRange.*,
            allowCredentials = false,
            allowedMethods = immutable.Seq(GET,PUT,POST,HEAD,OPTIONS)
        )

        val rejectionHandler = corsRejectionHandler withFallback RejectionHandler.default

        val exceptionHandler = ExceptionHandler {
            case e: NoSuchElementException => complete(StatusCodes.NotFound -> e.getMessage)
        }

        val handleErrors = handleRejections(rejectionHandler) & handleExceptions(exceptionHandler)

        handleErrors {
            cors(corsSettings) {
                handleErrors {
                    pathPrefix("v1") {
                        pathPrefix("users") {
                            parameters('api_key) { (api_key) =>
                                post {
                                    entity(as[User]) { user => completeWithLocationHeader(
                                        resourceId = userService.createUser(api_key, user),
                                        ifDefinedStatus = 201, ifEmptyStatus = 409)
                                    }
                                } ~ pathPrefix(Segment) { email =>
                                    pathEnd {
                                        get {
                                            complete(userService.getUser(decode(email)))
                                        }
                                    } ~ put {
                                        entity(as[UserUpdate]) { update =>
                                            complete(userService.updateUser(decode(email), update))
                                        }
                                    } ~ delete {
                                        complete(userService.deleteUser(decode(email)))
                                    } ~ pathPrefix("movies") {
                                        post {
                                            entity(as[FavoriteMovie]) { movie => completeWithLocationHeader(
                                                resourceId = userService.addMovie(api_key, decode(email), movie),
                                                ifDefinedStatus = 201, ifEmptyStatus = 409)
                                            }
                                        } ~ pathEnd {
                                            get {
                                                complete(userService.getMovies(api_key, decode(email)))
                                            }
                                        } ~ pathPrefix("recommendations") {
                                            get {
                                                complete(userService.recommend(api_key, decode(email)))
                                            }
                                        }
                                    }
                                }
                            }
                        } ~ pathPrefix("movies") {
                            path(Segment) { title =>
                                complete(movieService.findMovie(title))
                            }
                        } ~ pathPrefix("actors") {
                            path(Segment) { name =>
                                complete(actorService.findActor(name))
                            }
                        }
                    }
                }
            }
        }
    }

    def decode(uri: String): String = {
        uri.replaceAll("%40", "@").replaceAll("%2E", ".")
    }

}
