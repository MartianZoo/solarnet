package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.CustomInstruction.ExecuteInsteadException
import dev.martianzoo.tfm.api.GameReader
import dev.martianzoo.tfm.api.GameWriter
import dev.martianzoo.tfm.api.SpecialClassNames.RAW
import dev.martianzoo.tfm.api.SpecialClassNames.THIS
import dev.martianzoo.tfm.api.UserException
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.pets.PureTransformers.replaceOwnerWith
import dev.martianzoo.tfm.pets.PureTransformers.transformInSeries
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Change
import dev.martianzoo.tfm.pets.ast.Instruction.Companion.split
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
import dev.martianzoo.tfm.pets.ast.TransformNode
import dev.martianzoo.tfm.types.Transformers

internal class ExecutionContext(
    val reader: GameReader,
    val writer: GameWriter,
    val xers: Transformers,
    val player: Player,
    val cause: Cause?,
) {
  fun doInstruction(instr: Instruction) {
    when (instr) {
      is NoOp -> {}
      is Change -> handleChange(instr)
      is Per -> doInstruction(instr.instruction * reader.count(instr.metric))
      is Gated -> {
        if (!reader.evaluate(instr.gate)) {
          if (instr.mandatory) throw UserException.requirementNotMet(instr.gate)
          return // do nothing
        }
        doInstruction(instr.instruction)
      }
      is Custom -> handleCustomInstruction(instr)
      is Or -> throw UserException.orWithoutChoice(instr)
      is Then -> split(instr.instructions).forEach { doInstruction(it) }
      is Multi -> error("should have been split: $instr")
      is Transform -> error("should have been transformed already: $instr")
    }
  }

  private fun handleChange(instr: Change) {
    val scal = instr.count as? ActualScalar ?: throw UserException.unresolvedX(instr)

    val amap =
        when (instr.intensity) {
          null -> error("should have had defaults inserted: $instr")
          MANDATORY -> false
          AMAP -> true
          OPTIONAL -> throw UserException.optionalAmount(instr)
        }

    writer.update(
        count = scal.value,
        gaining = instr.gaining?.let(reader::resolve),
        removing = instr.removing?.let(reader::resolve),
        amap = amap,
        cause = cause)
  }

  private fun handleCustomInstruction(instr: Custom) {
    val arguments = instr.arguments.map(reader::resolve)
    val abstractArgs = arguments.filter { it.abstract }
    if (abstractArgs.any()) throw UserException.abstractArguments(abstractArgs, instr)

    val custom = reader.setup.authority.customInstruction(instr.functionName)
    try {
      // Could call .raw() but would just unraw it again?
      val translated: Instruction = custom.translate(reader, arguments) * instr.multiplier

      // I guess custom instructions can't return things using `This`
      // and Owner means the context player... (TODO think)
      val instruction =
          transformInSeries(
              xers.atomizer(),
              xers.insertDefaults(THIS.expression), // TODO context component??
              xers.deprodify(),
              replaceOwnerWith(player.className),
              TransformNode.unwrapper(RAW),
          )
              .transform(translated)

      split(instruction).forEach { writer.addTasks(it, player, cause) }
    } catch (e: ExecuteInsteadException) {
      // this custom fn chose to override execute() instead of translate()
      custom.execute(reader, writer, arguments, instr.multiplier)
    }
  }
}
