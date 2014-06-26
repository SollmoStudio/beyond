package beyond

import play.modules.reactivemongo.ReactiveMongoPlugin
import reactivemongo.api.DB
import reactivemongo.api.MongoConnection
import reactivemongo.api.MongoDriver

trait MongoMixin {
  import play.api.Play.current

  /** Returns the current instance of the driver. */
  def driver: MongoDriver = ReactiveMongoPlugin.driver
  /** Returns the current MongoConnection instance (the connection pool manager). */
  def connection: MongoConnection = ReactiveMongoPlugin.connection
  /** Returns the default database (as specified in `application.conf`). */
  def db: DB = ReactiveMongoPlugin.db
}

