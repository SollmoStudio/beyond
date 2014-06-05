package beyond

import play.modules.reactivemongo.ReactiveMongoPlugin

trait Mongo {
  import play.api.Play.current

  /** Returns the current instance of the driver. */
  def driver = ReactiveMongoPlugin.driver
  /** Returns the current MongoConnection instance (the connection pool manager). */
  def connection = ReactiveMongoPlugin.connection
  /** Returns the default database (as specified in `application.conf`). */
  def db = ReactiveMongoPlugin.db
}

