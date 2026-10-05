package dev.martianzoo.engine

import dev.martianzoo.pets.ast.InstructionGroup

/** Exception types owned by the engine module. */
public object Exceptions {
  /** Requests rollback of the current transaction without reporting a failure. */
  public class AbortTransactionException : Exception()

  /** An automatic-effect chain attempted to exceed the engine's supported nesting depth. */
  public class RunawayEffectChainException(
      public val maximumDepth: Int,
      public val effectChain: List<InstructionGroup>,
  ) :
      IllegalStateException(
          "automatic effect chain exceeded depth $maximumDepth: " + effectChain.joinToString(" -> ")
      )
}
