package dev.martianzoo.tfm.language

import dev.martianzoo.api.SystemClasses.CLASS
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.Prod
import dev.martianzoo.tfm.data.TfmClasses.PRODUCTION

internal fun lowerProductionSyntax(instructionTree: InstructionTree): InstructionTree =
    productionSyntaxLowerer.transformInstructionTree(instructionTree)

internal fun lowerProductionSyntax(requirement: Requirement): Requirement =
    productionSyntaxLowerer.transformRequirement(requirement)

internal fun standardResourceProduction(
    expression: Expression,
): Pair<List<Expression>, ClassName>? {
  if (
      expression.className != PRODUCTION || expression.refinement != null || expression.complement
  ) {
    return null
  }
  val resourceDependency = expression.arguments.lastOrNull() ?: return null
  if (
      resourceDependency.className != CLASS ||
          resourceDependency.arguments.size != 1 ||
          resourceDependency.refinement != null ||
          resourceDependency.complement
  ) {
    return null
  }
  val resource = resourceDependency.arguments.single()
  if (!resource.simple || !isStandardResource(resource.className)) return null
  return expression.arguments.dropLast(1) to resource.className
}

private val productionSyntaxLowerer by lazy { Prod.deprodify(Canon.classTable) }
