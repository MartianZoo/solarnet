package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Requirement

internal fun renderRequirement(requirement: Requirement): String? =
    when (requirement) {
      is Requirement.Min -> renderMinimum(requirement)
      is Requirement.Max -> renderMaximum(requirement)
      is Requirement.And,
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

private fun temperature(steps: Int): String {
  val degreesCelsius = -30 + 2 * steps
  return "${if (degreesCelsius > 0) "+" else ""}${degreesCelsius}°C"
}

private val oceanTile = cn("OceanTile")
private val oxygenStep = cn("OxygenStep")
private val temperatureStep = cn("TemperatureStep")
private val venusStep = cn("VenusStep")
