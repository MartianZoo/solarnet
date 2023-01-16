package dev.martianzoo.tfm.pets.ast

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.tfm.pets.PetException
import dev.martianzoo.tfm.pets.PetParser
import dev.martianzoo.tfm.pets.ast.Instruction.Gated
import dev.martianzoo.tfm.pets.ast.TypeExpression.TypeParsers.genericType
import dev.martianzoo.util.iff

data class Effect(
    val trigger: Trigger,
    val instruction: Instruction,
    val automatic: Boolean,
) : PetNode() {

  override val kind = "Effect"

  override fun toString(): String {
    val instext = when (instruction) {
      is Gated -> "($instruction)"
      else -> "$instruction"
    }
    return "$trigger:${iff(automatic, ":")} $instext"
  }

  sealed class Trigger : PetNode() {
    override val kind = "Trigger"

    data class OnGain(val expression: TypeExpression) : Trigger() {
      override fun toString() = "$expression"
    }

    data class OnRemove(val expression: TypeExpression) : Trigger() {
      override fun toString() = "-${expression}"
    }

    data class Transform(val trigger: Trigger, override val transform: String) :
        Trigger(), GenericTransform<Trigger> {
      init {
        if (trigger !is OnGain && trigger !is OnRemove) {
          throw PetException("only gain/remove trigger can go in transform block")
        }
      }

      override fun toString() = "$transform[${trigger}]"

      override fun extract() = trigger
    }

    companion object : PetParser() {
      fun parser(): Parser<Trigger> {
        val onGain = genericType map Trigger::OnGain
        val onRemove = skipChar('-') and genericType map Trigger::OnRemove
        val atom = onGain or onRemove

        val transform = transform(atom) map { (node, type) -> Trigger.Transform(node, type) }

        return transform or atom
      }
    }
  }

  companion object : PetParser() {
    fun parser(): Parser<Effect> {
      val colons = _doubleColon or char(':') map { it.text == "::" }

      return Trigger.parser() and
          colons and
          maybeGroup(Instruction.parser()) map {
        (trig, immed, instr) ->
            Effect(trigger = trig, automatic = immed, instruction = instr)
          }
    }
  }
}
