package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Requirement

internal fun renderRequirement(requirement: Requirement): String? =
    when (requirement) {
      is Requirement.Min -> oxygenTarget(requirement)?.let { "Requires $it% oxygen." }
      is Requirement.Max -> oxygenTarget(requirement)?.let { "Oxygen must be $it% or less." }
      is Requirement.And,
      is Requirement.Eval,
      is Requirement.Exact,
      is Requirement.Or,
      is Requirement.Transform -> null
    }

private fun oxygenTarget(requirement: Requirement.Counting): Int? {
  val metric = requirement.metric as? Metric.Count ?: return null
  if (!metric.expression.simple || metric.expression.className != oxygenStep) return null
  return requirement.target
}

private val oxygenStep = cn("OxygenStep")
