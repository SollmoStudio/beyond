package beyond

class NullOps[T](nullable: T) {
  private def option = Option(nullable)

  def isNull: Boolean = option.isEmpty
  def notNull: Boolean = option.isDefined
}

object NullOps {
  implicit def asNullOps[T](nullable: T): NullOps[T] = new NullOps[T](nullable)
}
