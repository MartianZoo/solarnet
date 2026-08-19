package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.tfm.data.TfmClasses.PROD

internal fun renderRequirement(requirement: Requirement): String? =
    when (requirement) {
      is Requirement.Min -> renderMinimum(requirement)
      is Requirement.Max -> renderMaximum(requirement)
      is Requirement.And -> renderTagRequirementGroup(requirement)
      is Requirement.Eval,
      is Requirement.Exact,
      is Requirement.Or -> null
      is Requirement.Transform -> renderProductionRequirement(requirement)
    }

private fun renderProductionRequirement(requirement: Requirement.Transform): String? {
  if (requirement.transformKind != PROD) return null
  val minimum = requirement.requirement as? Requirement.Min ?: return null
  if (minimum.target != 1) return null
  val metric = minimum.metric as? Metric.Count ?: return null
  if (!metric.expression.simple || !isStandardResource(metric.expression.className)) return null
  return "Requires that you have ${componentNoun(metric.expression.className, 1)} production."
}

private fun renderMinimum(requirement: Requirement.Min): String? {
  val metric = requirement.metric as? Metric.Count ?: return null
  val expression = metric.expression
  val style = Describers[expression.className].requirement
  val target = requirement.target
  return when (style) {
    ComponentDescriber.Requirement.CITY_TILES_IN_PLAY -> {
      if (!inPlay(expression)) return null
      "Requires $target city ${if (target == 1) "tile" else "tiles"} in play."
    }
    ComponentDescriber.Requirement.COLONIES -> {
      if (!expression.simple) return null
      "Requires $target ${if (target == 1) "colony" else "colonies"}."
    }
    ComponentDescriber.Requirement.GREENERY_TILES -> {
      if (!expression.simple) return null
      "Requires that you have $target greenery ${if (target == 1) "tile" else "tiles"}."
    }
    ComponentDescriber.Requirement.OCEAN_TILES -> {
      if (!expression.simple) return null
      "Requires $target ocean ${if (target == 1) "tile" else "tiles"}."
    }
    ComponentDescriber.Requirement.OXYGEN_PERCENT -> {
      if (!expression.simple) return null
      "Requires $target% oxygen."
    }
    ComponentDescriber.Requirement.TEMPERATURE -> {
      if (!expression.simple) return null
      "Requires ${temperature(target)} or warmer."
    }
    ComponentDescriber.Requirement.TERRAFORM_RATING -> {
      if (!expression.simple) return null
      "Requires that you have at least $target TR."
    }
    ComponentDescriber.Requirement.VENUS_PERCENT -> {
      if (!expression.simple) return null
      "Requires Venus ${target * 2}%."
    }
    null -> renderTagRequirement(requirement)
  }
}

private fun renderMaximum(requirement: Requirement.Max): String? {
  val metric = requirement.metric as? Metric.Count ?: return null
  val expression = metric.expression
  if (!expression.simple) return null
  val target = requirement.target
  return when (Describers[expression.className].requirement) {
    ComponentDescriber.Requirement.COLONIES ->
        "You must have no more than $target ${if (target == 1) "colony" else "colonies"}."
    ComponentDescriber.Requirement.OCEAN_TILES -> "There must be $target or fewer ocean tiles."
    ComponentDescriber.Requirement.OXYGEN_PERCENT -> "Oxygen must be $target% or less."
    ComponentDescriber.Requirement.TEMPERATURE ->
        "Temperature must be ${temperature(target)} or colder."
    ComponentDescriber.Requirement.VENUS_PERCENT -> "Venus must be ${target * 2}% or less."
    ComponentDescriber.Requirement.CITY_TILES_IN_PLAY,
    ComponentDescriber.Requirement.GREENERY_TILES,
    ComponentDescriber.Requirement.TERRAFORM_RATING,
    null -> null
  }
}

private fun inPlay(expression: dev.martianzoo.pets.ast.Expression): Boolean =
    expression.arguments == listOf(anyoneExpression) &&
        expression.refinement == null &&
        !expression.complement

private fun renderTagRequirement(requirement: Requirement.Min): String? {
  val (tagName) = tagName(requirement) ?: return null
  return if (requirement.target == 1) {
    "Requires ${indefiniteArticle(tagName)} $tagName tag."
  } else {
    "Requires ${requirement.target} $tagName tags."
  }
}

private fun renderTagRequirementGroup(requirement: Requirement.And): String? {
  val tags =
      requirement.requirements.map { child ->
        val minimum = child as? Requirement.Min ?: return null
        if (minimum.target != 1) return null
        tagName(minimum) ?: return null
      }
  val allPlanetTags = tags.all { (_, planet) -> planet }
  if (!allPlanetTags && tags.any { (_, planet) -> planet }) return null
  val nouns =
      if (allPlanetTags) {
        tags.map { (name) -> name }
      } else {
        tags.map { (name) -> "${indefiniteArticle(name)} $name tag" }
      }
  return "Requires ${englishList(nouns)}${if (allPlanetTags) " tags" else ""}."
}

private fun tagName(requirement: Requirement.Min): Pair<String, Boolean>? {
  val metric = requirement.metric as? Metric.Count ?: return null
  if (!metric.expression.simple) return null
  return tagName(metric.expression.className)
}

private fun englishList(parts: List<String>): String =
    when (parts.size) {
      1 -> parts.single()
      2 -> parts.joinToString(" and ")
      else -> parts.dropLast(1).joinToString(", ") + ", and " + parts.last()
    }

private fun indefiniteArticle(noun: String): String = if (noun.first() in "aeiou") "an" else "a"

private fun temperature(steps: Int): String {
  val degreesCelsius = -30 + 2 * steps
  return "${if (degreesCelsius > 0) "+" else ""}${degreesCelsius}°C"
}

private val anyoneExpression = cn("Anyone").expression
