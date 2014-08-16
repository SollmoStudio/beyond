package com.beyondframework.rhino

import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable

object ContextOps {
  implicit def from(cx: Context): ContextOps = new ContextOps(cx)
}

class ContextOps(context: Context) {
  def newObject(scope: Scriptable, constructorName: String, args: AnyRef*): Scriptable =
    context.newObject(scope, constructorName, args.toArray)

  def newArray(scope: Scriptable, elements: AnyRef*): Scriptable =
    context.newArray(scope, elements.toArray)
}
