package dev.martianzoo.engine

import dev.martianzoo.pets.PetElaborator
import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.PetTransformer.Companion.chain
import dev.martianzoo.pets.Transforming.bindXTo
import dev.martianzoo.pets.Transforming.replaceThisExpressionsWith
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.InvalidPetDefinitionException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.PetException
import dev.martianzoo.pets.api.SystemClasses.ACTOR
import dev.martianzoo.pets.api.SystemClasses.OWNED
import dev.martianzoo.pets.api.SystemClasses.SYSTEM
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger
import dev.martianzoo.pets.ast.Effect.Trigger.BasicTrigger
import dev.martianzoo.pets.ast.Effect.Trigger.ByTrigger
import dev.martianzoo.pets.ast.Effect.Trigger.IfTrigger
import dev.martianzoo.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.pets.ast.Effect.Trigger.OnRemoveOf
import dev.martianzoo.pets.ast.Effect.Trigger.Or
import dev.martianzoo.pets.ast.Effect.Trigger.Transform
import dev.martianzoo.pets.ast.Effect.Trigger.WhenGain
import dev.martianzoo.pets.ast.Effect.Trigger.WhenRemove
import dev.martianzoo.pets.ast.Effect.Trigger.WrappingTrigger
import dev.martianzoo.pets.ast.Effect.Trigger.XTrigger
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.types.Type
import dev.martianzoo.pets.types.TypeVariable
import dev.martianzoo.pets.types.TypeVariableScope
import dev.martianzoo.state.Actor
import dev.martianzoo.state.Component
import dev.martianzoo.state.GameEvent.ChangeEvent
import dev.martianzoo.state.GameReader
import dev.martianzoo.state.Player
import dev.martianzoo.state.toComponent

/** One specialized component effect ready for subscription matching and firing. */
internal class LiveEffect
private constructor(
    private val subscription: Subscription,
    internal val effect: Effect,
    private val context: Component,
    private val triggerClass: ClassName?,
    private val elaborator: PetElaborator,
) {
  internal val automatic: Boolean
    get() = effect.automatic

  internal val registryKey = RegistryKey(automatic, triggerClass)

  internal val listensToOtherComponents: Boolean
    get() = subscription.listensToOtherComponents

  internal fun onChangeToSelf(
      triggerEvent: ChangeEvent,
      controller: Actor,
      reader: GameReader,
      resolvedChange: ResolvedChange,
  ): PendingTask? = onChange(triggerEvent, controller, reader, resolvedChange, isSelf = true)

  internal fun onChangeToOther(
      triggerEvent: ChangeEvent,
      controller: Actor,
      reader: GameReader,
      resolvedChange: ResolvedChange,
  ): PendingTask? = onChange(triggerEvent, controller, reader, resolvedChange, isSelf = false)

  private fun onChange(
      triggerEvent: ChangeEvent,
      controller: Actor,
      reader: GameReader,
      resolvedChange: ResolvedChange,
      isSelf: Boolean,
  ): PendingTask? {
    val changedComponentPlayer = resolvedChange.changedComponentPlayer
    val hit =
        subscription.checkForHit(
            triggerEvent,
            resolvedChange,
            isSelf,
            reader,
            elaborator,
        ) ?: return null
    val instruction =
        try {
          elaborator.evaluateProperties(
              effect.typeVariables
                  .expandNames()
                  .transformInstructionTree(hit.specialize(effect.instruction)),
              context.expression,
          )
        } catch (e: PetException) {
          throw InvalidPetDefinitionException(
              "invalid effect for component `${context.expression}`: `$effect`: ${e.detail}",
              e,
              e.sourceLocation ?: effect.sourceLocation,
          )
        }
    return PendingTask.fromEffect(
        context = context,
        triggerEvent = triggerEvent,
        controller = controller,
        changedComponentPlayer = changedComponentPlayer,
        automatic = automatic,
        instruction = InstructionGroup.of(instruction),
    )
  }

  override fun equals(other: Any?): Boolean =
      other is LiveEffect &&
          subscription == other.subscription &&
          automatic == other.automatic &&
          effect.instruction == other.effect.instruction &&
          context == other.context &&
          triggerClass == other.triggerClass

  override fun hashCode(): Int = cachedHashCode

  private val cachedHashCode: Int = run {
    var result = subscription.hashCode()
    result = 31 * result + automatic.hashCode()
    result = 31 * result + effect.instruction.hashCode()
    result = 31 * result + context.hashCode()
    result = 31 * result + (triggerClass?.hashCode() ?: 0)
    result
  }

  internal data class RegistryKey(
      private val automatic: Boolean,
      private val triggerClass: ClassName?,
  )

  internal class ResolvedChange(
      val gaining: Type?,
      val removing: Type?,
  ) {
    val changedComponentPlayer: Player? = (gaining ?: removing)?.toComponent()?.owningPlayer

    fun type(matchOnGain: Boolean): Type? = if (matchOnGain) gaining else removing
  }

  internal companion object {
    internal fun compile(
        component: Component,
        elaborator: PetElaborator,
    ): List<LiveEffect> =
        specialize(component, elaborator).map { create(it, component, elaborator) }

    private fun create(
        effect: Effect,
        context: Component,
        elaborator: PetElaborator,
    ): LiveEffect {
      val explicit =
          context.owningPlayer?.let { player ->
            object : PetTransformer() {
                  override fun transformNode(node: PetNode): PetNode {
                    if (node is ByTrigger) return node
                    if (node is OnGainOf || node is OnRemoveOf) {
                      val watched =
                          when (node) {
                            is OnGainOf -> node.expression
                            is OnRemoveOf -> node.expression
                          }
                      val watchedClass = elaborator.classTable.getClass(watched.className)
                      if (
                          watchedClass.allSuperclasses().none {
                            it.className == OWNED || it.className == SYSTEM
                          }
                      ) {
                        return ByTrigger(node as Trigger, player.expression)
                      }
                    }
                    return transformChildren(node)
                  }
                }
                .transformEffect(effect)
          } ?: effect
      // Lowering can consume the trigger-side occurrence (for example PROD), so prefer the frozen
      // authored origins even when they can no longer be rediscovered from the transformed tree.
      val typeVariables = explicit.typeVariables
      val subscription = Subscription.from(explicit.trigger, context, typeVariables)
      val triggerClass = subscription.classToCheck?.let(elaborator.classTable::getClass)?.className
      return LiveEffect(subscription, explicit, context, triggerClass, elaborator)
    }

    private fun specialize(component: Component, elaborator: PetElaborator): List<Effect> {
      val thisBinding = replaceThisExpressionsWith(component.expression)

      return if (component.owner == null || component.owningPlayer != null) {
        elaborator.classEffects(component.type.rootClass).map { effect ->
          val bound =
              try {
                elaborator.specializeEffect(
                    component.type.rootClass.defaultType,
                    component.type,
                    effect,
                    component.expression,
                )
              } catch (e: NarrowingException) {
                throw ExpressionException(
                    "invalid effect for component `${component.type.expressionFull}`: ${e.detail}",
                    e,
                    effect.sourceLocation,
                )
              }
          try {
            elaborator.classTable.checkAllTypes(bound)
            bound
          } catch (e: ExpressionException) {
            throw ExpressionException(
                "invalid effect for component `${component.type.expressionFull}`: `$bound`: ${e.detail}",
                e,
                effect.trigger.sourceLocation
                    ?: effect.trigger.descendantsOfType<Expression>().firstOrNull()?.sourceLocation
                    ?: effect.sourceLocation
                    ?: e.sourceLocation,
            )
          }
        }
      } else {
        elaborator.classEffects(component.type.rootClass).mapNotNull { effect ->
          val contextualizer = thisBinding
          val contextualScope = effect.typeVariables.transformedBy(contextualizer)
          val variableBinding =
              contextualScope.bind(
                  component.type.variableBindingsFrom(
                      component.type.rootClass.defaultType,
                      effect.typeVariables.variables,
                  ),
                  elaborator.classTable,
              )
          val uncheckedBinding =
              chain(
                  contextualizer,
                  variableBinding,
              )
          val bound = uncheckedBinding.transformEffect(effect)
          try {
            elaborator.classTable.checkAllTypes(bound)
            bound
          } catch (e: ExpressionException) {
            // An Owner-only component can inherit an effect whose output is Player-bound. The
            // source effect is valid, but it does not apply to that Owner; for example, the
            // starting tiles owned by SoloOpponent do not score VictoryPoint<Player> components.
            val sourceEffect =
                replaceThisExpressionsWith(component.type.rootClass.className.expression)
                    .transformEffect(effect)
            elaborator.classTable.checkAllTypes(sourceEffect)
            null
          }
        }
      }
    }
  }

  private sealed class Subscription {
    companion object {
      fun from(
          trigger: Trigger,
          context: Component,
          typeVariables: TypeVariableScope,
      ): Subscription {
        return when (trigger) {
          is Or -> AnyOf(trigger.triggers.map { from(it, context, typeVariables) })
          is BasicTrigger -> {
            when (trigger) {
              is WhenGain -> Self(context, matchOnGain = true)
              is WhenRemove -> Self(context, matchOnGain = false)
              is OnGainOf ->
                  Regular(
                      trigger.expression,
                      matchOnGain = true,
                      typeVariables = typeVariables,
                  )
              is OnRemoveOf ->
                  Regular(
                      trigger.expression,
                      matchOnGain = false,
                      typeVariables = typeVariables,
                  )
            }
          }
          is WrappingTrigger -> {
            val inner =
                from(
                    trigger.inner,
                    context,
                    typeVariables,
                )
            when (trigger) {
              is ByTrigger ->
                  Personal(
                      inner,
                      trigger.by,
                      typeVariables,
                      typeVariables.variableDeclaredAt(trigger.by),
                  )
              is IfTrigger -> Conditional(inner, trigger.condition)
              is XTrigger -> CountBinding(inner)
              is Transform -> error("should have been transformed by now: $trigger")
            }
          }
        }
      }
    }

    abstract fun checkForHit(
        currentEvent: ChangeEvent,
        resolvedChange: ResolvedChange,
        isSelf: Boolean,
        reader: GameReader,
        elaborator: PetElaborator,
    ): Hit?

    abstract val classToCheck: ClassName?

    val listensToOtherComponents: Boolean
      get() =
          when (this) {
            is AnyOf -> alternatives.any(Subscription::listensToOtherComponents)
            is Self -> false
            is Regular -> true
            is Personal -> inner.listensToOtherComponents
            is Conditional -> inner.listensToOtherComponents
            is CountBinding -> inner.listensToOtherComponents
          }

    abstract fun transform(transformer: PetTransformer): Subscription

    private data class AnyOf(val alternatives: List<Subscription>) : Subscription() {
      override fun checkForHit(
          currentEvent: ChangeEvent,
          resolvedChange: ResolvedChange,
          isSelf: Boolean,
          reader: GameReader,
          elaborator: PetElaborator,
      ): Hit? {
        alternatives.forEach { alternative ->
          alternative
              .checkForHit(
                  currentEvent,
                  resolvedChange,
                  isSelf,
                  reader,
                  elaborator,
              )
              ?.let {
                return it
              }
        }
        return null
      }

      override val classToCheck = null

      override fun transform(transformer: PetTransformer): Subscription =
          copy(alternatives = alternatives.map { it.transform(transformer) })
    }

    private data class Regular(
        val match: Expression,
        val matchOnGain: Boolean,
        val typeVariables: TypeVariableScope,
    ) : Subscription() {
      override fun checkForHit(
          currentEvent: ChangeEvent,
          resolvedChange: ResolvedChange,
          isSelf: Boolean,
          reader: GameReader,
          elaborator: PetElaborator,
      ): Hit? {
        if (isSelf) return null
        val change = currentEvent.change
        val changeType = resolvedChange.type(matchOnGain) ?: return null
        // Will be refinement-aware (#48)
        val matchType = reader.resolve(match)
        return if (changeType.narrows(matchType, reader)) {
          val binder =
              try {
                elaborator.specializeVariables(
                    matchType,
                    changeType,
                    match,
                    typeVariables,
                )
              } catch (_: NarrowingException) {
                return null
              }
          Hit(listOf(binder), change.count)
        } else {
          null
        }
      }

      override val classToCheck = match.className

      override fun transform(transformer: PetTransformer): Subscription =
          copy(
              match = transformer.transformExpression(match),
              typeVariables = typeVariables.transformedBy(transformer),
          )
    }

    private data class Self(val context: Component, val matchOnGain: Boolean) : Subscription() {
      override fun checkForHit(
          currentEvent: ChangeEvent,
          resolvedChange: ResolvedChange,
          isSelf: Boolean,
          reader: GameReader,
          elaborator: PetElaborator,
      ): Hit? {
        if (!isSelf) return null
        val changeType = resolvedChange.type(matchOnGain) ?: return null

        return if (changeType == context.type) {
          Hit(emptyList(), currentEvent.change.count)
        } else {
          null
        }
      }

      override val classToCheck = null

      override fun transform(transformer: PetTransformer): Subscription = this
    }

    private data class Personal(
        val inner: Subscription,
        val selector: Expression,
        val typeVariables: TypeVariableScope,
        val actorVariable: TypeVariable?,
    ) : Subscription() {
      override fun checkForHit(
          currentEvent: ChangeEvent,
          resolvedChange: ResolvedChange,
          isSelf: Boolean,
          reader: GameReader,
          elaborator: PetElaborator,
      ): Hit? {
        val actor = currentEvent.actor
        val actorType = reader.classTable.resolve(actor.expression)

        // An explicit Actor declaration is bound before the inner trigger is matched. A use inside
        // a NOT refinement therefore receives the concrete Actor before that difference is tested.
        if (actorVariable != null) {
          val actorDomain = reader.resolve(ACTOR.expression)
          if (!reader.classTable.matchesConstraint(actorType, selector, actorDomain, reader)) {
            return null
          }
          val binding = typeVariables.bind(mapOf(actorVariable to actorType), reader.classTable)
          val hit =
              inner
                  .transform(binding)
                  .checkForHit(
                      currentEvent,
                      resolvedChange,
                      isSelf,
                      reader,
                      elaborator,
                  ) ?: return null
          return hit.before(binding)
        }

        val hit =
            inner.checkForHit(
                currentEvent,
                resolvedChange,
                isSelf,
                reader,
                elaborator,
            ) ?: return null

        // BY describes the Actor that performed the triggering change, recorded on the event.
        val specializedSelector = hit.specialize(selector)

        val actorDomain = reader.resolve(ACTOR.expression)
        if (
            !reader.classTable.matchesConstraint(
                actorType,
                specializedSelector,
                actorDomain,
                reader,
            )
        ) {
          return null
        }

        return hit
      }

      override val classToCheck = inner.classToCheck

      override fun transform(transformer: PetTransformer): Subscription =
          copy(
              inner = inner.transform(transformer),
              selector = transformer.transformExpression(selector),
              typeVariables = typeVariables.transformedBy(transformer),
          )
    }

    private data class Conditional(val inner: Subscription, val condition: Requirement) :
        Subscription() {
      override fun checkForHit(
          currentEvent: ChangeEvent,
          resolvedChange: ResolvedChange,
          isSelf: Boolean,
          reader: GameReader,
          elaborator: PetElaborator,
      ): Hit? {
        val wouldHit =
            inner.checkForHit(
                currentEvent,
                resolvedChange,
                isSelf,
                reader,
                elaborator,
            ) ?: return null
        return if (reader.has(wouldHit.specialize(condition))) wouldHit else null
      }

      override val classToCheck = inner.classToCheck

      override fun transform(transformer: PetTransformer): Subscription =
          copy(
              inner = inner.transform(transformer),
              condition = transformer.transformRequirement(condition),
          )
    }

    private data class CountBinding(val inner: Subscription) : Subscription() {
      override fun checkForHit(
          currentEvent: ChangeEvent,
          resolvedChange: ResolvedChange,
          isSelf: Boolean,
          reader: GameReader,
          elaborator: PetElaborator,
      ): Hit? {
        val hit =
            inner.checkForHit(
                currentEvent,
                resolvedChange,
                isSelf,
                reader,
                elaborator,
            ) ?: return null
        return hit.bindCount(currentEvent.change.count)
      }

      override val classToCheck = inner.classToCheck

      override fun transform(transformer: PetTransformer): Subscription =
          copy(inner = inner.transform(transformer))
    }
  }

  private data class Hit(
      private val transformers: List<PetTransformer>,
      private val count: Int,
  ) {
    fun specialize(instruction: InstructionTree): InstructionTree =
        transformers.fold(instruction) { current, transformer ->
          transformer.transformInstructionTree(current)
        } * count

    fun bindCount(value: Int): Hit =
        Hit(
            transformers + bindXTo(value),
            count = 1,
        )

    fun specialize(expression: Expression): Expression =
        transformers.fold(expression) { current, transformer ->
          transformer.transformExpression(current)
        }

    fun specialize(requirement: Requirement): Requirement =
        transformers.fold(requirement) { current, transformer ->
          transformer.transformRequirement(current)
        }

    fun then(transformer: PetTransformer) = copy(transformers = transformers + transformer)

    fun before(transformer: PetTransformer) =
        copy(transformers = listOf(transformer) + transformers)
  }
}
