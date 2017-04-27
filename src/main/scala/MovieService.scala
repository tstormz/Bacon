import java.util.UUID

import com.datastax.driver.core.{Row, ResultSet, Session}
import com.tstorm.seed.BaconSeeder
import collection.JavaConversions._

import scala.concurrent._
import ExecutionContext.Implicits.global

case class Movie(title: String, year: String, cast: List[CastMember])
case class CastMember(id: String, name: String)

class MovieService(val movies: Session, val executionContext: ExecutionContext) {

    def findMovie(title: String): Future[List[Movie]] = Future {
        val query = String.format(BaconSeeder.SELECT_MOVIE, title.replaceAll("'", "''"))
        var movieMatches = List[Movie]()
        val results: ResultSet = movies.execute(query)
        for (row: Row <- results.all()) {
            val cast: java.util.Map[UUID, String] = row.getMap("cast", classOf[UUID], classOf[String])
            var castMembers: List[CastMember] = List[CastMember]()
            for (id <- cast.keySet()) {
                castMembers ::= new CastMember(id.toString, cast.get(id))
            }
            movieMatches ::= new Movie(row.getString("title"), row.getString("year"), castMembers)
        }
        movieMatches
    }

}