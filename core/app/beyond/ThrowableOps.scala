package beyond

import beyond.NullOps.asNullOps

class ThrowableOps(t: Throwable) {
  def getRootCause: Throwable = {
    var rootCause = t
    while (rootCause.getCause.notNull) {
      rootCause = rootCause.getCause
    }
    rootCause
  }
}

object ThrowableOps {
  implicit def from(t: Throwable): ThrowableOps = new ThrowableOps(t)
}

