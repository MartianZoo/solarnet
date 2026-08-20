package dev.martianzoo.engine

import dev.martianzoo.api.Exceptions.ExpressionException
import dev.martianzoo.api.GameReader
import dev.martianzoo.api.SystemClasses.ACTOR
import dev.martianzoo.api.SystemClasses.ANYONE
import dev.martianzoo.api.SystemClasses.OWNED
import dev.martianzoo.api.SystemClasses.OWNER
import dev.martianzoo.api.SystemClasses.PLAYER
import dev.martianzoo.api.SystemClasses.SYSTEM
import dev.martianzoo.data.Actor
import dev.martianzoo.data.GameEvent.ChangeEvent
import dev.martianzoo.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.data.Player
import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.PetTransformer.Companion.chain
import dev.martianzoo.pets.Transforming.replaceOwnerWith
import dev.martianzoo.pets.Transforming.replaceThisExpressionsWith
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

/** One specialized component effect ready for subscription matching and firing. */
internal class LiveEffect
private constructor(
    private val subscription: Subscription,
    internal val effect: Effect,
    private val context: Component,
    private val triggerClass: ClassName?,
    private val transformers: Transformers,
) {
  internal val automatic: Boolean
    get() = effect.automatic

  internal val registryKey = RegistryKey(automatic, triggerClass)

  internal fun onChangeToSelf(triggerEvent: ChangeEvent, reader: GameReader): PendingTask? =
      onChange(triggerEvent, reader, isSelf = true)

  internal fun onChangeToOther(triggerEvent: ChangeEvent, reader: GameReader): PendingTask? =
      onChange(triggerEvent, reader, isSelf = false)

  private fun onChange(
      triggerEvent: ChangeEvent,
      reader: GameReader,
      isSelf: Boolean,
  ): PendingTask? {
    // An owned effect belongs to the Player owning the component that carries it. This must win
    // over every other identity here: when Player1 places beside Player2's Philares tile, Player2
    // owns the effect and therefore chooses the standard resource. Using the changed tile's Owner
    // instead would incorrectly give that choice to Player1. This is intentionally Player-only:
    // no accepted SoloOpponent rule gives a passive Owner triggered choices or pending work.
    val effectOwner = context.playerOwner

    // An unowned effect can still be reacting to a Player-owned component. Retaining that Player
    // lets generic output such as `Plant<Owner>` bind to the component's Owner instead of the
    // gameplay scope that happens to execute the effect. A passive Owner is ignored here because
    // ownership alone must not give SoloOpponent task or gameplay authority.
    val changedComponentPlayer = changedComponentPlayer(triggerEvent, reader)

    // If neither the effect nor the changed component supplies ownership, a Player Actor is the
    // last legitimate source for contextual `Owner`. Engine is deliberately excluded: it is an
    // Actor but not an Owner, so treating it as one would manufacture invalid owned components.
    val contextualOwner = effectOwner ?: changedComponentPlayer ?: (triggerEvent.actor as? Player)

    // Assignment is a separate compatibility rule. Keeping this call separate from
    // contextualOwner prevents the assignee from silently becoming the Actor used by authored
    // `BY`, while preserving today's Philares behavior in which its Owner receives the task.
    val assignee = assigneeForTriggeredWork(triggerEvent, effectOwner, changedComponentPlayer)
    val hit = subscription.checkForHit(triggerEvent, contextualOwner, isSelf, reader) ?: return null
    val cause = Cause(context.expression, triggerEvent.ordinal)
    val instruction =
        transformers
            .evaluateProperties(context.expression, contextualOwner)
            .transformInstructionTree(hit.specialize(effect.instruction))
    return PendingTask(
        assignee = assignee,
        actor = effectOwner ?: triggerEvent.actor,
        instruction = InstructionGroup.of(instruction),
        cause = cause,
    )
  }

  /**
   * The compatibility rule for choosing the assignee of work produced by an effect. Authored `BY`
   * independently tests the Actor recorded on the triggering event.
   *
   * Automatic effects are represented temporarily as PendingTasks but execute inline using the
   * PendingTask's stored Actor, so an effect owner remains the Actor when one exists.
   */
  private fun assigneeForTriggeredWork(
      triggerEvent: ChangeEvent,
      effectOwner: Player?,
      changedComponentPlayer: Player?,
  ): Actor = effectOwner ?: changedComponentPlayer ?: triggerEvent.actor

  private fun changedComponentPlayer(triggerEvent: ChangeEvent, reader: GameReader): Player? {
    val expression = triggerEvent.change.gaining ?: triggerEvent.change.removing ?: return null
    return reader.resolve(expression).toComponent().playerOwner
  }

  override fun equals(other: Any?): Boolean =
      other is LiveEffect &&
          subscription == other.subscription &&
          automatic == other.automatic &&
          effect.instruction == other.effect.instruction &&
          context == other.context &&
          triggerClass == other.triggerClass

  override fun hashCode(): Int {
    var result = subscription.hashCode()
    result = 31 * result + automatic.hashCode()
    result = 31 * result + effect.instruction.hashCode()
    result = 31 * result + context.hashCode()
    result = 31 * result + (triggerClass?.hashCode() ?: 0)
    return result
  }

  internal data class RegistryKey(
      val automatic: Boolean,
      val triggerClass: ClassName?,
  )

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
      val linkedSources = effect.linkedTypeSources
      val subscription = Subscription.from(effect.trigger, context, linkedSources)
      val triggerClass =
          subscription.classToCheck?.let(transformers.classTable::getClass)?.className
      return LiveEffect(subscription, effect, context, triggerClass, transformers)
    }

    private fun specialize(component: Component, transformers: Transformers): List<Effect> {
      val ownerBinding = component.owner?.let(::replaceOwnerWith)
      val thisBinding = replaceThisExpressionsWith(component.expression)

      return if (component.owner == null || component.playerOwner != null) {
        transformers.classEffects(component.type.rootClass).map { effect ->
          // Anyone repeated across a trigger and its result is local to that event, not the
          // component's contextual Owner. Leave it for subscription matching to specialize.
          val triggerBindings =
              effect.linkedTypeSources.filterTo(mutableSetOf()) { it.className == ANYONE }
          val checkedBinding =
              transformers.checkedSubstituterPreserving(
                  component.type.rootClass.defaultType,
                  component.type,
                  triggerBindings,
                  ownerBinding,
                  thisBinding,
                  transformers.insertDeferredComplementDefaults(component.expression),
              )
          val bound = checkedBinding.transformEffect(effect)
          try {
            component.type.classTable.checkAllTypes(bound)
            bound
          } catch (e: ExpressionException) {
            throw ExpressionException(
                "invalid component effect for ${component.type.expressionFull}: $bound",
                e,
            )
          }
        }
      } else {
        val uncheckedBinding =
            chain(
                transformers.substituter(component.type.rootClass.defaultType, component.type),
                ownerBinding,
                thisBinding,
                transformers.insertDeferredComplementDefaults(component.expression),
            )
        transformers.classEffects(component.type.rootClass).mapNotNull { effect ->
          val bound = uncheckedBinding.transformEffect(effect)
          try {
            component.type.classTable.checkAllTypes(bound)
            bound
          } catch (e: ExpressionException) {
            // An Owner-only component can inherit an effect whose output is Player-bound. The
            // source effect is valid, but it does not apply to that Owner; for example, the
            // starting tiles owned by SoloOpponent do not score VictoryPoint<Player> components.
            val sourceEffect =
                replaceThisExpressionsWith(component.type.rootClass.className.expression)
                    .transformEffect(effect)
            component.type.classTable.checkAllTypes(sourceEffect)
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
          linkedSources: Set<Expression>,
          implicitOwner: Player? = context.playerOwner,
      ): Subscription {
        return when (trigger) {
          is Or -> AnyOf(trigger.triggers.map { from(it, context, linkedSources, implicitOwner) })
          is BasicTrigger -> {
            when (trigger) {
              is WhenGain -> Self(context, matchOnGain = true)
              is WhenRemove -> Self(context, matchOnGain = false)
              is OnGainOf ->
                  Regular(
                      trigger.expression,
                      matchOnGain = true,
                      implicitOwner = implicitOwner,
                      linkedSources = linkedSources,
                  )
              is OnRemoveOf ->
                  Regular(
                      trigger.expression,
                      matchOnGain = false,
                      implicitOwner = implicitOwner,
                      linkedSources = linkedSources,
                  )
            }
          }
          is WrappingTrigger -> {
            val inner =
                from(
                    trigger.inner,
                    context,
                    linkedSources,
                    implicitOwner = if (trigger is ByTrigger) null else implicitOwner,
                )
            when (trigger) {
              is ByTrigger -> Personal(inner, trigger.by)
              is IfTrigger -> Conditional(inner, trigger.condition)
              is XTrigger -> Unscaled(inner)
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
        isSelf: Boolean,
        reader: GameReader,
    ): Hit?

    abstract val classToCheck: ClassName?

    abstract fun transform(transformer: PetTransformer): Subscription

    private data class AnyOf(val alternatives: List<Subscription>) : Subscription() {
      override fun checkForHit(
          currentEvent: ChangeEvent,
          contextualOwner: Player?,
          isSelf: Boolean,
          reader: GameReader,
      ): Hit? {
        alternatives.forEach { alternative ->
          alternative.checkForHit(currentEvent, contextualOwner, isSelf, reader)?.let {
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
        val linkedSources: Set<Expression>,
    ) : Subscription() {
      override fun checkForHit(
          currentEvent: ChangeEvent,
          contextualOwner: Player?,
          isSelf: Boolean,
          reader: GameReader,
      ): Hit? {
        reader as GameReaderImpl
        if (isSelf) return null
        val change = currentEvent.change
        val expr = (if (matchOnGain) change.gaining else change.removing) ?: return null
        // Will be refinement-aware (#48)
        val changeType = reader.resolve(expr)
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
          // Resolving a Player-bounded expression such as UseAction1<Owner, Foo> correctly
          // intersects its type to UseAction1<Player, Foo>. Keep the original Owner token's other
          // role as a contextual variable without treating that Owner as the executing Actor.
          val ownerSubstitution =
              if (OWNER in match) contextualOwner?.let(::replaceOwnerWith) else null
          val substituter =
              reader.transformers.checkedLinkageSubstituter(
                  matchType,
                  changeType,
                  linkedSources,
                  ownerSubstitution,
              )
          Hit(listOf(substituter), change.count)
        } else {
          null
        }
      }

      override val classToCheck = match.className

      override fun transform(transformer: PetTransformer): Subscription =
          copy(
              match = transformer.transformExpression(match),
              linkedSources = linkedSources.mapTo(mutableSetOf(), transformer::transformExpression),
          )
    }

    private data class Self(val context: Component, val matchOnGain: Boolean) : Subscription() {
      override fun checkForHit(
          currentEvent: ChangeEvent,
          contextualOwner: Player?,
          isSelf: Boolean,
          reader: GameReader,
      ): Hit? {
        if (!isSelf) return null
        val change = currentEvent.change
        val expr = (if (matchOnGain) change.gaining else change.removing) ?: return null

        return if (expr == context.expressionFull) {
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
    ) : Subscription() {
      override fun checkForHit(
          currentEvent: ChangeEvent,
          contextualOwner: Player?,
          isSelf: Boolean,
          reader: GameReader,
      ): Hit? {
        reader as GameReaderImpl
        val actor = currentEvent.actor
        val actorType = reader.resolve(actor.expression)

        // A positive abstract Actor selector is also a trigger-local type variable. Bind it before
        // matching the inner trigger so `Resource<!Player> BY Player` means a resource belonging
        // to someone other than the concrete Player who performed this event, and so the same
        // Player can be retained in the triggered instruction.
        if (!selector.complement && selector.simple && selector.className != ANYONE) {
          val selectorType = reader.resolve(selector)
          val actorClass = selectorType.classTable.getClass(ACTOR)
          if (selectorType.rootClass.abstract && selectorType.rootClass.isSubtypeOf(actorClass)) {
            val actorDomain = reader.resolve(ACTOR.expression)
            if (!reader.matchesConstraint(actorType, selector, actorDomain)) return null
            val binding = reader.transformers.checkedSubstituter(selectorType, actorType)
            val hit =
                inner.transform(binding).checkForHit(currentEvent, contextualOwner, isSelf, reader)
                    ?: return null
            return hit.before(binding)
          }
        }

        var hit = inner.checkForHit(currentEvent, contextualOwner, isSelf, reader) ?: return null

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
          val actorClass = excludedType.classTable.getClass(ACTOR)
          val abstractActorSupertypes =
              excludedType.rootClass.allSuperclasses().filter {
                it.abstract && it.isSubtypeOf(actorClass)
              }
          val selectorDomain =
              abstractActorSupertypes.singleOrNull { candidate ->
                abstractActorSupertypes.none {
                  it != candidate && it.isSubtypeOf(candidate)
                }
              }
                  ?: run {
                    // A passive Owner such as SoloOpponent is not an Actor. Its opposing Actors
                    // are Players, not the administrative Engine.
                    val ownerClass = excludedType.classTable.getClass(OWNER)
                    if (!excludedType.rootClass.isSubtypeOf(ownerClass)) return null
                    excludedType.classTable.getClass(PLAYER)
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
          )
    }

    private data class Conditional(val inner: Subscription, val condition: Requirement) :
        Subscription() {
      override fun checkForHit(
          currentEvent: ChangeEvent,
          contextualOwner: Player?,
          isSelf: Boolean,
          reader: GameReader,
      ): Hit? {
        val wouldHit =
            inner.checkForHit(currentEvent, contextualOwner, isSelf, reader) ?: return null
        return if (reader.has(wouldHit.specialize(condition))) wouldHit else null
      }

      override val classToCheck = inner.classToCheck

      override fun transform(transformer: PetTransformer): Subscription =
          copy(
              inner = inner.transform(transformer),
              condition = transformer.transformRequirement(condition),
          )
    }

    private data class Unscaled(val inner: Subscription) : Subscription() {
      override fun checkForHit(
          currentEvent: ChangeEvent,
          contextualOwner: Player?,
          isSelf: Boolean,
          reader: GameReader,
      ): Hit? {
        // Just fake it like only one happened.
        return inner.checkForHit(
            currentEvent.copy(change = currentEvent.change.copy(count = 1)),
            contextualOwner,
            isSelf,
            reader,
        )
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
