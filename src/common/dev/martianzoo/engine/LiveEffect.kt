package dev.martianzoo.engine

import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.PetTransformer.Companion.chain
import dev.martianzoo.pets.Transforming.bindXTo
import dev.martianzoo.pets.Transforming.replaceOwnerWith
import dev.martianzoo.pets.Transforming.replaceThisExpressionsWith
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.api.SystemClasses.ACTOR
import dev.martianzoo.pets.api.SystemClasses.ANYONE
import dev.martianzoo.pets.api.SystemClasses.OWNED
import dev.martianzoo.pets.api.SystemClasses.OWNER
import dev.martianzoo.pets.api.SystemClasses.PLAYER
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
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.pets.data.GameEvent.ChangeEvent
import dev.martianzoo.pets.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.pets.data.Player
import dev.martianzoo.pets.types.Type
import dev.martianzoo.pets.types.TypeVariable
import dev.martianzoo.pets.types.TypeVariableScope

/** One specialized component effect ready for subscription matching and firing. */
internal class LiveEffect
private constructor(
    private val subscription: Subscription,
    internal val effect: Effect,
    private val context: Component,
    private val triggerClass: ClassName?,
    private val transformers: Transformers,
) {
  // The context is immutable, so its ownership cannot change during this effect's lifetime.
  private val effectOwner: Player? = context.playerOwner

  internal val automatic: Boolean
    get() = effect.automatic

  internal val registryKey = RegistryKey(automatic, triggerClass)

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
    // An owned effect belongs to the Player owning the component that carries it. That Player is
    // the default performer and eventual narrower, while the triggering operation's controller
    // retains the task until selection. This is intentionally Player-only: no accepted
    // SoloOpponent rule gives a passive Owner triggered choices or pending work.

    // An unowned effect can still be reacting to a Player-owned component. Retaining that Player
    // lets generic output such as `Plant<Owner>` bind to the component's Owner instead of the
    // Agent scope that happens to execute the effect. A passive Owner is ignored here because
    // ownership alone must not give SoloOpponent task or Agent authority.
    val changedComponentPlayer = resolvedChange.changedComponentPlayer

    // If neither the effect nor the changed component supplies ownership, a Player Actor is the
    // last legitimate source for contextual `Owner`. Engine is deliberately excluded: it is an
    // Actor but not an Owner, so treating it as one would manufacture invalid owned components.
    val contextualOwner = effectOwner ?: changedComponentPlayer ?: (triggerEvent.actor as? Player)
    val defaultActor =
        effectOwner ?: changedComponentPlayer.takeUnless { automatic } ?: triggerEvent.actor
    val taskController =
        (controller as? Player) ?: effectOwner ?: changedComponentPlayer ?: triggerEvent.actor

    val hit =
        subscription.checkForHit(
            triggerEvent,
            contextualOwner,
            resolvedChange,
            isSelf,
            reader,
        ) ?: return null
    val cause = Cause(context.expression, triggerEvent.ordinal)
    val instruction =
        transformers
            .evaluateProperties(context.expression, contextualOwner)
            .transformInstructionTree(hit.specialize(effect.instruction))
    return PendingTask(
        controller = taskController,
        actor = defaultActor,
        instruction = InstructionGroup.of(instruction),
        cause = cause,
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
    val changedComponentPlayer: Player? = (gaining ?: removing)?.toComponent()?.playerOwner

    fun type(matchOnGain: Boolean): Type? = if (matchOnGain) gaining else removing
  }

  internal companion object {
    internal fun compile(
        component: Component,
        transformers: Transformers,
    ): List<LiveEffect> =
        specialize(component, transformers).map { create(it, component, transformers) }

    private fun create(
        effect: Effect,
        context: Component,
        transformers: Transformers,
    ): LiveEffect {
      // Lowering can consume the trigger-side occurrence (for example PROD), so prefer the frozen
      // authored origins even when they can no longer be rediscovered from the transformed tree.
      val typeVariables = effect.typeVariables
      val subscription = Subscription.from(effect.trigger, context, typeVariables)
      val triggerClass =
          subscription.classToCheck?.let(transformers.classTable::getClass)?.className
      return LiveEffect(subscription, effect, context, triggerClass, transformers)
    }

    private fun specialize(component: Component, transformers: Transformers): List<Effect> {
      val ownerBinding = component.owner?.let(::replaceOwnerWith)
      val thisBinding = replaceThisExpressionsWith(component.expression)

      return if (component.owner == null || component.playerOwner != null) {
        transformers.classEffects(component.type.rootClass).map { effect ->
          val checkedBinding =
              transformers.bindEffectVariables(
                  component.type.rootClass.defaultType,
                  component.type,
                  effect,
                  ownerBinding,
                  thisBinding,
                  transformers.insertDeferredComplementDefaults(component.expression),
              )
          val bound = checkedBinding.transformEffect(effect)
          try {
            transformers.classTable.checkAllTypes(bound)
            bound
          } catch (e: ExpressionException) {
            throw ExpressionException(
                "invalid component effect for ${component.type.expressionFull}: $bound",
                e,
            )
          }
        }
      } else {
        transformers.classEffects(component.type.rootClass).mapNotNull { effect ->
          val contextualizer =
              chain(
                  ownerBinding,
                  thisBinding,
                  transformers.insertDeferredComplementDefaults(component.expression),
              )
          val contextualScope = effect.typeVariables.transformedBy(contextualizer)
          val variableBinding =
              contextualScope.bind(
                  component.type.variableBindingsFrom(
                      component.type.rootClass.defaultType,
                      effect.typeVariables.variables,
                  )
              )
          val uncheckedBinding =
              chain(
                  contextualizer,
                  variableBinding,
              )
          val bound = uncheckedBinding.transformEffect(effect)
          try {
            transformers.classTable.checkAllTypes(bound)
            bound
          } catch (e: ExpressionException) {
            // An Owner-only component can inherit an effect whose output is Player-bound. The
            // source effect is valid, but it does not apply to that Owner; for example, the
            // starting tiles owned by SoloOpponent do not score VictoryPoint<Player> components.
            val sourceEffect =
                replaceThisExpressionsWith(component.type.rootClass.className.expression)
                    .transformEffect(effect)
            transformers.classTable.checkAllTypes(sourceEffect)
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
          implicitOwner: Player? = context.playerOwner,
      ): Subscription {
        return when (trigger) {
          is Or -> AnyOf(trigger.triggers.map { from(it, context, typeVariables, implicitOwner) })
          is BasicTrigger -> {
            when (trigger) {
              is WhenGain -> Self(context, matchOnGain = true)
              is WhenRemove -> Self(context, matchOnGain = false)
              is OnGainOf ->
                  Regular(
                      trigger.expression,
                      matchOnGain = true,
                      implicitOwner = implicitOwner,
                      typeVariables = typeVariables,
                  )
              is OnRemoveOf ->
                  Regular(
                      trigger.expression,
                      matchOnGain = false,
                      implicitOwner = implicitOwner,
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
                    implicitOwner = if (trigger is ByTrigger) null else implicitOwner,
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

    /**
     * [contextualOwner] is the Player used to bind contextual `Owner` placeholders in a triggered
     * instruction. It is not generalized to every Pets Owner: this path produces executable or
     * choice-bearing work, and no passive-Owner rule for that work has been defined.
     */
    abstract fun checkForHit(
        currentEvent: ChangeEvent,
        contextualOwner: Player?,
        resolvedChange: ResolvedChange,
        isSelf: Boolean,
        reader: GameReader,
    ): Hit?

    abstract val classToCheck: ClassName?

    abstract fun transform(transformer: PetTransformer): Subscription

    private data class AnyOf(val alternatives: List<Subscription>) : Subscription() {
      override fun checkForHit(
          currentEvent: ChangeEvent,
          contextualOwner: Player?,
          resolvedChange: ResolvedChange,
          isSelf: Boolean,
          reader: GameReader,
      ): Hit? {
        alternatives.forEach { alternative ->
          alternative
              .checkForHit(
                  currentEvent,
                  contextualOwner,
                  resolvedChange,
                  isSelf,
                  reader,
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
        val implicitOwner: Player?,
        val typeVariables: TypeVariableScope,
    ) : Subscription() {
      override fun checkForHit(
          currentEvent: ChangeEvent,
          contextualOwner: Player?,
          resolvedChange: ResolvedChange,
          isSelf: Boolean,
          reader: GameReader,
      ): Hit? {
        reader as GameReaderImpl
        if (isSelf) return null
        val change = currentEvent.change
        val changeType = resolvedChange.type(matchOnGain) ?: return null
        // Will be refinement-aware (#48)
        val matchType = reader.resolve(match)
        val triggerIsOwnedOrSystem =
            matchType.rootClass.allSuperclasses().any {
              it.className == OWNED || it.className == SYSTEM
            }
        if (
            !triggerIsOwnedOrSystem && implicitOwner != null && currentEvent.actor != implicitOwner
        ) {
          return null
        }
        return if (changeType.narrows(matchType, reader)) {
          // TODO: Replace this compatibility binding with an explicit Pets representation for
          // contextual Owner.
          // Resolving a Player-bounded expression such as UseAction<Owner, Foo, Action1> correctly
          // intersects its type to UseAction<Player, Foo, Action1>. Keep the original Owner token's
          // other
          // role as a contextual variable without treating that Owner as the executing Actor.
          val ownerSubstitution =
              if (OWNER in match) contextualOwner?.let(::replaceOwnerWith) else null
          val binder =
              reader.transformers.bindVariablesFrom(
                  matchType,
                  changeType,
                  match,
                  typeVariables,
                  ownerSubstitution,
              )
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
          contextualOwner: Player?,
          resolvedChange: ResolvedChange,
          isSelf: Boolean,
          reader: GameReader,
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
          contextualOwner: Player?,
          resolvedChange: ResolvedChange,
          isSelf: Boolean,
          reader: GameReader,
      ): Hit? {
        reader as GameReaderImpl
        val actor = currentEvent.actor
        val actorType = reader.resolve(actor.expression)

        // An explicit Actor declaration is bound before the inner trigger is matched. Derived
        // uses such as `!Player` retain their operator while receiving the concrete Actor value.
        if (actorVariable != null) {
          val actorDomain = reader.resolve(ACTOR.expression)
          if (!reader.matchesConstraint(actorType, selector, actorDomain)) return null
          val binding = typeVariables.bind(mapOf(actorVariable to actorType))
          val hit =
              inner
                  .transform(binding)
                  .checkForHit(
                      currentEvent,
                      contextualOwner,
                      resolvedChange,
                      isSelf,
                      reader,
                  ) ?: return null
          return hit.before(binding)
        }

        var hit =
            inner.checkForHit(
                currentEvent,
                contextualOwner,
                resolvedChange,
                isSelf,
                reader,
            ) ?: return null

        // BY describes the Actor that performed the triggering change, recorded on the event.
        fun specializeSelector(): Expression {
          if (!selector.complement) return hit.specialize(selector)

          var excluded = hit.specialize(selector.copy(complement = false))
          if (contextualOwner != null) {
            excluded = replaceOwnerWith(contextualOwner).transformExpression(excluded)
          }
          return excluded.copy(complement = true)
        }

        var specializedSelector = specializeSelector()

        // On an unowned effect, an otherwise-unbound positive Owner means the performing Player.
        // Apply that established contextual rule before evaluating the selector as an Actor type.
        if (specializedSelector == OWNER.expression) {
          val owner = actor as? Player ?: return null
          hit = hit.then(replaceOwnerWith(owner))
          specializedSelector = specializeSelector()
        }
        val by = specializedSelector.className

        // Anyone is the icon-grammar spelling for an unrestricted trigger; unlike the other
        // selectors, its class hierarchy is about ownership rather than the Actor domain.
        if (by == ANYONE && !specializedSelector.complement) return hit

        if (specializedSelector.complement) {
          val excludedType = reader.resolve(specializedSelector.copy(complement = false))
          val actorClass = reader.resolve(ACTOR.expression).rootClass
          val abstractActorSupertypes =
              excludedType.rootClass.allSuperclasses().filter {
                it.abstract && it.isSubtypeOf(actorClass)
              }
          val selectorDomain =
              abstractActorSupertypes.singleOrNull { candidate ->
                abstractActorSupertypes.none { it != candidate && it.isSubtypeOf(candidate) }
              }
                  ?: run {
                    // A passive Owner such as SoloOpponent is not an Actor. Its opposing Actors
                    // are Players, not the administrative Engine.
                    val ownerClass = reader.resolve(OWNER.expression).rootClass
                    if (!excludedType.rootClass.isSubtypeOf(ownerClass)) return null
                    reader.resolve(PLAYER.expression).rootClass
                  }
          if (!actorType.narrows(selectorDomain.defaultType, reader)) return null
          if (actorType.narrows(excludedType, reader)) return null
          return hit
        }

        val actorDomain = reader.resolve(ACTOR.expression)
        if (!reader.matchesConstraint(actorType, specializedSelector, actorDomain)) return null

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
          contextualOwner: Player?,
          resolvedChange: ResolvedChange,
          isSelf: Boolean,
          reader: GameReader,
      ): Hit? {
        val wouldHit =
            inner.checkForHit(
                currentEvent,
                contextualOwner,
                resolvedChange,
                isSelf,
                reader,
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
          contextualOwner: Player?,
          resolvedChange: ResolvedChange,
          isSelf: Boolean,
          reader: GameReader,
      ): Hit? {
        val hit =
            inner.checkForHit(
                currentEvent,
                contextualOwner,
                resolvedChange,
                isSelf,
                reader,
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
