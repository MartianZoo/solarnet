package dev.martianzoo.pets.ast

/**
 * A "major" kind of Pets node, like an [Instruction], but not an ancillary type like
 * [FromExpression], [ScaledExpression], or [ClassName].
 */
public sealed class PetElement : PetNode() {
  private var linkedTypeSourcesIn: Set<Expression> = emptySet()

  internal val recordedLinkedTypeSources: Set<Expression>
    get() = linkedTypeSourcesIn

  internal fun recordLinkedTypeSources(sources: Set<Expression>) {
    linkedTypeSourcesIn = sources
  }
}

internal fun <P : PetElement> P.withLinkedTypeSources(sources: Set<Expression>): P = apply {
  recordLinkedTypeSources(sources)
}
