package dev.martianzoo.tfm.engine

import dev.martianzoo.engine.World
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Instruction.Change
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Per
import dev.martianzoo.pets.ast.Instruction.Quantifier.AMAP
import dev.martianzoo.pets.ast.Instruction.Quantifier.MANDATORY
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.pets.types.Type
import dev.martianzoo.state.Component
import dev.martianzoo.state.GameReader
import dev.martianzoo.state.Player
import dev.martianzoo.state.Task

/** Canon-specific predicates for Player tasks that can be claimed before competing work. */
public object TfmAutoExec {
  private val mc = cn("MC")
  private val stockResources =
      setOf(mc, cn("Steel"), cn("Titanium"), cn("Plant"), cn("Energy"), cn("Heat"))
  private val terraformRating = cn("TerraformRating")
  private val production = cn("Production")
  private val simpleGlobalSteps = setOf(cn("OxygenStep"), cn("TemperatureStep"), cn("VenusStep"))
  private val cardFront = cn("CardFront")
  private val cardBack = cn("CardBack")
  private val actionScope = cn("ActionScope")
  private val nestedPlaySignals =
      setOf(
          cn("PlayCard"),
          cn("PlayPrelude"),
          cn("PlayProject"),
          cn("CopyPrelude"),
          cn("CopyProductionBox"),
          cn("UseAction"),
          cn("UseCardAction"),
          cn("UseStandardProject"),
          cn("UseTurmoilPolicyAction"),
          cn("DoRequiredActions"),
          cn("StandardAction"),
      )

  /**
   * Whether a register row accepts claiming [task] before the other pending work in [world]. The
   * caller supplies a current task while no task is selected; this query does not claim it. False
   * means that no enabled Canon row matches, not that the task is illegal.
   */
  public fun safeTask(world: World, task: Task): Boolean {
    val player = task.assignee as? Player ?: return false
    val instruction = task.instruction
    val change =
        when (instruction) {
          is Change -> instruction
          is Per -> instruction.inner as Change
          else -> return false
        }
    val count = change.count as? ActualScalar ?: return false
    if (count.value <= 0 || task.then != null || task.deferred) return false
    val reader = world.reader
    if (change.isAbstract(reader)) return false

    val target = reader.resolve(change.gaining ?: change.removing!!)
    if (target.refinement != null) return false
    if (isSameTrackStep(world, task, change, target, player)) return true
    if (change.quantifier != MANDATORY) return false
    if (Component(target).owningPlayer != player) {
      return false
    }
    if (change is Gain && isCardDraw(reader, target)) return true
    if (instruction !is Change) return false
    // Outside an action, settle only simple fixed changes to public resources. Card moves,
    // optional changes, and structural tasks can still contain a Player choice.
    if (reader.getComponents(cn("TurnBasedPhase").expression).elements.isEmpty()) {
      return (change is Gain || change is Remove) &&
          target.className in stockResources + terraformRating + production
    }
    return when (change) {
      is Gain ->
          when (target.className) {
            in stockResources ->
                !isInPlay(reader, cn("SolarPhase")) &&
                    !hasNestedPlayRoute(world) &&
                    !hasOrderSensitiveLossOfType(world, task, target) &&
                    (target.className != mc || !mcOrderCanMatter(world))
            terraformRating ->
                hasActionScope(reader, player) &&
                    !hasNestedPlayRoute(world) &&
                    !isInPlay(reader, cn("TerraformingDeal"))
            production ->
                productionResource(target) in stockResources &&
                    hasActionScope(reader, player) &&
                    !isInPlay(reader, cn("SoloMode")) &&
                    !isInPlay(reader, cn("SolarPhase")) &&
                    !hasNestedPlayRoute(world) &&
                    onlyIndependentChangesCompete(
                        world,
                        task,
                        production,
                        allowProductionGains = true,
                    )
            else -> false
          }
      is Remove ->
          hasActionScope(reader, player) &&
              !hasNestedPlayRoute(world) &&
              ((target.className == terraformRating &&
                  onlyIndependentChangesCompete(world, task, terraformRating)) ||
                  (target.className in stockResources &&
                      !isInPlay(reader, cn("SoloMode")) &&
                      !isInPlay(reader, cn("SolarPhase")) &&
                      (target.className != mc || !mcOrderCanMatter(world)) &&
                      !(target.className == mc &&
                          isInPlay(reader, cn("RedsPolicy")) &&
                          hasPendingGainOfType(world, terraformRating)) &&
                      onlyIndependentChangesCompete(world, task, target.className)))
      else -> false
    }
  }

  /** The three identical Venus L1 Shade steps may cross the track cap in any order. */
  private fun isSameTrackStep(
      world: World,
      task: Task,
      change: Change,
      target: Type,
      player: Player,
  ): Boolean {
    if (change !is Gain || change.quantifier != AMAP || target.className !in simpleGlobalSteps) {
      return false
    }
    if ((change.count as? ActualScalar)?.value != 1 || !hasActionScope(world.reader, player)) {
      return false
    }
    if (hasNestedPlayRoute(world)) return false
    return world.tasks
        .extract { it }
        .filter { it.id != task.id && !it.deferred }
        .all { other ->
          if (other.assignee != player || other.then != null || other.cause != task.cause) {
            return@all false
          }
          val step = other.instruction as? Gain ?: return@all false
          step.quantifier == AMAP &&
              (step.count as? ActualScalar)?.value == 1 &&
              world.reader.resolve(step.gaining) == target
        }
  }

  /** Card draws are taken early while the Playground permits undo back through their deal. */
  private fun isCardDraw(reader: GameReader, target: Type): Boolean {
    val back = reader.classTable.findClass(cardBack) ?: return false
    return target.rootClass.isSubtypeOf(back)
  }

  private fun onlyIndependentChangesCompete(
      world: World,
      task: Task,
      targetType: ClassName,
      allowProductionGains: Boolean = false,
  ): Boolean =
      world.tasks
          .extract { it }
          .filter { it.id != task.id && !it.deferred }
          .all { other ->
            if (other.assignee != task.assignee) return@all false
            val change = other.instruction as? Change ?: return@all false
            val amount = change.count as? ActualScalar ?: return@all false
            if (amount.value <= 0 || change.quantifier != MANDATORY || other.then != null) {
              return@all false
            }
            if (change.isAbstract(world.reader)) return@all false
            val gainedType = change.gaining?.let(world.reader::resolve)
            val gaining = gainedType?.className
            val removedType = change.removing?.let(world.reader::resolve)
            val removing = removedType?.className
            if (
                listOfNotNull(gainedType, removedType).any {
                  it.refinement != null || Component(it).owningPlayer != task.assignee
                }
            ) {
              return@all false
            }
            if (
                allowProductionGains &&
                    change is Gain &&
                    gainedType != null &&
                    gaining == production
            ) {
              return@all productionResource(gainedType) in stockResources
            }
            gaining != targetType &&
                removing != targetType &&
                listOfNotNull(gaining, removing).all {
                  it in stockResources || it == terraformRating
                }
          }

  private fun productionResource(type: Type): ClassName? =
      type.typeDependencies
          .singleOrNull { it.key.declaringClass == production }
          ?.boundType
          ?.representedClass
          ?.className

  private fun mcOrderCanMatter(world: World): Boolean {
    val reader = world.reader
    if (isInPlay(reader, cn("PharmacyUnion")) || isInPlay(reader, cn("MonsInsurance"))) {
      return true
    }
    // A disease/loss sequence can remain queued after Pharmacy Union flips. Inspect that work
    // below, so the predicate returns as soon as the outstanding loss is resolved.
    return world.tasks
        .extract { it }
        .any { pending ->
          listOfNotNull(pending.instruction, pending.then).any { instruction ->
            instruction.descendantsOfType<Change>().any { change ->
              change.removing?.className == mc && change.quantifier == AMAP
            }
          }
        }
  }

  private fun hasOrderSensitiveLossOfType(world: World, task: Task, target: Type): Boolean =
      world.tasks
          .extract { it }
          .filterNot { it.deferred }
          .any { pending ->
            listOfNotNull(pending.instruction, pending.then).any { instruction ->
              instruction.descendantsOfType<Change>().any loss@{ change ->
                val removing = change.removing ?: return@loss false
                val removedType = world.reader.resolve(removing)
                if (!target.narrows(removedType, world.reader)) return@loss false
                val fixedOwnLoss =
                    pending.instruction === change &&
                        pending.then == null &&
                        pending.assignee == task.assignee &&
                        change is Remove &&
                        change.quantifier == MANDATORY &&
                        (change.count as? ActualScalar)?.value?.let { it > 0 } == true &&
                        removedType == target
                !fixedOwnLoss
              }
            }
          }

  private fun hasPendingGainOfType(world: World, type: ClassName): Boolean =
      world.tasks
          .extract { it }
          .filterNot { it.deferred }
          .any { pending ->
            listOfNotNull(pending.instruction, pending.then).any { instruction ->
              instruction.descendantsOfType<Gain>().any { it.gaining.className == type }
            }
          }

  private fun hasNestedPlayRoute(world: World): Boolean {
    val reader = world.reader
    val frontClass = reader.classTable.findClass(cardFront)
    return world.tasks
        .extract { it }
        .any { pending ->
          listOfNotNull(pending.instruction, pending.then).any { instruction ->
            instruction.descendantsOfType<ClassName>().any { it in nestedPlaySignals } ||
                // Once PlayCard is claimed, its payment chain can still end in a card placement.
                (frontClass != null &&
                    instruction.descendantsOfType<Change>().any { change ->
                      change.gaining?.className?.let { name ->
                        reader.classTable.findClass(name)?.isSubtypeOf(frontClass)
                      } == true
                    })
          }
        }
  }

  private fun isInPlay(reader: GameReader, name: ClassName): Boolean =
      reader.classTable.findClass(name)?.let { reader.count(it.defaultType) > 0 } == true

  private fun hasActionScope(reader: GameReader, player: Player): Boolean =
      reader.classTable.findClass(actionScope)?.let { scopeClass ->
        reader.getComponents(scopeClass.defaultType).elements.any { type ->
          type.typeDependencies
              .singleOrNull { it.key.declaringClass == actionScope }
              ?.boundType
              ?.className == player.className
        }
      } == true
}
