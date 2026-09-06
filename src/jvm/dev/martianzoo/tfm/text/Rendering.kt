package dev.martianzoo.tfm.text

import dev.martianzoo.pets.ast.PetNode

/** A total rendering result: visible text together with every unresolved subtree it contains. */
internal data class Rendering<out T>(val value: T, val unresolved: List<Unresolved> = emptyList()) {
  internal fun <R> map(transform: (T) -> R): Rendering<R> = Rendering(transform(value), unresolved)

  internal companion object {
    internal fun <T> resolved(value: T): Rendering<T> = Rendering(value)

    internal fun <T> unresolved(node: PetNode, reason: RefusalReason, fallback: T): Rendering<T> =
        Rendering(fallback, listOf(Unresolved(node, reason)))
  }
}
