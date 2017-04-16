import akka.http.scaladsl.server.Route

trait UserResource extends BaconResource {
    val userService: UserService

    def userRoutes: Route = pathPrefix("v1") {
        pathPrefix("users") {
            parameters('api_key) { (api_key) =>
                post {
                    entity(as[User]) { user => completeWithLocationHeader(
                        resourceId = userService.createUser(api_key, user),
                        ifDefinedStatus = 201, ifEmptyStatus = 409)
                    }
                } ~
                pathPrefix(Segment) { email =>
                    pathEnd {
                        get {
                            complete(userService.getUser(decode(email)))
                        }
                    } ~
                    put {
                        entity(as[UserUpdate]) { update =>
                            complete(userService.updateUser(decode(email), update))
                        }
                    } ~
                    delete {
                        complete(userService.deleteUser(decode(email)))
                    } ~
                    pathPrefix("movies") {
                        post {
                            entity(as[FavoriteMovie]) { movie => completeWithLocationHeader(
                                resourceId = userService.addMovie(api_key, decode(email), movie),
                                ifDefinedStatus = 201, ifEmptyStatus = 409)
                            }
                        } ~
                        get {
                             complete(userService.getMovies(api_key, decode(email)))
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
