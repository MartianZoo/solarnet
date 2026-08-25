package dev.martianzoo.tfm.text

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Property
import dev.martianzoo.pets.ast.Requirement

/** The printed card fact used by one canonical card operation. */
internal sealed interface CardCriterion {
  data class Tag(val className: ClassName) : CardCriterion

  data object NoTags : CardCriterion

  data object HasRequirement : CardCriterion

  data class ResourceIcon(val className: ClassName) : CardCriterion
}

internal fun Describers.cardCriterion(requirement: Requirement): CardCriterion? {
  val counting = requirement as? Requirement.Counting ?: return null
  if (counting is Requirement.Min && counting.target == 1) {
    val metric = counting.metric
    if (metric is Metric.Count && metric.expression.simple) {
      tagName(metric.expression.className)?.let {
        return CardCriterion.Tag(metric.expression.className)
      }
    }
    if (
        metric is Property && metric.receiver == null && metric.propertyName.value == "requirement"
    ) {
      return CardCriterion.HasRequirement
    }
    if (metric is Metric.Count) {
      val citations = metric.expression
      if (
          citations.className == CITATIONS &&
              citations.arguments.size == 1 &&
              citations.refinement == null &&
              !citations.complement
      ) {
        val representedClass = citations.arguments.single()
        if (representedClass.className != CLASS || representedClass.arguments.size != 1) return null
        val resource = representedClass.arguments.single()
        if (!resource.simple || cardResourceNoun(resource.className, 1) == null) return null
        return CardCriterion.ResourceIcon(resource.className)
      }
    }
  }
  if (
      counting is Requirement.Max &&
          counting.target == 0 &&
          (counting.metric as? Metric.Count)?.expression?.let {
            it.simple && it.className == TAG
          } == true
  ) {
    return CardCriterion.NoTags
  }
  return null
}

private val CITATIONS = cn("Citations")
private val CLASS = cn("Class")
private val TAG = cn("Tag")
