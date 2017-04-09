import java.text.SimpleDateFormat
import java.sql.Timestamp

import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import org.json4s.ext.JodaTimeSerializers
import org.json4s.{CustomSerializer, Formats, native, DefaultFormats}
import org.json4s.JsonAST.{JInt, JNull}


trait JsonSupport extends Json4sSupport{
    implicit val serialization = native.Serialization

    implicit def json4sFormats: Formats = customDateFormat ++ JodaTimeSerializers.all ++ CustomSerializers.all

    val customDateFormat = new DefaultFormats {
        override def dateFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
    }
}

object CustomSerializers {
    val all = List(CustomTimestampSerializer)
}

case object CustomTimestampSerializer extends CustomSerializer[Timestamp](format =>
    ({
        case JInt(x) => new Timestamp(x.longValue * 1000)
        case JNull => null
    },
        {
            case date: Timestamp => JInt(date.getTime / 1000)
        }))