/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 * Copyright (C) 2015 Company 100 Inc.
 */
package beyond

import play.api.Mode.{ Mode => PlayMode }

object Mode extends Enumeration {
  type Mode = Value
  val Dev, Test, Prod = Value

  implicit def from(mode: PlayMode): Mode = mode match {
    case play.api.Mode.Dev => Dev
    case play.api.Mode.Prod => Prod
    case play.api.Mode.Test => Test
  }
}
