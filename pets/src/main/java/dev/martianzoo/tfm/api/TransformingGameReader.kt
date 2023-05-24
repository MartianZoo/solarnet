package dev.martianzoo.tfm.api

import dev.martianzoo.tfm.pets.PetTransformer
import dev.martianzoo.tfm.pets.ast.Metric
import dev.martianzoo.tfm.pets.ast.PetElement
import dev.martianzoo.tfm.pets.ast.Requirement

public class TransformingGameReader(val delegate: GameReader, val xer: PetTransformer) :
    GameReader by delegate {
  override fun evaluate(requirement: Requirement) = delegate.evaluate(xer.transform(requirement))

  override fun count(metric: Metric) = delegate.count(xer.transform(metric))

  override fun <P : PetElement> preprocess(node: P) = delegate.preprocess(xer.transform(node))
}
