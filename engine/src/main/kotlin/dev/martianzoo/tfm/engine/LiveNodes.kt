package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.CustomInstruction
import dev.martianzoo.tfm.api.CustomInstruction.ExecuteInsteadException
import dev.martianzoo.tfm.api.standardResourceNames
import dev.martianzoo.tfm.engine.ComponentGraph.Component
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
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.deprodify
import dev.martianzoo.tfm.types.PetType
import kotlin.Int.Companion.MAX_VALUE

class LiveNodes(val game: Game) {

  @JvmName("resolveNullable")
  private fun resolve(type: TypeExpression?) = type?.let { game.resolve(it) }
  private fun resolve(type: TypeExpression) = type.let { game.resolve(it) }
  private fun resolve(types: List<TypeExpression>) = types.map { game.resolve(it) }

  inner class LiveMetric(val type: PetType, val amount: Int = 1) {
    init {
      require(amount > 0)
    }
    fun measure(): Int = game.count(type) / amount
  }

  @JvmName("fromInstructions")
  fun from(inses: List<Instruction>): List<LiveInstruction> = inses.map(::from)

  fun from(ins: Instruction): LiveInstruction {
    return when (ins) {
      is Instruction.Change ->
          Change(ins.count, ins.intensity ?: MANDATORY, resolve(ins.removing), resolve(ins.gaining))
      is Instruction.Per ->
          Per(LiveMetric(resolve(ins.sat.type), ins.sat.scalar), from(ins.instruction))
      is Instruction.Gated -> Gated(from(ins.gate), from(ins.instruction))
      is Instruction.Custom ->
          Custom(game.authority.customInstruction(ins.functionName), resolve(ins.arguments))
      is Instruction.Or -> OrIns(from(ins.instructions.toList())) // TODO
      is Instruction.Then -> Then(from(ins.instructions))
      is Instruction.Multi -> Then(from(ins.instructions))
      else -> TODO()
    }
  }

  abstract inner class LiveInstruction {
    open operator fun times(factor: Int): LiveInstruction = error("Not supported")
    open fun execute(): Unit = error("Not Supported")
  }

  inner class Change(
      val count: Int,
      val intensity: Intensity = MANDATORY,
      val removing: PetType? = null,
      val gaining: PetType? = null
  ) : LiveInstruction() {
    init {
      require(count > 0)
    }

    override fun times(factor: Int) = Change(count * factor, intensity, removing, gaining)

    override fun execute() {
      // treat null as MANDATORY (TODO)
      if (intensity == OPTIONAL) {
        throw AbstractInstructionException("optional")
      }
      game.applyChange(
          count = count,
          // TODO fix
          gaining = gaining?.let { Component(it).asTypeExpression.asGeneric() },
          removing = removing?.let { Component(it).asTypeExpression.asGeneric() },
          amap = intensity == AMAP)
    }
  }

  inner class Per(val metric: LiveMetric, val instruction: LiveInstruction) : LiveInstruction() {

    override fun times(factor: Int) = Per(metric, instruction * factor)

    override fun execute() = (instruction * metric.measure()).execute()
  }

  inner class Gated(val gate: LiveRequirement, val instruction: LiveInstruction) :
      LiveInstruction() {
    override fun times(factor: Int) = Gated(gate, instruction * factor)
    override fun execute() =
        if (gate.isMet()) {
          instruction.execute()
        } else {
          throw UserException("Requirement not met: $gate")
        }
  }

  inner class Custom(val custom: CustomInstruction, val arguments: List<PetType>) :
      LiveInstruction() {
    override fun execute() {
      try {
        var translated = custom.translate(game.asGameState, arguments.map { it.toTypeExpression() })
        if (translated.childNodesOfType<PetNode>().any {
          // TODO deprodify could do this??
          it is GenericTransform<*> && it.transform == "PROD"
        }) {
          translated = deprodify(translated, standardResourceNames(game.asGameState))
        }
        from(translated).execute()
      } catch (e: ExecuteInsteadException) {
        custom.execute(game.asGameState, arguments.map { it.toTypeExpression() })
      }
    }
  }

  inner class OrIns(val instructions: List<LiveInstruction>) : LiveInstruction() {
    override fun times(factor: Int) = OrIns(instructions.map { it * factor })
    override fun execute() = throw UserException("Can't execute an OR")
  }

  inner class Then(val instructions: List<LiveInstruction>) : LiveInstruction() {
    override fun times(factor: Int) = Then(instructions.map { it * factor })
    override fun execute() = instructions.forEach { it.execute() }
  }

  @JvmName("fromRequirements")
  fun from(reqs: List<Requirement>): List<LiveRequirement> = reqs.map(::from)

  fun from(req: Requirement): LiveRequirement {
    return when (req) {
      is Min -> InRange(LiveMetric(game.resolve(req.sat.type)), req.sat.scalar..MAX_VALUE)
      is Max -> InRange(LiveMetric(game.resolve(req.sat.type)), 0..req.sat.scalar)
      is Exact -> InRange(LiveMetric(game.resolve(req.sat.type)), req.sat.scalar..req.sat.scalar)
      is Requirement.Or ->
          object : LiveRequirement() {
            override fun isMet() = from(req.requirements.toList()).any { it.isMet() }
          }
      is Requirement.And ->
          object : LiveRequirement() {
            override fun isMet() = from(req.requirements).all { it.isMet() }
          }
      is Requirement.Transform -> error("should have been transformed by now")
    }
  }

  abstract inner class LiveRequirement {
    abstract fun isMet(): Boolean
  }

  inner class InRange(val metric: LiveMetric, val range: IntRange) : LiveRequirement() {
    override fun isMet() = metric.measure() in range
  }
}
