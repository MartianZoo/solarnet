package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.GameReader
import dev.martianzoo.tfm.api.UserException
import dev.martianzoo.tfm.api.UserException.LimitsException
import dev.martianzoo.tfm.api.UserException.NotNowException
import dev.martianzoo.tfm.engine.Game.ComponentGraph
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Change
import dev.martianzoo.tfm.pets.ast.Instruction.Change.Companion.change
import dev.martianzoo.tfm.pets.ast.Instruction.Custom
import dev.martianzoo.tfm.pets.ast.Instruction.Gated
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.AMAP
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.OPTIONAL
import dev.martianzoo.tfm.pets.ast.Instruction.Multi
import dev.martianzoo.tfm.pets.ast.Instruction.NoOp
import dev.martianzoo.tfm.pets.ast.Instruction.Or
import dev.martianzoo.tfm.pets.ast.Instruction.Per
import dev.martianzoo.tfm.pets.ast.Instruction.Then
import dev.martianzoo.tfm.pets.ast.Instruction.Transform
import dev.martianzoo.tfm.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.tfm.types.MType
import kotlin.math.min

internal class Preparer(
    private val agentReader: GameReader,
    private val components: ComponentGraph
) {
  /**
   * A function that transforms [uncommitted] into a more specific version of itself that *must* be
   * executed as the very next state change. The returned instruction might still be abstract and
   * need further refining. Might also return [NoOp], which can be discarded. This should never be
   * called if there is already a committed change. This function changes no game state by itself.
   */
  fun toPreparedForm(uncommitted: Instruction): Instruction {
    return when (uncommitted) {
      is NoOp -> NoOp
      is Change -> prepareChangeInstruction(uncommitted)
      is Per -> toPreparedForm(uncommitted.instruction * agentReader.count(uncommitted.metric))
      is Gated -> {
        if (agentReader.evaluate(uncommitted.gate)) {
          toPreparedForm(uncommitted.instruction)
        } else if (uncommitted.mandatory) {
          throw UserException.requirementNotMet(uncommitted.gate)
        } else {
          NoOp
        }
      }
      is Custom -> {
        // make sure it exists
        agentReader.authority.customInstruction(uncommitted.functionName)
        uncommitted // TODO
      }
      is Or -> {
        // To commit it we try to commit each arm and then form an OR out of what survives that
        val options =
            uncommitted.instructions.mapNotNull {
              try {
                toPreparedForm(it)
              } catch (e: Exception) {
                null
              }
            }
        if (options.none()) {
          throw NotNowException("all OR options are impossible at this time")
        }
        Or.create(options)
      }
      is Then -> Then.create(uncommitted.instructions.map { toPreparedForm(it) })
      is Multi -> error("should have been split by now: $uncommitted")
      is Transform -> error("should have been transformed already: $uncommitted")
    }
  }

  private fun prepareChangeInstruction(instruction: Change): Instruction {
    // can't prepare at all if we still have an X?
    val scal = instruction.count as? ActualScalar ?: return instruction
    val (g: MType?, r: MType?) = autoNarrowTypes(instruction.gaining, instruction.removing)

    if (g?.abstract == true || r?.abstract == true) {
      // Then narrowing the types some is all we'll do (TODO get more clever)
      return change(
          scal.value,
          gaining = g?.expression,
          removing = r?.expression,
          intensity = instruction.intensity!!,
      )
    }

    val gc = g?.let { Component.ofType(agentReader.resolve(it.expressionFull) as MType) }
    val rc = r?.let { Component.ofType(agentReader.resolve(it.expressionFull) as MType) }

    val upperLimit: Int = components.findLimit(gaining = gc, removing = rc)
    val adjusted: Int = min(scal.value, upperLimit)

    return if (instruction.intensity!! == MANDATORY) {
      // As long as we're capable of doing the full amount...
      if (adjusted != scal.value) {
        throw LimitsException(
            "When gaining $gc and removing $rc: can do only $adjusted of ${scal.value} required",
        )
      }
      // Then change nothing...
      return change(scal.value, gaining = gc?.expression, removing = rc?.expression)
    } else {
      val newIntensity = if (instruction.intensity == AMAP) MANDATORY else OPTIONAL
      change(adjusted, gaining = g?.expression, removing = r?.expression, newIntensity)
    }
  }

  private fun autoNarrowTypes(gaining: Expression?, removing: Expression?): Pair<MType?, MType?> {
    var g: MType? = gaining?.let(agentReader::resolve) as? MType
    var r: MType? = removing?.let(agentReader::resolve) as? MType

    if (g?.abstract == true) {
      // Infer a type if there IS only one concrete subtype
      // TODO filter down to those whose dependents exist
      g = g.allConcreteSubtypes().singleOrNull() ?: g
    }
    if (r?.abstract == true) {
      // Infer a type if there IS only one kind of component that has it
      // TODO could be smarter, like if the instr is mandatory and only one cpt type can satisfy
      r = agentReader.getComponents(r).singleOrNull() as? MType ?: r
    }
    return g to r
  }
}
