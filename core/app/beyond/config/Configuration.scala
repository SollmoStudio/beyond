/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 * Copyright (C) 2015 Company 100 Inc.
 */
package beyond.config

import java.io.File

import com.typesafe.config.{ ConfigParseOptions, Config, ConfigFactory }
import play.api.{ Configuration => PlayConfiguration }
import scala.collection.JavaConverters._

object Configuration {
  def empty: Configuration = Configuration(ConfigFactory.empty())

  def apply(config: Config): Configuration =
    new Configuration(config)

  def apply(configuration: PlayConfiguration): Configuration =
    apply(configuration.underlying)

  def from(data: Map[String, Any]): Configuration = {
    def asJavaRecursively[A](data: Map[A, Any]): Map[A, Any] = {
      data.mapValues {
        case v: Map[_, _] => asJavaRecursively(v).asJava
        case v: Iterable[_] => v.asJava
        case v => v
      }
    }

    Configuration(ConfigFactory.parseMap(asJavaRecursively[String](data).asJava))
  }

  private[this] lazy val dontAllowMissingConfigOptions = ConfigParseOptions.defaults().setAllowMissing(false)
  private[this] lazy val dontAllowMissingConfig = ConfigFactory.load(dontAllowMissingConfigOptions)

  def load(appPath: File): Configuration =
    Configuration(dontAllowMissingConfig)
}

class Configuration(underlying: Config) {
  private def readValue[T](path: String, v: => T): Option[T] = {
    try {
      Option(v)
    } catch {
      case _: Throwable => None
    }
  }

  def getLong(path: String): Option[Long] = readValue(path, underlying.getLong(path))

  def getInt(path: String): Option[Int] = readValue(path, underlying.getInt(path))

  def getBoolean(path: String): Option[Boolean] = readValue(path, underlying.getBoolean(path))

  def getString(path: String, validValues: Option[Set[String]] = None): Option[String] = readValue(path, underlying.getString(path)).flatMap { value =>
    validValues match {
      case Some(values) if values.contains(value) => Some(value)
      case Some(values) if values.isEmpty => Some(value)
      case Some(values) => None
      case None => Some(value)
    }
  }

  def getStringList(path: String): Option[java.util.List[java.lang.String]] =
    readValue(path, underlying.getStringList(path))
  def getStringSeq(path: String): Option[Seq[java.lang.String]] =
    getStringList(path).map(_.asScala.toSeq)

  def getConfig(path: String): Option[Configuration] =
    readValue(path, underlying.getConfig(path)).map(Configuration(_))

  def subKeys: Set[String] = underlying.root().keySet().asScala.toSet
  def keys: Set[String] = underlying.entrySet.asScala.map(_.getKey).toSet
}
