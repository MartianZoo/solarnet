package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.CustomInstruction
import dev.martianzoo.tfm.api.CustomInstruction.ExecuteInsteadException
import dev.martianzoo.tfm.api.ReadOnlyGameState.GameState
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
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.Requirement.Exact
import dev.martianzoo.tfm.pets.ast.Requirement.Max
import dev.martianzoo.tfm.pets.ast.Requirement.Min
import dev.martianzoo.tfm.pets.ast.ScaledTypeExpr

internal object LiveNodes {
  fun from(ins: Instruction, game: GameState): LiveInstruction {
    return when (ins) {
      is Instruction.Change ->
          Change(
              ins.count,
              ins.intensity ?: MANDATORY,
              removing = ins.removing?.let { game.resolveType(it) },
              gaining = ins.gaining?.let { game.resolveType(it) },
          )
      is Instruction.Per ->
          Per(
              game.resolveType(ins.scaledType.typeExpr),
              ins.scaledType.scalar,
              from(ins.instruction, game))
      is Instruction.Gated -> Gated(from(ins.gate, game), from(ins.instruction, game))
      is Instruction.Custom ->
          Custom(
              game.setup.authority.customInstruction(ins.functionName),
              ins.arguments.map { game.resolveType(it) })
      is Instruction.Or -> OrIns(ins.instructions.toList().map { from(it, game) }) // TODO
      is Instruction.Then -> Then(ins.instructions.map { from(it, game) })
      is Instruction.Multi -> Then(ins.instructions.map { from(it, game) })
      is Instruction.Transform -> error("should have been transformed already")
    }
  }

  abstract class LiveInstruction {
    open operator fun times(factor: Int): LiveInstruction = error("Not supported")
    open fun execute(game: GameState): Unit = error("Not Supported")
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
      // treat null as MANDATORY (TODO)
      if (intensity == OPTIONAL) {
        throw AbstractInstructionException("optional")
      }
      game.applyChange(
          count = count, removing = removing, gaining = gaining, amap = intensity == AMAP)
    }
  }

  class Per(val ptype: Type, private val unit: Int = 1, val instruction: LiveInstruction) :
      LiveInstruction() {

    override fun times(factor: Int) = Per(ptype, unit, instruction * factor)

    override fun execute(game: GameState) =
        (instruction * (game.countComponents(ptype) / unit)).execute(game)
  }

  class Gated(private val gate: LiveRequirement, val instruction: LiveInstruction) :
      LiveInstruction() {
    override fun times(factor: Int) = Gated(gate, instruction * factor)
    override fun execute(game: GameState) =
        if (gate.isMet(game)) {
          instruction.execute(game)
        } else {
          throw UserException("Requirement not met: $gate")
        }
  }

  class Custom(private val custom: CustomInstruction, private val arguments: List<Type>) :
      LiveInstruction() {
    override fun execute(game: GameState) {
      try {
        val translated: Instruction = custom.translate(game, arguments)
        val deprodded = deprodify(translated, standardResourceNames(game))
        from(deprodded, game).execute(game)
      } catch (e: ExecuteInsteadException) {
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

  fun from(req: Requirement, game: GameState): LiveRequirement {
    fun count(scaledType: ScaledTypeExpr) =
        game.countComponents(game.resolveType(scaledType.typeExpr))

    return when (req) {
      is Min -> LiveRequirement { count(req.scaledType) >= req.scaledType.scalar }
      is Max -> LiveRequirement { count(req.scaledType) <= req.scaledType.scalar }
      is Exact -> LiveRequirement { count(req.scaledType) == req.scaledType.scalar }
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

  class LiveRequirement(private val isMet: (GameState) -> Boolean) {
    fun isMet(game: GameState) = isMet.invoke(game)
  }

  fun from(trig: Trigger, game: GameState): LiveTrigger {
    return when (trig) {
      is Trigger.OnGainOf -> LiveTrigger(game.resolveType(trig.typeExpr), isGain = true)
      is Trigger.OnRemoveOf -> LiveTrigger(game.resolveType(trig.typeExpr), isGain = false)
      is Trigger.ByTrigger -> from(trig.inner, game).copy(by = trig.by)
      else -> error("this shouldn't still be here")
    }
  }

  data class LiveTrigger(val ptype: Type, val isGain: Boolean, val by: ClassName? = null) {
    fun hits(change: StateChange, game: GameState): Int {
      val g = if (isGain) change.gaining else change.removing
      return if (g != null && game.resolveType(g).isSubtypeOf(ptype)) change.count else 0
    }
  }

  fun from(effect: Effect, game: GameState) =
      LiveEffect(from(effect.trigger, game), from(effect.instruction, game))

  class LiveEffect(val trigger: LiveTrigger, val instruction: LiveInstruction)
}
