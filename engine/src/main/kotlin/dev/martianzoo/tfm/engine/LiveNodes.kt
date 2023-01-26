package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.CustomInstruction
import dev.martianzoo.tfm.api.CustomInstruction.ExecuteInsteadException
import dev.martianzoo.tfm.api.standardResourceNames
import dev.martianzoo.tfm.data.StateChange
import dev.martianzoo.tfm.engine.ComponentGraph.Component
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Effect.Trigger
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.AbstractInstructionException
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.AMAP
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.OPTIONAL
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.Requirement.Exact
import dev.martianzoo.tfm.pets.ast.Requirement.Max
import dev.martianzoo.tfm.pets.ast.Requirement.Min
import dev.martianzoo.tfm.pets.deprodify
import dev.martianzoo.tfm.types.PType

object LiveNodes {
  fun from(ins: Instruction, game: Game): LiveInstruction {
    return when (ins) {
      is Instruction.Change ->
          Change(
              ins.count,
              ins.intensity ?: MANDATORY,
              removing = ins.removing?.let { game.resolve(it) },
              gaining = ins.gaining?.let { game.resolve(it) },
          )
      is Instruction.Per ->
          Per(game.resolve(ins.sat.typeExpr), ins.sat.scalar, from(ins.instruction, game))
      is Instruction.Gated -> Gated(from(ins.gate, game), from(ins.instruction, game))
      is Instruction.Custom ->
          Custom(
              game.authority.customInstruction(ins.functionName),
              ins.arguments.map { game.resolve(it) })
      is Instruction.Or -> OrIns(ins.instructions.toList().map { from(it, game) }) // TODO
      is Instruction.Then -> Then(ins.instructions.map { from(it, game) })
      is Instruction.Multi -> Then(ins.instructions.map { from(it, game) })
      else -> TODO()
    }
  }

  abstract class LiveInstruction {
    open operator fun times(factor: Int): LiveInstruction = error("Not supported")
    open fun execute(game: Game): Unit = error("Not Supported")
  }

  class Change(
      val count: Int,
      private val intensity: Intensity = MANDATORY,
      val removing: PType? = null,
      val gaining: PType? = null,
  ) : LiveInstruction() {
    init {
      require(count > 0)
    }

    override fun times(factor: Int) = Change(count * factor, intensity, removing, gaining)

    override fun execute(game: Game) {
      // treat null as MANDATORY (TODO)
      if (intensity == OPTIONAL) {
        throw AbstractInstructionException("optional")
      }
      game.applyChange(
          count = count,
          // TODO fix
          removing = removing?.let(::Component),
          gaining = gaining?.let(::Component),
          amap = intensity == AMAP)
    }
  }

  class Per(val ptype: PType, private val unit: Int = 1, val instruction: LiveInstruction) :
      LiveInstruction() {

    override fun times(factor: Int) = Per(ptype, unit, instruction * factor)

    override fun execute(game: Game) = (instruction * (game.count(ptype) / unit)).execute(game)
  }

  class Gated(private val gate: LiveRequirement, val instruction: LiveInstruction) : LiveInstruction() {
    override fun times(factor: Int) = Gated(gate, instruction * factor)
    override fun execute(game: Game) =
        if (gate.isMet(game)) {
          instruction.execute(game)
        } else {
          throw UserException("Requirement not met: $gate")
        }
  }

  class Custom(private val custom: CustomInstruction, private val arguments: List<PType>) : LiveInstruction() {
    override fun execute(game: Game) {
      try {
        val translated: Instruction =
            custom.translate(game.asGameState, arguments.map { it.toTypeExprFull() })
        val deprodded = deprodify(translated, standardResourceNames(game.asGameState))
        from(deprodded, game).execute(game)
      } catch (e: ExecuteInsteadException) {
        custom.execute(game.asGameState, arguments.map { it.toTypeExprFull() })
      }
    }
  }

  class OrIns(private val instructions: List<LiveInstruction>) : LiveInstruction() {
    override fun times(factor: Int) = OrIns(instructions.map { it * factor })
    override fun execute(game: Game) = throw UserException("Can't execute an OR")
  }

  class Then(private val instructions: List<LiveInstruction>) : LiveInstruction() {
    override fun times(factor: Int) = Then(instructions.map { it * factor })
    override fun execute(game: Game) = instructions.forEach { it.execute(game) }
  }

  fun from(req: Requirement, game: Game): LiveRequirement {
    return when (req) {
      is Min -> LiveRequirement { game.count(req.sat.typeExpr) >= req.sat.scalar }
      is Max -> LiveRequirement { game.count(req.sat.typeExpr) <= req.sat.scalar }
      is Exact -> LiveRequirement { game.count(req.sat.typeExpr) == req.sat.scalar }
      is Requirement.Or -> {
        val reqs = req.requirements.toList().map { from(it, game) }
        LiveRequirement { reqs.any { it.isMet(game) } }
      }
      is Requirement.And -> {
        val reqs = req.requirements.map { from(it, game) }
        LiveRequirement { reqs.all { it.isMet(game) } }
      }
      is Requirement.Transform -> error("should have been transformed by now")
    }
  }

  class LiveRequirement(private val isMet: (Game) -> Boolean) {
    fun isMet(game: Game) = isMet.invoke(game)
  }

  fun from(trig: Trigger, game: Game): LiveTrigger {
    return when (trig) {
      is Trigger.OnGain -> OnGain(game.resolve(trig.typeExpr))
      is Trigger.OnRemove -> OnRemove(game.resolve(trig.typeExpr))
      is Trigger.Transform -> error("")
    }
  }

  abstract class LiveTrigger {
    abstract fun hits(change: StateChange, game: Game): Int
  }

  class OnGain(val ptype: PType) : LiveTrigger() {
    override fun hits(change: StateChange, game: Game): Int {
      val g = change.gaining
      return if (g != null && game.resolve(g).isSubtypeOf(ptype)) change.count else 0
    }
  }

  class OnRemove(val ptype: PType) : LiveTrigger() {
    override fun hits(change: StateChange, game: Game): Int {
      val r = change.removing
      return if (r != null && game.resolve(r).isSubtypeOf(ptype)) change.count else 0
    }
  }

  fun from(effect: Effect, game: Game) =
      LiveEffect(from(effect.trigger, game), from(effect.instruction, game))

  class LiveEffect(val trigger: LiveTrigger, val instruction: LiveInstruction)
}
