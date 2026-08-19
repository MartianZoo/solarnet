package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.Requirement

internal fun renderRequirement(requirement: Requirement, describers: Describers): String? =
    renderLoweredRequirement(lowerProductionSyntax(requirement), describers)

private fun renderLoweredRequirement(requirement: Requirement, describers: Describers): String? =
    when (requirement) {
      is Requirement.Min -> renderMinimum(requirement, describers)
      is Requirement.Max -> renderMaximum(requirement, describers)
      is Requirement.And -> describers.renderRequirementGroup(requirement)
      is Requirement.Eval,
      is Requirement.Exact,
      is Requirement.Or -> null
      is Requirement.Transform -> null
    }

private fun renderMinimum(requirement: Requirement.Min, describers: Describers): String? =
    describers.renderMinimum(requirement)

private fun renderMaximum(requirement: Requirement.Max, describers: Describers): String? =
    describers.renderMaximum(requirement)
