package dev.martianzoo.pets

import dev.martianzoo.pets.TransformHandler.Companion.selectedMe
import dev.martianzoo.pets.Transforming.replaceThisExpressionsWith
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.api.SystemClasses.CLASS
import dev.martianzoo.pets.api.SystemClasses.OWNED
import dev.martianzoo.pets.api.SystemClasses.SYSTEM
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger.ByTrigger
import dev.martianzoo.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.pets.ast.Effect.Trigger.OnRemoveOf
import dev.martianzoo.pets.ast.Effect.Trigger.XTrigger
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Expression.Refinement.Has
import dev.martianzoo.pets.ast.Expression.TypeVariableName.Declaration
import dev.martianzoo.pets.ast.Expression.TypeVariableName.Reference
import dev.martianzoo.pets.ast.Expression.TypeVariableName.Resolution
import dev.martianzoo.pets.ast.FromExpression.Compact
import dev.martianzoo.pets.ast.FromExpression.Unchanged
import dev.martianzoo.pets.ast.Instruction.Each
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.withTypeVariables
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.pets.types.Dependency.Key
import dev.martianzoo.pets.util.invoke

/**
 * Ownership insertion inside explicit OWN syntax; all traversal and deferral use ordinary handlers.
 */
internal class Own(private val classTable: ClassTable) : TransformHandler {
  override val idempotent = true

  override fun transform(inner: PetNode, scope: TransformHandler.Scope): PetNode =
      insertOwners(inner, scope)

  private fun insertOwners(inner: PetNode, initialScope: TransformHandler.Scope): PetNode {
    val context = initialScope.context
    val owned = classTable.findClass(OWNED) ?: return inner
    val ownerKey = owned.dependencies.keys.single()
    var activeRepresentedClassMarkers = initialScope.representedClassMarkers
    val refinementCandidates = mutableListOf<Expression?>()

    fun <T> withinRefinementCandidate(candidate: Expression?, block: () -> T): T {
      refinementCandidates.add(candidate)
      return try {
        block()
      } finally {
        refinementCandidates.removeLast()
      }
    }

    fun matchedKeys(expression: Expression): List<Key> {
      val klass = classTable.getClass(expression.className)
      val contextualArguments =
          expression.arguments.map { argument ->
            replaceThisExpressionsWith(context).transformExpression(argument)
          }
      return klass.argumentDependencies.matchPartial(contextualArguments, classTable).keys
    }

    fun missingOwningArgument(expression: Expression): Boolean {
      val klass = classTable.findClass(expression.className) ?: return false
      if (!klass.isSubtypeOf(owned)) return false
      val marker = expression.typeVariableName
      if (
          marker != null &&
              !(marker is Reference && marker.identity in activeRepresentedClassMarkers) &&
              marker !is Declaration
      )
          return false
      if (ownerKey !in klass.argumentDependencies.keys) return false
      val supplied = matchedKeys(expression)
      if (ownerKey in supplied || klass.dependencyDeterminedBy(ownerKey, supplied)) return false
      val candidate = refinementCandidates.lastOrNull() ?: return true
      val candidateKey =
          try {
            klass.matchDependencyKeys(listOf(candidate), classTable).single()
          } catch (_: ExpressionException) {
            return true
          }
      return ownerKey != candidateKey &&
          !klass.dependencyDeterminedBy(ownerKey, supplied + candidateKey)
    }

    fun insert(expression: Expression, me: Expression): Expression {
      if (!missingOwningArgument(expression)) return expression
      val klass = classTable.getClass(expression.className)
      val existing = matchedKeys(expression).zip(expression.arguments).toMap()
      val arguments =
          klass.argumentDependencies.keys.mapNotNull { key ->
            existing[key] ?: me.takeIf { key == ownerKey }
          }
      val marker = expression.typeVariableName
      val insertedMarker =
          if (marker is Reference) {
            Reference(marker.name, marker.boundClassName, argumentsSpecified = true)
                .resolved(requireNotNull(marker.resolution))
          } else marker
      return expression
          .copy(
              arguments = arguments,
              argumentsSpecified = true,
              typeVariableName = insertedMarker,
          )
          .also {
            it.sourceLocation = expression.sourceLocation
          }
    }

    fun transformRefinementForCandidate(
        transformer: PetTransformer,
        refinement: Expression.Refinement,
        candidate: Expression,
    ): Expression.Refinement =
        when (refinement) {
          is Expression.Refinement.And ->
              Expression.Refinement.create(
                  refinement.refinements.map {
                    transformRefinementForCandidate(transformer, it, candidate)
                  }
              )
          is Has ->
              withinRefinementCandidate(candidate) { transformer.transformRefinement(refinement) }
          is Expression.Refinement.Not -> transformer.transformRefinement(refinement)
        }

    fun needsMe(node: PetNode, available: Boolean = false): Boolean {
      // An OWN-marked EVAL must capture its trigger player before its value can be expanded.
      if (
          !available &&
              ((node is Metric.Eval && node.me == null) ||
                  (node is Requirement.Eval && node.me == null))
      )
          return true
      if (node is Each) {
        val selected = available || selectedMe(node.selector) != null
        return needsMe(node.selector, selected) || needsMe(node.body, selected)
      }
      if (node is Metric.Rank) {
        val selected = available || node.selector?.let(::selectedMe) != null
        return withinRefinementCandidate(null) {
          (node.selector?.let { needsMe(it, selected) } ?: false) ||
              node.metrics.any { needsMe(it, selected) }
        }
      }
      if (node is Expression) {
        if (node.className == CLASS) {
          activeRepresentedClassMarkers +=
              node.arguments.mapNotNull { it.typeVariableName?.identity }
          return node.refinement?.let { needsMe(it, available) } ?: false
        }
        if (missingOwningArgument(node) && !available) return true
        if (withinRefinementCandidate(null) { node.arguments.any { needsMe(it, available) } })
            return true
        val candidate = node.copy(refinement = null)
        fun needsMeInRefinement(refinement: Expression.Refinement): Boolean =
            when (refinement) {
              is Expression.Refinement.And -> refinement.refinements.any(::needsMeInRefinement)
              is Has -> {
                withinRefinementCandidate(candidate) {
                  needsMe(refinement.requirement, available)
                }
              }
              is Expression.Refinement.Not -> needsMe(refinement.excluded, available)
            }
        return node.refinement?.let(::needsMeInRefinement) ?: false
      }
      return node.immediateChildren().any { needsMe(it, available) }
    }

    fun supplyFromTrigger(trigger: Effect.Trigger): Pair<Effect.Trigger, Expression> {
      if (trigger.descendantsOfType<Effect.Trigger.Or>().any()) {
        throw PetSyntaxException(
            "an OR trigger cannot implicitly supply `Me`; bind `Me` around the whole trigger",
            sourceLocation = trigger.sourceLocation,
        )
      }
      val resolution = Resolution()
      val declaration =
          Expression(
              dev.martianzoo.pets.api.SystemClasses.PLAYER,
              typeVariableName =
                  Declaration("Me", dev.martianzoo.pets.api.SystemClasses.PLAYER)
                      .resolved(resolution),
          )
      val me =
          Expression(
              dev.martianzoo.pets.api.SystemClasses.PLAYER,
              typeVariableName =
                  Reference("Me", dev.martianzoo.pets.api.SystemClasses.PLAYER)
                      .resolved(resolution),
          )
      var supplied = false
      val marked =
          object : PetTransformer() {
                override fun transformNode(node: PetNode): PetNode {
                  if (!supplied && node is OnGainOf && missingOwningArgument(node.expression)) {
                    supplied = true
                    return OnGainOf.create(insert(node.expression, declaration))
                  }
                  if (!supplied && node is OnRemoveOf && missingOwningArgument(node.expression)) {
                    supplied = true
                    return OnRemoveOf.create(insert(node.expression, declaration))
                  }
                  return transformChildren(node)
                }
              }
              .transformTrigger(trigger)
      if (
          !supplied &&
              trigger.descendantsOfType<Effect.Trigger>().any { part ->
                val watched =
                    when (part) {
                      is OnGainOf -> part.expression
                      is OnRemoveOf -> part.expression
                      Effect.Trigger.WhenGain,
                      Effect.Trigger.WhenRemove -> context
                      else -> null
                    }
                watched != null &&
                    classTable.getClass(watched.className).allSuperclasses().any {
                      it.className == SYSTEM
                    }
              }
      ) {
        throw PetSyntaxException(
            "a System trigger cannot implicitly supply a Player through BY; bind Me explicitly",
            sourceLocation = trigger.sourceLocation,
        )
      }
      return (if (supplied) marked else ByTrigger(marked, declaration)) to me
    }

    return object : TransformHandler.Rewriter("OWN", initialScope) {
          private var explicitActor = false

          override fun rewrite(node: PetNode): PetNode {
            if (node is Effect) {
              activeRepresentedClassMarkers += scope.representedClassMarkers
              if (scope.me == null && needsMe(node.instruction)) {
                val (trigger, me) = supplyFromTrigger(node.trigger)
                scope = scope.copy(me = me)
                return transformChildren(
                    node.copy(trigger = trigger).withTypeVariables(node.typeVariables)
                )
              }
              // With locally supplied recipients, a whole effect can still watch a global
              // condition.
              if (scope.me == null) {
                return node
                    .copy(instruction = transformInstructionTree(node.instruction))
                    .withTypeVariables(node.typeVariables.transformedBy(this))
              }
              return transformChildren(node)
            }
            if (node is ByTrigger) {
              val previous = explicitActor
              explicitActor = true
              return try {
                transformChildren(node)
              } finally {
                explicitActor = previous
              }
            }
            if (!explicitActor && scope.me != null) {
              val basic = if (node is XTrigger) node.inner else node
              val watched =
                  when (basic) {
                    is OnGainOf -> basic.expression
                    is OnRemoveOf -> basic.expression
                    else -> null
                  }
              if (
                  watched != null &&
                      classTable.getClass(watched.className).allSuperclasses().none {
                        it.className == OWNED || it.className == SYSTEM
                      }
              ) {
                explicitActor = true
                val transformed =
                    try {
                      transformChildren(node)
                    } finally {
                      explicitActor = false
                    }
                return ByTrigger(transformed as Effect.Trigger, requireNotNull(scope.me))
              }
            }
            if (node is Metric.Rank)
                return withinRefinementCandidate(null) { transformChildren(node) }
            if (node is Compact) {
              val transformed =
                  withinRefinementCandidate(null) { transformChildren(node) as Compact }
              if (!missingOwningArgument(transformed.toExpression)) return transformed
              val currentMe =
                  scope.me
                      ?: throw ExpressionException(
                          "OWN requires a visible Me binding for `$transformed`; " +
                              "remove a redundant inner OWN or bind Me explicitly in the trigger"
                      )
              val klass = classTable.getClass(transformed.className)
              val byKey = matchedKeys(transformed.toExpression).zip(transformed.arguments).toMap()
              return transformed.copy(
                  arguments =
                      klass.argumentDependencies.keys.mapNotNull { key ->
                        byKey[key] ?: Unchanged(currentMe).takeIf { key == ownerKey }
                      }
              )
            }
            if (node is Expression) {
              if (node.className == CLASS) {
                activeRepresentedClassMarkers += scope.representedClassMarkers
                activeRepresentedClassMarkers +=
                    node.arguments.mapNotNull { it.typeVariableName?.identity }
              }
              val shell =
                  if (node.className == CLASS) node.copy(refinement = null)
                  else
                      withinRefinementCandidate(null) {
                        transformChildren(node.copy(refinement = null)) as Expression
                      }
              if (scope.me == null && missingOwningArgument(shell)) {
                throw ExpressionException(
                    "OWN requires a visible Me binding for `$shell`; " +
                        "remove a redundant inner OWN or bind Me explicitly in the trigger"
                )
              }
              val ownedShell = scope.me?.let { insert(shell, it) } ?: shell
              val refinement =
                  node.refinement?.let {
                    transformRefinementForCandidate(this, it, ownedShell)
                  }
              return ownedShell.copy(refinement = refinement).also {
                it.sourceLocation = node.sourceLocation
              }
            }
            return transformChildren(node)
          }
        }
        .transformWithoutKindCheck(inner)
  }
}
