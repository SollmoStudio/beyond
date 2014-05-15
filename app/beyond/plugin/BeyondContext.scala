package beyond.plugin

import akka.actor.ActorRef
import org.mozilla.javascript.Context

class BeyondContext(factory: BeyondContextFactory, val actor: ActorRef) extends Context(factory)

