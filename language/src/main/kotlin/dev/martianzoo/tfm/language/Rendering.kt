package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.PetNode

/** Why the current renderer left a Pets node unresolved. */
internal enum class RefusalReason {
  LEGACY_ACTION_RENDERER_DECLINED,
  LEGACY_EFFECT_RENDERER_DECLINED,
  LEGACY_INSTRUCTION_RENDERER_DECLINED,
  LEGACY_REQUIREMENT_RENDERER_DECLINED,
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
