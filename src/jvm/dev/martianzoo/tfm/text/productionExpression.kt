package dev.martianzoo.tfm.text

import dev.martianzoo.pets.api.SystemClasses.OWNED
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.types.Dependency.Key
import dev.martianzoo.pets.types.Type

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

internal fun refinedProductionCategoryExpressions(
    expression: Expression,
    describers: Describers,
): List<ProductionExpression>? {
  val resolved = describers.resolveExpression(expression) ?: return null
  if (!describers.isProduction(resolved.type.rootClass.className)) return null
  val resource = resolved.dependency(Key(PRODUCTION, 0)) ?: return null
  if (resource.refinement == null) return null
  val ownerKey = Key(OWNED, 0)
  if (resolved.dependency(ownerKey) == null) return null
  val owner = resolved.sourceDependency(ownerKey)?.takeUnless { it == describers.ownerExpression }
  return resource
      .allConcreteSubtypes()
      .mapNotNull { it.representedClass?.className }
      .distinct()
      .map { ProductionExpression(owner, it) }
      .toList()
      .takeIf { productions ->
        productions.isNotEmpty() &&
            productions.all { describers.plainGainCategoryNoun(it.resource, 1) != null }
      }
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
      resolved.dependency(Key(PRODUCTION, 0))?.takeIf { it.refinement == null }?.representedType()
          ?: return null
  val ownerKey = Key(OWNED, 0)
  if (resolved.dependency(ownerKey) == null) return null
  val owner = resolved.sourceDependency(ownerKey)?.takeUnless { it == describers.ownerExpression }
  return ProductionExpression(owner, resource.className)
}

// TODO: Resolve contextual This through its Type Variable, then delete this positional fallback.
private fun parseContextualProductionExpression(
    expression: Expression,
    describers: Describers,
): ProductionExpression? {
  if (!describers.isProduction(expression.className) || expression.refinement != null) {
    return null
  }
  val resourceDependency = expression.arguments.lastOrNull() ?: return null
  if (
      resourceDependency.className != CLASS ||
          resourceDependency.arguments.size != 1 ||
          resourceDependency.refinement != null
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
  if (
      !describers.isProduction(expression.className) ||
          expression.refinement is Expression.Refinement.Not
  ) {
    return null
  }
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
