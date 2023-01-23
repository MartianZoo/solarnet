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
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.PetNode.GenericTransform
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.Requirement.Exact
import dev.martianzoo.tfm.pets.ast.Requirement.Max
import dev.martianzoo.tfm.pets.ast.Requirement.Min
import dev.martianzoo.tfm.pets.deprodify
import dev.martianzoo.tfm.types.PetType

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
        Per(game.resolve(ins.sat.type), ins.sat.scalar, from(ins.instruction, game))

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
      val intensity: Intensity = MANDATORY,
      val removing: PetType? = null,
      val gaining: PetType? = null,
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
          gaining = gaining?.let(::Component),
          removing = removing?.let(::Component),
          amap = intensity == AMAP)
    }
  }

  class Per(val type: PetType, val unit: Int = 1, val instruction: LiveInstruction) :
      LiveInstruction() {

    override fun times(factor: Int) = Per(type, unit, instruction * factor)

    override fun execute(game: Game) = (instruction * (game.count(type) / unit)).execute(game)
  }

  class Gated(val gate: LiveRequirement, val instruction: LiveInstruction) : LiveInstruction() {
    override fun times(factor: Int) = Gated(gate, instruction * factor)
    override fun execute(game: Game) =
        if (gate.isMet(game)) {
          instruction.execute(game)
        } else {
          throw UserException("Requirement not met: $gate")
        }
  }

  class Custom(val custom: CustomInstruction, val arguments: List<PetType>) : LiveInstruction() {
    override fun execute(game: Game) {
      try {
        var translated: Instruction =
            custom.translate(game.asGameState, arguments.map { it.toTypeExpression() })
        if (translated.childNodesOfType<PetNode>().any {
            // TODO deprodify could do this??
            it is GenericTransform<*> && it.transform == "PROD"
          }) {
          translated = deprodify(translated, standardResourceNames(game.asGameState))
        }
        from(translated, game).execute(game)
      } catch (e: ExecuteInsteadException) {
        custom.execute(game.asGameState, arguments.map { it.toTypeExpression() })
      }
    }
  }

  class OrIns(val instructions: List<LiveInstruction>) : LiveInstruction() {
    override fun times(factor: Int) = OrIns(instructions.map { it * factor })
    override fun execute(game: Game) = throw UserException("Can't execute an OR")
  }

  class Then(val instructions: List<LiveInstruction>) : LiveInstruction() {
    override fun times(factor: Int) = Then(instructions.map { it * factor })
    override fun execute(game: Game) = instructions.forEach { it.execute(game) }
  }

  fun from(req: Requirement, game: Game): LiveRequirement {
    return when (req) {
      is Min -> LiveRequirement { game.count(req.sat.type) >= req.sat.scalar }
      is Max -> LiveRequirement { game.count(req.sat.type) <= req.sat.scalar }
      is Exact -> LiveRequirement { game.count(req.sat.type) == req.sat.scalar }
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

  class LiveRequirement(val isMet: (Game) -> Boolean) {
    fun isMet(game: Game) = isMet.invoke(game)
  }

  fun from(trig: Trigger, game: Game): LiveTrigger {
    return when (trig) {
      is Trigger.OnGain -> OnGain(game.resolve(trig.expression))
      is Trigger.OnRemove -> OnRemove(game.resolve(trig.expression))
      is Trigger.Transform -> error("")
    }
  }

  abstract class LiveTrigger {
    abstract fun hits(change: StateChange, game: Game): Int
  }

  class OnGain(val type: PetType) : LiveTrigger() {
    override fun hits(change: StateChange, game: Game): Int {
      val g = change.gaining
      return if (g != null && game.resolve(g).isSubtypeOf(type)) change.count else 0
    }
  }

  class OnRemove(val type: PetType) : LiveTrigger() {
    override fun hits(change: StateChange, game: Game): Int {
      val r = change.removing
      return if (r != null && game.resolve(r).isSubtypeOf(type)) change.count else 0
    }
  }

  fun from(effect: Effect, game: Game) =
      LiveEffect(from(effect.trigger, game), from(effect.instruction, game))

  class LiveEffect(val trigger: LiveTrigger, val instruction: LiveInstruction)
}
