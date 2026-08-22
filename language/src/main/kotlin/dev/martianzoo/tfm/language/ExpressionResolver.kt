package dev.martianzoo.tfm.language

import dev.martianzoo.api.Exceptions.ExpressionException
import dev.martianzoo.api.SystemClasses.OWNED
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.tfm.data.TfmClasses.PRODUCTION
import dev.martianzoo.tfm.data.TfmClasses.STANDARD_RESOURCE
import dev.martianzoo.types.Class
import dev.martianzoo.types.Dependency.Key
import dev.martianzoo.types.Dependency.TypeDependency
import dev.martianzoo.types.DependencySet.DependencyPath

/**
 * Resolves authored expressions and answers structural Class questions for the English renderer.
 */
internal class ExpressionResolver(classes: Set<Class>) {
  private val classTable =
      requireNotNull(classes.firstOrNull()?.classTable) {
        "English descriptions must include at least one Class"
      }
  internal val classesByName = classTable.allClasses().associateBy(Class::className)

  init {
    require(classes.all { it.classTable === classTable })
  }

  internal fun resolve(expression: Expression): ResolvedExpression? =
      resolve(expression, contextualThisKey = null)

  internal fun resolve(
      expression: Expression,
      contextualThisKey: Key?,
  ): ResolvedExpression? {
    if (expression.complement) return null
    val declaredClass = classesByName[expression.className] ?: return null
    val playerOwned = declaredClass.isSubtypeOf(classesByName.getValue(OWNED))
    val directSourceType =
        try {
          classTable.resolve(expression)
        } catch (_: ExpressionException) {
          null
        }
    val semanticSourceArguments =
        if (directSourceType != null) {
          expression.arguments
        } else {
          val contextualThisType = contextualThisKey?.let { key ->
            if (key !in declaredClass.baseType.dependencies.keys) return@let null
            (declaredClass.baseType.dependencies.at(DependencyPath(listOf(key))) as? TypeDependency)
                ?.boundType
          }
          expression.arguments.map { argument ->
            when {
              argument == thisExpression && contextualThisType != null ->
                  contextualThisType.expression
              playerOwned && (argument == anyoneExpression || argument == notOwnerExpression) ->
                  playerExpression
              else -> argument
            }
          }
        }
    val sourceType =
        directSourceType
            ?: try {
              classTable.resolve(expression.copy(arguments = semanticSourceArguments))
            } catch (_: ExpressionException) {
              return null
            }
    val rootClass = sourceType.rootClass
    val sourceKeys = rootClass.matchDependencyKeys(semanticSourceArguments)
    val sourceDependencies = sourceKeys.zip(expression.arguments).toMap()
    val semanticSourceDependencies = sourceKeys.zip(semanticSourceArguments).toMap()
    val defaultArguments = rootClass.defaultType.expressionFull.arguments
    val defaults = rootClass.matchDependencyKeys(defaultArguments).zip(defaultArguments).toMap()
    val defaultedKeys =
        rootClass.dependencies.keys.filterTo(linkedSetOf()) { key ->
          val path = DependencyPath(listOf(key))
          rootClass.defaultType.dependencies.at(path) != rootClass.baseType.dependencies.at(path)
        }
    val semanticArguments =
        rootClass.dependencies.keys.map { semanticSourceDependencies[it] ?: defaults.getValue(it) }
    val semanticExpression = expression.copy(arguments = semanticArguments)
    val semanticType =
        try {
          classTable.resolve(semanticExpression)
        } catch (_: ExpressionException) {
          return null
        }
    return ResolvedExpression(
        semanticType,
        sourceDependencies.keys + defaultedKeys,
        sourceDependencies,
    )
  }

  internal fun representedClass(expression: Expression): Expression? {
    val classType = representedClassType(expression) ?: return null
    val represented = classType.representedClass ?: return null
    if (classType.refinement != null) return null
    return represented.className.expression
  }

  internal fun representedExpression(expression: Expression): Expression? {
    val classType = representedClassType(expression) ?: return null
    val represented = classType.representedClass?.baseType ?: return null
    return represented.expression.copy(refinement = classType.refinement)
  }

  internal fun representedClassArgument(expression: Expression): Expression? {
    val type = resolve(expression)?.type ?: return null
    val represented = type.representedClass?.baseType ?: return null
    return represented.expression.copy(refinement = type.refinement)
  }

  internal fun resolveCardResource(expression: Expression): ResolvedExpression? {
    if (!isCardResource(expression.className)) return null
    return resolve(expression, Key(CARD_RESOURCE, 0))
  }

  internal fun cardResourceHolder(resolved: ResolvedExpression): Expression? =
      resolved.sourceDependency(Key(CARD_RESOURCE, 0))

  internal fun cardResourceHasHolder(
      resolved: ResolvedExpression,
      holder: Expression,
  ): Boolean {
    val holderKey = Key(CARD_RESOURCE, 0)
    val ownerKey = Key(OWNED, 0)
    if (resolved.sourceDependency(holderKey) != holder) return false
    return resolved.sourceDependencies.all { (key, source) ->
      key == holderKey || (key == ownerKey && source == ownerExpression)
    }
  }

  private fun representedClassType(expression: Expression) =
      resolve(expression)?.let { resolved ->
        val key =
            resolved.type.rootClass.dependencies.keys.singleOrNull {
              resolved.selectedDependency(it)?.rootClass?.className == CLASS
            } ?: return@let null
        resolved.dependency(key)
      }

  internal fun concrete(className: ClassName): Boolean = classesByName[className]?.abstract == false

  internal fun isStandardResource(className: ClassName): Boolean =
      isSubtypeOf(className, STANDARD_RESOURCE)

  internal fun isCardResource(className: ClassName): Boolean = isSubtypeOf(className, CARD_RESOURCE)

  internal fun isTag(className: ClassName): Boolean = isSubtypeOf(className, TAG)

  internal fun isPlanetTag(className: ClassName): Boolean = isSubtypeOf(className, PLANET_TAG)

  internal fun isProduction(className: ClassName): Boolean = isSubtypeOf(className, PRODUCTION)

  internal fun isGameParticipant(className: ClassName): Boolean = isSubtypeOf(className, PLAYER)

  internal fun isGenerationScoped(className: ClassName): Boolean =
      isSubtypeOf(className, GENERATIONAL)

  internal fun isEndTrigger(className: ClassName): Boolean = isSubtypeOf(className, END)

  private fun isSubtypeOf(className: ClassName, superclassName: ClassName): Boolean =
      classesByName.getValue(className).isSubtypeOf(classesByName.getValue(superclassName))

  internal val anyoneExpression = cn("Anyone").expression
  internal val notOwnerExpression = cn("Owner").expression.copy(complement = true)
  internal val ownerExpression = cn("Owner").expression
  internal val playerExpression = cn("Player").expression
  internal val thisExpression = cn("This").expression

  private companion object {
    val CARD_RESOURCE = cn("CardResource")
    val CLASS = cn("Class")
    val END = cn("End")
    val GENERATIONAL = cn("Generational")
    val PLANET_TAG = cn("PlanetTag")
    val PLAYER = cn("Player")
    val TAG = cn("Tag")
  }
}
