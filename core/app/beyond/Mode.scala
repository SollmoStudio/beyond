/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 * Copyright (C) 2015 Company 100 Inc.
 */
package beyond

import play.api.{ Mode => PlayMode }

object Mode extends Enumeration {
  type Mode = Value
  val Dev, Test, Prod = Value

  implicit def from(mode: PlayMode.Mode): Mode = mode match {
    case PlayMode.Dev => Dev
    case PlayMode.Prod => Prod
    case PlayMode.Test => Test
  }

  implicit def to(mode: Mode): PlayMode.Mode = mode match {
    case Dev => PlayMode.Dev
    case Prod => PlayMode.Prod
    case Test => PlayMode.Test
  }
}
