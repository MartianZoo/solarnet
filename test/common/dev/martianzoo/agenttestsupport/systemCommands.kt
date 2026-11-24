package dev.martianzoo.agenttestsupport

import dev.martianzoo.agent.Agents
import dev.martianzoo.pets.Parsing
import dev.martianzoo.pets.PetElaborator
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.state.Actor.Companion.ADMIN

/**
 * Submits explicit system instructions for setup and scenarios, with session checks and recovery.
 */
internal fun Agents.runSystem(text: String, body: () -> Unit = {}) = transact {
  val instruction =
      PetElaborator(world.classTable).elaborateInput(Parsing.parse<InstructionTree>(text), null)
  world.actorEngine(ADMIN).addTasks(InstructionGroup.of(instruction as InstructionTree))
  transact()
  body()
}

/** Correction notation for scenarios; the session owns validation and the engine owns mutations. */
internal fun Agents.correct(text: String) =
    exMachina(
        InstructionGroup.of(
            PetElaborator(world.classTable)
                .elaborateInput(Parsing.parse<InstructionTree>(text), null) as InstructionTree
        )
    )
