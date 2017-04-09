//import akka.actor.{Actor, Props}
//
//case class SayHello(greeting: String)
//case class CreateUser(username: String)
//
//class User(val username: String, val password: String)
//object MyJsonProtocol extends DefaultJsonProtocol {
//    implicit object UserJsonFormat extends RootJsonFormat[User] {
//        def write(u: User) = JsArray(JsString(u.username), JsString(u.password))
//
//        def read(value: JsValue) = value match {
//            case JsArray(Vector(JsString(username), JsString(password))) =>
//                new User(username, password)
//            case _ => deserializationError("User expected")
//        }
//    }
//}
//
//class UserActor extends Actor {
//    val javaActor = context.actorOf(Props[ActorTest], "javaActor")
//
//    override def receive = {
//        case SayHello(greeting) => javaActor.tell(greeting + " Java Actor", sender)
//        case CreateUser(userinfo) =>
//   }
//}
