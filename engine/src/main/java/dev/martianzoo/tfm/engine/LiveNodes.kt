package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.CustomInstruction
import dev.martianzoo.tfm.api.CustomInstruction.ExecuteInsteadException
import dev.martianzoo.tfm.api.GameState
import dev.martianzoo.tfm.api.ResourceUtils.standardResourceNames
import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.data.ChangeRecord.StateChange
import dev.martianzoo.tfm.pets.AstTransforms.deprodify
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Effect.Trigger
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.AbstractInstructionException
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.AMAP
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.OPTIONAL
import dev.martianzoo.tfm.pets.ast.Metric
import dev.martianzoo.tfm.pets.ast.Metric.Count
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.Requirement.Exact
import dev.martianzoo.tfm.pets.ast.Requirement.Min
import dev.martianzoo.tfm.pets.ast.ScaledExpression
import kotlin.math.min

internal object LiveNodes {
  data class LiveMetric(val type: Type, val divisor: Int = 1, val max: Int = Int.MAX_VALUE) {
    fun count(game: GameState) = min(game.count(type) / divisor, max)
  }

  fun from(met: Metric, game: GameState): LiveMetric =
      when (met) {
        is Count -> LiveMetric(game.resolve(met.scaledEx.expression), met.scaledEx.scalar)
        is Metric.Max -> from(met.metric, game).copy(max = met.maximum)
      }

  fun from(ins: Instruction, game: GameState): LiveInstruction {
    return when (ins) {
      is Instruction.Change ->
        Change(
            ins.count,
            ins.intensity ?: MANDATORY,
            removing = ins.removing?.let { game.resolve(it) },
            gaining = ins.gaining?.let { game.resolve(it) },
        )

      is Instruction.Per -> Per(from(ins.metric, game), from(ins.instruction, game))
      is Instruction.Gated -> Gated(from(ins.gate, game), from(ins.instruction, game))
      is Instruction.Custom ->
          Custom(
              game.authority.customInstruction(ins.functionName),
              ins.arguments.map { game.resolve(it) })
      is Instruction.Or -> OrIns(ins.instructions.map { from(it, game) })
      is Instruction.Then -> Then(ins.instructions.map { from(it, game) })
      is Instruction.Multi -> Then(ins.instructions.map { from(it, game) })
      is Instruction.Transform -> error("should have been transformed already")
    }
  }

  abstract class LiveInstruction {
    abstract operator fun times(factor: Int): LiveInstruction
    abstract fun execute(game: GameState)
  }

  class Change(
      val count: Int,
      private val intensity: Intensity = MANDATORY,
      val removing: Type? = null, // must be concrete by the time executed
      val gaining: Type? = null,
  ) : LiveInstruction() {
    init {
      require(count > 0)
    }

    override fun times(factor: Int) = Change(count * factor, intensity, removing, gaining)

    override fun execute(game: GameState) {
      // treat null as MANDATORY (TODO change that?)
      if (intensity == OPTIONAL) {
        throw AbstractInstructionException("optional")
      }
      game.applyChangeAndPublish(
          count = count, removing = removing, gaining = gaining, amap = intensity == AMAP)
    }
  }

  class Per(val metric: LiveMetric, val instruction: LiveInstruction) : LiveInstruction() {

    override fun times(factor: Int) = Per(metric, instruction * factor)
    override fun execute(game: GameState) = (instruction * metric.count(game)).execute(game)
  }

  class Gated(private val gate: LiveRequirement, val instruction: LiveInstruction) :
      LiveInstruction() {
    override fun times(factor: Int) = Gated(gate, instruction * factor)
    override fun execute(game: GameState) =
        if (gate.evaluate(game)) {
          instruction.execute(game)
        } else {
          throw UserException("Requirement not met: $gate")
        }
  }

  class Custom(private val custom: CustomInstruction, private val arguments: List<Type>) :
      LiveInstruction() {

    override fun times(factor: Int) = error("can't")

    override fun execute(game: GameState) {
      try {
        val translated: Instruction = custom.translate(game, arguments)
        val deprodded = deprodify(translated, standardResourceNames(game))
        from(deprodded, game).execute(game)

      } catch (e: ExecuteInsteadException) {
        // `custom` chose to override execute() instead of translate()
        custom.execute(game, arguments)
      }
    }
  }

  class OrIns(private val instructions: List<LiveInstruction>) : LiveInstruction() {
    override fun times(factor: Int) = OrIns(instructions.map { it * factor })
    override fun execute(game: GameState) = throw UserException("Can't execute an OR")
  }

  class Then(private val instructions: List<LiveInstruction>) : LiveInstruction() {
    override fun times(factor: Int) = Then(instructions.map { it * factor })
    override fun execute(game: GameState) = instructions.forEach { it.execute(game) }
  }

  fun from(reqt: Requirement, game: GameState): LiveRequirement {
    fun count(scaledEx: ScaledExpression) =
        game.count(game.resolve(scaledEx.expression))

    return when (reqt) {
      is Min -> LiveRequirement { count(reqt.scaledEx) >= reqt.scaledEx.scalar }
      is Requirement.Max -> LiveRequirement { count(reqt.scaledEx) <= reqt.scaledEx.scalar }
      is Exact -> LiveRequirement { count(reqt.scaledEx) == reqt.scaledEx.scalar }
      is Requirement.Or -> {
        val reqts = reqt.requirements.toList().map { from(it, game) }
        LiveRequirement { reqts.any { it.evaluate(game) } }
      }
      is Requirement.And -> {
        val reqts = reqt.requirements.map { from(it, game) }
        LiveRequirement { reqts.all { it.evaluate(game) } }
      }
      is Requirement.Transform -> error("should have been transformed by now")
    }
  }

  class LiveRequirement(private val evaluator: (GameState) -> Boolean) {
    fun evaluate(game: GameState) = evaluator(game)
  }

  fun from(trig: Trigger, game: GameState): LiveTrigger {
    return when (trig) {
      is Trigger.OnGainOf -> LiveTrigger(game.resolve(trig.expression), gain = true)
      is Trigger.OnRemoveOf -> LiveTrigger(game.resolve(trig.expression), gain = false)
      is Trigger.ByTrigger -> from(trig.inner, game).copy(by = trig.by)
      else -> error("this shouldn't still be here")
    }
  }

  data class LiveTrigger(val ptype: Type, val gain: Boolean, val by: ClassName? = null) {
    fun hits(change: StateChange, game: GameState): Int {
      val g = if (gain) change.gaining else change.removing
      return if (g != null && game.resolve(g).isSubtypeOf(ptype)) change.count else 0
    }
  }

  fun from(effect: Effect, game: GameState) =
      LiveEffect(from(effect.trigger, game), from(effect.instruction, game))

  class LiveEffect(val trigger: LiveTrigger, val instruction: LiveInstruction)
}
