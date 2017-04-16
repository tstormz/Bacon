import com.datastax.driver.core.{Row, ResultSet, Session}
import collection.JavaConversions._

import scala.concurrent._
import ExecutionContext.Implicits.global

case class Actor(name: String, id: String, nickName: String, suffix: String, filmography: List[Picture])
case class Picture(id: String, title: String, year: String)

class ActorService(session: Session, executionContext: ExecutionContext) {
    val SELECT_ACTOR = "SELECT * FROM actors WHERE name = '%s'"
    val SELECT_MOVIES_BY_ACTOR = "SELECT * FROM movies_by_actors WHERE actor_id=%s"

    def findActor(name: String): Future[List[Actor]] = Future {
        val query = SELECT_ACTOR.format(name.replaceAll("'", "''"))
        var actorMatches = List[Actor]()
        val results: ResultSet = session.execute(query)
        for (row: Row <- results.all()) {
            val name = row.getString("name")
            val id = row.getUUID("id").toString
            val nickname = row.getString("nickname")
            val suffix = row.getString("suffix")
            actorMatches ::= new Actor(name, id, nickname, suffix, getFilmography(id))
        }
        actorMatches
    }

    private def getFilmography(actorId: String): List[Picture] = {
        val query = SELECT_MOVIES_BY_ACTOR.format(actorId)
        var pictureMatches = List[Picture]()
        val results: ResultSet = session.execute(query)
        for (row: Row <- results.all()) {
            val id: String = row.getUUID("movie_id").toString
            val title: String = row.getString("movie_title")
            val year: String = row.getString("movie_year")
            pictureMatches ::= new Picture(id, title, year)
        }
        pictureMatches
    }

}
