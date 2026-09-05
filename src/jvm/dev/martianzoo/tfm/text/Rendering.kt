package dev.martianzoo.tfm.text

import dev.martianzoo.pets.ast.PetNode

/** Why the current renderer left a Pets node unresolved. */
internal enum class RefusalReason {
  ACTION_ALTERNATIVES_NOT_COMBINABLE,
  REFINED_CHANGE_EXPRESSION,
  UNSUPPORTED_CARD_RESOURCE_CHANGE,
  UNSUPPORTED_DECLARED_CHANGE,
  UNSUPPORTED_DISCARD,
  UNSUPPORTED_DRAW,
  UNSUPPORTED_PLACEMENT_CHANGE,
  UNSUPPORTED_PRODUCTION_CHANGE,
  UNSUPPORTED_STANDARD_RESOURCE_CHANGE,
  UNSUPPORTED_TRACK_CHANGE,
  UNSUPPORTED_CHANGE_QUANTITY,
  UNKNOWN_ACTION_FRAME,
  UNKNOWN_CHANGE_FRAME,
  UNKNOWN_REQUIREMENT_FRAME,
  UNSUPPORTED_ACTION_CONDITION,
  UNSUPPORTED_ACTION_COST,
  UNSUPPORTED_ALTERNATIVES,
  UNSUPPORTED_END_EFFECT,
  UNSUPPORTED_FANOUT,
  UNSUPPORTED_EFFECT_TRIGGER,
  UNSUPPORTED_GATE,
  UNSUPPORTED_INSTRUCTION_KIND,
  UNSUPPORTED_SCALING,
  UNSUPPORTED_SEQUENCE,
}

/** A Pets node retained as source because the current renderer declined to interpret it. */
internal data class Unresolved(val node: PetNode, val reason: RefusalReason)

/** A total rendering result: visible text together with every unresolved subtree it contains. */
internal data class Rendering<out T>(val value: T, val unresolved: List<Unresolved> = emptyList()) {
  internal fun <R> map(transform: (T) -> R): Rendering<R> = Rendering(transform(value), unresolved)

  internal companion object {
    internal fun <T> resolved(value: T): Rendering<T> = Rendering(value)

    internal fun <T> unresolved(node: PetNode, reason: RefusalReason, fallback: T): Rendering<T> =
        Rendering(fallback, listOf(Unresolved(node, reason)))
  }
}
