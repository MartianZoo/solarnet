package dev.martianzoo.api

import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.PetElement
import dev.martianzoo.pets.ast.Requirement

public class TransformingGameReader(val delegate: GameReader, val xer: PetTransformer) :
    GameReader by delegate {
  override fun has(requirement: Requirement) = delegate.has(xer.transform(requirement))

  override fun count(metric: Metric) = delegate.count(xer.transform(metric))

  override fun <P : PetElement> preprocess(node: P) = delegate.preprocess(xer.transform(node))
}
