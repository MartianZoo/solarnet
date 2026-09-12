package dev.martianzoo.tfm.tests

import dev.martianzoo.agent.Agent.OperationScope
import dev.martianzoo.generated.Player
import dev.martianzoo.tfm.engine.TfmGameplay

/** An operation body whose card occurrences belong to [P]. */
internal interface TypedOperationBody<P : Player> : OperationScope {
  val gameplay: TfmGameplay<P>
}
