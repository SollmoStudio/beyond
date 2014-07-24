package beyond.engine.javascript.lib

import java.{ lang => jl }
import org.mozilla.javascript.Function
import reactivemongo.bson.BSONBoolean
import reactivemongo.bson.BSONDouble
import reactivemongo.bson.BSONInteger
import reactivemongo.bson.BSONJavaScript
import reactivemongo.bson.BSONLong
import reactivemongo.bson.BSONString
import reactivemongo.bson.BSONValue
import reactivemongo.bson.BSONWriter

package object database {
  implicit object AnyRefBSONWriter extends BSONWriter[AnyRef, BSONValue] {
    override def write(value: AnyRef): BSONValue = value match {
      case s: String => BSONString(s)
      case i: jl.Integer => BSONInteger(i)
      case j: jl.Long => BSONLong(j)
      case z: jl.Boolean => BSONBoolean(z)
      case d: jl.Double => BSONDouble(d)
      case f: Function => BSONJavaScript(f.toString)
      case _ =>
        throw new IllegalArgumentException(s"$value(${value.getClass} cannot be a BSONValue")
    }
  }
}
