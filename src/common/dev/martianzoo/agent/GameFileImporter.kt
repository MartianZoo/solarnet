package dev.martianzoo.agent

import dev.martianzoo.catalog.Catalog
import dev.martianzoo.catalog.GamePremise
import dev.martianzoo.engine.recording
import dev.martianzoo.pets.api.SystemClasses.COMPONENT
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.state.Actor
import dev.martianzoo.state.ComponentChange
import dev.martianzoo.state.GameEvent
import dev.martianzoo.state.GameEvent.ChangeEvent.Cause
import dev.martianzoo.state.GameFileJson
import dev.martianzoo.state.RecordedInput
import dev.martianzoo.state.Task
import dev.martianzoo.state.Task.TaskId

/** Rebuilds a game from its recorded Actor inputs, with all Actor autoexecution disabled. */
public object GameFileImporter {
  /**
   * Checks the Catalog and resolved premise before creating a live game. [newAgents] provides the
   * game's runtime classes; [startGame] starts the recorded game. A full file also checks every
   * component change and pending task after replay, ignoring internal event ordinals and
   * intermediate task revisions. Change commentary from the file is retained on matching live
   * changes; commentary produced by the live game must appear verbatim in the recorded note.
   */
  public suspend fun replay(
      document: GameFileJson.Document,
      catalog: Catalog,
      newAgents: (GamePremise) -> Agents,
      startGame: (Agents) -> Unit,
      supplyCardInputs: ((Agents, List<RecordedInput.CardInput>) -> Unit)? = null,
  ): Agents {
    val premise = document.premise(catalog)
    val expected = if (document.hasEvents) document.decode(premise) else null
    val agents = newAgents(premise)
    TaskLog.replay(
        document.inputs(premise),
        agents,
        startGame = {
          startGame(agents)
        },
        supplyCardInputs = { command ->
          requireNotNull(supplyCardInputs) { "card identities have no input supplier" }(
              agents,
              command.cardInputs,
          )
        },
    )
    expected?.let { source ->
      val actual = agents.world.recording()
      val sourceChanges = source.events.filterIsInstance<GameEvent.ChangeEvent>()
      val actualChanges = actual.events.filterIsInstance<GameEvent.ChangeEvent>()
      val sharedSignatures = mutableMapOf<ChangeSignature, Int>()
      val sourceSignatures = signatures(sourceChanges, sharedSignatures)
      val actualSignatures = signatures(actualChanges, sharedSignatures)
      val sourceByChange = sourceChanges.groupBy { sourceSignatures.getValue(it.ordinal) }
      val actualByChange = actualChanges.groupBy { actualSignatures.getValue(it.ordinal) }
      check(
          sourceByChange.mapValues { it.value.size } == actualByChange.mapValues { it.value.size }
      ) {
        "replayed component changes differ"
      }
      actualByChange.forEach { (change, imported) ->
        val unmatched = sourceByChange.getValue(change).toMutableList()
        imported.forEach { actualChange ->
          val liveNote = actualChange.notes
          val matching = unmatched.indexOfFirst { recorded ->
            liveNote == null ||
                recorded.notes?.lineSequence()?.toSet()?.let { lines ->
                  liveNote.lineSequence().all { it in lines }
                } == true
          }
          check(matching >= 0) { "replayed component note differs for $change" }
          val sourceChange = unmatched.removeAt(matching)
          agents.world.events.changeAt(actualChange.ordinal)?.notes = sourceChange.notes
        }
      }
      val passive = source.open().world
      val expectedComponents = passive.reader.getComponents(COMPONENT.expression).entries
      val actualComponents = agents.world.reader.getComponents(COMPONENT.expression).entries
      check(expectedComponents == actualComponents) {
        "replayed final components differ: expected only ${expectedComponents - actualComponents}; actual only ${actualComponents - expectedComponents}"
      }
      check(
          pending(passive.tasks.extract { it }, sourceSignatures) ==
              pending(agents.world.tasks.extract { it }, actualSignatures)
      ) {
        "replayed pending tasks differ"
      }
    }
    return agents
  }

  private data class ChangeSignature(
      val actor: Actor,
      val change: ComponentChange,
      val cause: Pair<Expression, Int>?,
  )

  // Intern both histories together: a cause points to its trigger's signature, preserving the
  // complete ancestry without copying an increasingly long list for every change in a game.
  private fun signatures(
      changes: List<GameEvent.ChangeEvent>,
      shared: MutableMap<ChangeSignature, Int>,
  ): Map<Int, Int> {
    val byOrdinal = mutableMapOf<Int, Int>()
    changes.forEach { event ->
      val signature =
          ChangeSignature(event.actor, event.change, causeSignature(event.cause, byOrdinal))
      byOrdinal[event.ordinal] = shared.getOrPut(signature) { shared.size }
    }
    return byOrdinal
  }

  private fun causeSignature(
      cause: Cause?,
      signatures: Map<Int, Int>,
  ): Pair<Expression, Int>? = cause?.let { it.context to signatures.getValue(it.triggerEvent) }

  private fun pending(
      tasks: List<Task>,
      signatures: Map<Int, Int>,
  ): Map<Pair<Task, Pair<Expression, Int>?>, Int> =
      tasks
          .map { it.copy(id = TaskId(0), cause = null) to causeSignature(it.cause, signatures) }
          .groupingBy { it }
          .eachCount()
}
