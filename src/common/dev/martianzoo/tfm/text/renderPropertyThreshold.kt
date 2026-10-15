package dev.martianzoo.tfm.text

import dev.martianzoo.pets.ast.Requirement

/** Wording shared by property filters in card counts and event triggers. */
internal fun renderPropertyThreshold(
    property: ComponentDescriber.MinimumProperty.Threshold,
    counting: Requirement.Counting,
): Modifier {
  val unit = property.unit?.let { " $it" }.orEmpty()
  val value =
      when (counting) {
        is Requirement.Min -> "${counting.target}$unit or more"
        is Requirement.Max -> "${counting.target}$unit or less"
        is Requirement.Exact -> "exactly ${counting.target}$unit"
      }
  return Modifier.Relation(
      "with",
      NounPhrase("${property.noun} of $value", determiner = Determiner.INDEFINITE),
  )
}
