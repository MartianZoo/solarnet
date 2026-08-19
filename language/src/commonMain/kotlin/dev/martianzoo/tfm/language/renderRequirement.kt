package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.Requirement

internal fun renderRequirement(requirement: Requirement): String? =
    renderLoweredRequirement(lowerProductionSyntax(requirement))

private fun renderLoweredRequirement(requirement: Requirement): String? =
    when (requirement) {
      is Requirement.Min -> renderMinimum(requirement)
      is Requirement.Max -> renderMaximum(requirement)
      is Requirement.And -> Describers.renderRequirementGroup(requirement)
      is Requirement.Eval,
      is Requirement.Exact,
      is Requirement.Or -> null
      is Requirement.Transform -> null
    }

private fun renderMinimum(requirement: Requirement.Min): String? =
    Describers.renderMinimum(requirement)

private fun renderMaximum(requirement: Requirement.Max): String? =
    Describers.renderMaximum(requirement)
