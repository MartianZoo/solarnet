package dev.martianzoo.tfm.text.turmoilexpansion

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.SystemClasses.OWNED
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.ScaledExpression.Companion.scaledEx
import dev.martianzoo.pets.types.Class
import dev.martianzoo.pets.types.Dependency.Key
import dev.martianzoo.tfm.text.Clause
import dev.martianzoo.tfm.text.Describers
import dev.martianzoo.tfm.text.EnglishText
import dev.martianzoo.tfm.text.Modifier
import dev.martianzoo.tfm.text.NounPhrase
import dev.martianzoo.tfm.text.Predicate
import dev.martianzoo.tfm.text.RenderedInstructions
import dev.martianzoo.tfm.text.Verb
import dev.martianzoo.tfm.text.renderEffect
import dev.martianzoo.tfm.text.renderGateCondition
import dev.martianzoo.tfm.text.renderInstructions
import dev.martianzoo.tfm.text.sameNamedTypeVariable

/** The resolution signal and universal player scope supply the printed event's context. */
internal fun renderGlobalEvent(event: Class, describers: Describers): EnglishText =
    EnglishText.join(
        event.declaration.authoredEffects.map { effect ->
          val conditional = effect.trigger as? Effect.Trigger.IfTrigger
          val trigger = conditional?.inner ?: effect.trigger
          if (trigger != resolutionTrigger) {
            renderEffect(effect, describers)
          } else {
            renderResolution(
                    Instruction.Gated.createTree(conditional?.condition, effect.instruction),
                    describers,
                )
                .asSentences()
          }
        }
    )

private fun renderResolution(
    instruction: InstructionTree,
    describers: Describers,
): RenderedInstructions {
  when (instruction) {
    is Instruction.Each -> {
      if (
          instruction.selector.copy(typeVariableName = null) == player &&
              instruction.selector.typeVariableName?.name == "Me"
      )
          return renderInstructions(instruction.body, describers)
      if (instruction.selector.copy(typeVariableName = null) == firstPlayer) {
        val gain = instruction.body as? Instruction.Gain
        val owner =
            gain?.gaining?.let(describers::resolveExpression)?.sourceDependency(Key(OWNED, 0))
        if (gain == null || owner == null || !sameNamedTypeVariable(owner, instruction.selector)) {
          return renderInstructions(instruction, describers)
        }
        val local =
            gain.copy(
                scaledEx =
                    scaledEx(
                        gain.gaining.copy(
                            arguments =
                                gain.gaining.arguments.filterNot {
                                  sameNamedTypeVariable(it, instruction.selector)
                                },
                            argumentsSpecified = false,
                        ),
                        gain.count,
                    )
            )
        val body = renderInstructions(local, describers)
        if (body.clauses.all { it is Clause.Simple && it.subject == null }) {
          return RenderedInstructions(
              body.clauses.map {
                Clause.Simple(
                    Predicate(
                        Verb("must"),
                        complement = Predicate.Complement.BareInfinitive(it),
                    ),
                    NounPhrase.text("the first player"),
                )
              }
          )
        }
      }
    }
    is Instruction.Gated -> {
      val condition =
          conditions[instruction.gate]?.let { Clause.Simple(Predicate(Verb(it))) }
              ?: describers.renderGateCondition(instruction.gate)
      if (condition != null) {
        return RenderedInstructions(
            listOf(
                Clause.Prefaced(
                    Clause.Preface.Conditional(condition),
                    renderResolution(instruction.inner, describers).asCoordinatedClause(),
                )
            )
        )
      }
    }
    is Instruction.By -> {
      if (instruction.actor == admin) {
        val body = renderInstructions(instruction.inner, describers)
        if (body.clauses.all { it is Clause.Simple && it.subject == null }) {
          return RenderedInstructions(
              body.clauses.map {
                val clause = it as Clause.Simple
                clause.copy(
                    predicate =
                        clause.predicate.copy(
                            modifiers =
                                listOf(
                                    Modifier.Phrase(
                                        "without gaining terraform rating or other bonuses"
                                    )
                                ) + clause.predicate.modifiers
                        )
                )
              }
          )
        }
      }
    }
    else -> Unit
  }
  return renderInstructions(instruction, describers)
}

private val resolutionTrigger = parse<Effect.Trigger>("ResolveGlobalEvent<Class<This>>")
private val player = parse<Expression>("Player")
private val firstPlayer = parse<Expression>("Player(HAS StartToken)")
private val admin = parse<Expression>("Admin")
private val conditions =
    mapOf(
        parse<Requirement>("GpIncomplete<Class<OceanTile>>") to "not all oceans have been placed",
        parse<Requirement>("GpIncomplete<Class<TemperatureStep>>") to
            "temperature has not reached its maximum",
    )
