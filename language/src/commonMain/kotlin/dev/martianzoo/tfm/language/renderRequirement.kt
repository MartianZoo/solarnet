package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.tfm.canon.Canon

internal fun renderRequirement(requirement: Requirement): String? =
    when (requirement) {
      is Requirement.Min -> renderMinimum(requirement)
      is Requirement.Max -> renderMaximum(requirement)
      is Requirement.And -> renderTagRequirementGroup(requirement)
      is Requirement.Eval,
      is Requirement.Exact,
      is Requirement.Or,
      is Requirement.Transform -> null
    }

private fun renderMinimum(requirement: Requirement.Min): String? =
    target(requirement, oxygenStep)?.let { "Requires $it% oxygen." }
        ?: target(requirement, temperatureStep)?.let { "Requires ${temperature(it)} or warmer." }
        ?: target(requirement, oceanTile)?.let {
          "Requires $it ocean ${if (it == 1) "tile" else "tiles"}."
        }
        ?: target(requirement, venusStep)?.let { "Requires Venus ${it * 2}%." }
        ?: target(requirement, terraformRating)?.let {
          "Requires that you have at least $it TR."
        }
        ?: target(requirement, greeneryTile)?.let {
          "Requires that you have $it greenery ${if (it == 1) "tile" else "tiles"}."
        }
        ?: target(requirement, colony)?.let {
          "Requires $it ${if (it == 1) "colony" else "colonies"}."
        }
        ?: renderTagRequirement(requirement)

private fun renderMaximum(requirement: Requirement.Max): String? =
    target(requirement, oxygenStep)?.let { "Oxygen must be $it% or less." }
        ?: target(requirement, temperatureStep)?.let {
          "Temperature must be ${temperature(it)} or colder."
        }
        ?: target(requirement, oceanTile)?.let {
          "There must be $it or fewer ocean tiles."
        }
        ?: target(requirement, venusStep)?.let { "Venus must be ${it * 2}% or less." }

private fun target(requirement: Requirement.Counting, className: ClassName): Int? {
  val metric = requirement.metric as? Metric.Count ?: return null
  if (!metric.expression.simple || metric.expression.className != className) return null
  return requirement.target
}

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
  val className = metric.expression.className
  val componentClass = Canon.classTable.findClass(className) ?: return null
  if (componentClass.abstract || !componentClass.isSubtypeOf(Canon.classTable.getClass(tag))) {
    return null
  }
  val ordinaryName = className.toString().removeSuffix("Tag").lowercase()
  val isPlanetTag = componentClass.isSubtypeOf(Canon.classTable.getClass(planetTag))
  val name = if (isPlanetTag) ordinaryName.replaceFirstChar(Char::uppercaseChar) else ordinaryName
  return name to isPlanetTag
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

private val colony = cn("Colony")
private val greeneryTile = cn("GreeneryTile")
private val oceanTile = cn("OceanTile")
private val oxygenStep = cn("OxygenStep")
private val planetTag = cn("PlanetTag")
private val tag = cn("Tag")
private val temperatureStep = cn("TemperatureStep")
private val terraformRating = cn("TerraformRating")
private val venusStep = cn("VenusStep")
