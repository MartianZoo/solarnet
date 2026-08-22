package dev.martianzoo.tfm.language

import dev.martianzoo.api.SystemClasses.OWNED
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.Prod
import dev.martianzoo.types.Dependency.Key
import dev.martianzoo.types.Type

internal fun lowerProductionSyntax(instructionTree: InstructionTree): InstructionTree =
    productionSyntaxLowerer.transformInstructionTree(instructionTree)

internal fun lowerProductionSyntax(action: Action): Action =
    productionSyntaxLowerer.transformAction(action)

internal fun lowerProductionSyntax(effect: Effect): Effect =
    productionSyntaxLowerer.transformEffect(effect)

internal fun lowerProductionSyntax(requirement: Requirement): Requirement =
    productionSyntaxLowerer.transformRequirement(requirement)

private val productionSyntaxLowerer by lazy { Prod.deprodify(Canon.classTable) }

internal fun productionExpression(
    expression: Expression,
    describers: Describers,
): ProductionExpression? =
    parseProductionExpression(expression, describers)?.takeIf { production ->
      describers.plainGainNoun(production.resource, 1) != null
    }

internal fun productionCategoryExpression(
    expression: Expression,
    describers: Describers,
): ProductionExpression? =
    parseProductionExpression(expression, describers)?.takeIf { production ->
      describers.plainGainCategoryNoun(production.resource, 1) != null
    }

private fun parseProductionExpression(
    expression: Expression,
    describers: Describers,
): ProductionExpression? {
  val resolved =
      describers.resolveExpression(expression)
          ?: return parseContextualProductionExpression(expression, describers)
  if (!describers.isProduction(resolved.type.rootClass.className)) return null
  val resource =
      resolved.dependency(Key(PRODUCTION, 0))?.representedType()?.takeIf { it.refinement == null }
          ?: return null
  val ownerKey = Key(OWNED, 0)
  if (resolved.dependency(ownerKey) == null) return null
  val owner = resolved.sourceDependency(ownerKey)?.takeUnless { it == describers.ownerExpression }
  return ProductionExpression(owner, resource.className)
}

// TODO: Resolve contextual This through linked type sources, then delete this positional fallback.
private fun parseContextualProductionExpression(
    expression: Expression,
    describers: Describers,
): ProductionExpression? {
  if (
      !describers.isProduction(expression.className) ||
          expression.refinement != null ||
          expression.complement
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
  if (!resource.simple) return null
  val owner = expression.arguments.dropLast(1).singleOrNull()
  if (expression.arguments.size > 2) return null
  return ProductionExpression(owner, resource.className)
}

internal fun selectedProductionResource(
    expression: Expression,
    describers: Describers,
): Expression? {
  if (!describers.isProduction(expression.className) || expression.complement) return null
  val resolved = describers.resolveExpression(expression) ?: return null
  val resourceKey = Key(PRODUCTION, 0)
  if (resolved.sourceDependencies.keys != setOf(resourceKey)) return null
  return resolved.dependency(resourceKey)?.representedExpression()
}

private fun Type.representedType(): Type? = representedClass?.baseType

private fun Type.representedExpression(): Expression? {
  val represented = representedType() ?: return null
  return represented.expression.copy(refinement = refinement)
}

internal data class ProductionExpression(
    val owner: Expression?,
    val resource: ClassName,
)

private val CLASS = cn("Class")
private val PRODUCTION = cn("Production")
