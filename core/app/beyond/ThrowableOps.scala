package beyond

class ThrowableOps(t: Throwable) {
  def getRootCause: Throwable = {
    var rootCause = t
    while (rootCause.getCause != null) {
      rootCause = rootCause.getCause
    }
    rootCause
  }
}

object ThrowableOps {
  implicit def from(t: Throwable): ThrowableOps = new ThrowableOps(t)
}

