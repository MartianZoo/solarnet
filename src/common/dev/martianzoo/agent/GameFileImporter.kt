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
   * game's runtime classes; [startWorkflow] launches the recorded workflow. A full file also checks
   * every component change and pending task after replay, ignoring internal event ordinals and
   * intermediate task revisions. Change commentary from the file is retained on matching live
   * changes; commentary produced by the live game must appear verbatim in the recorded note.
   */
  public suspend fun replay(
      document: GameFileJson.Document,
      catalog: Catalog,
      newAgents: (GamePremise) -> Agents,
      startWorkflow: (Agents, String) -> Unit,
      supplyCardInputs: ((Agents, List<RecordedInput.CardInput>) -> Unit)? = null,
  ): Agents {
    val premise = document.premise(catalog)
    val expected = if (document.hasEvents) document.decode(premise) else null
    val agents = newAgents(premise)
    TaskLog.replay(
        document.inputs(premise),
        agents,
        startWorkflow = { startWorkflow(agents, it) },
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
      val sourceByChange = sourceChanges.groupBy { signature(source.events, it) }
      val actualByChange = actualChanges.groupBy { signature(actual.events, it) }
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
          pending(passive.tasks.extract { it }, source.events) ==
              pending(agents.world.tasks.extract { it }, actual.events)
      ) {
        "replayed pending tasks differ"
      }
    }
    return agents
  }

  private data class ChangeSignature(
      val actor: Actor,
      val change: ComponentChange,
      val cause: List<Triple<Expression, Actor, ComponentChange>>,
  )

  private fun signature(events: List<GameEvent>, event: GameEvent.ChangeEvent): ChangeSignature =
      ChangeSignature(event.actor, event.change, causeHistory(events, event.cause))

  private fun causeHistory(
      events: List<GameEvent>,
      cause: Cause?,
  ): List<Triple<Expression, Actor, ComponentChange>> {
    if (cause == null) return emptyList()
    val trigger = events[cause.triggerEvent] as GameEvent.ChangeEvent
    return listOf(Triple(cause.context, trigger.actor, trigger.change)) +
        causeHistory(events, trigger.cause)
  }

  private fun pending(
      tasks: List<Task>,
      events: List<GameEvent>,
  ): Map<Pair<Task, List<Triple<Expression, Actor, ComponentChange>>>, Int> =
      tasks
          .map { it.copy(id = TaskId(0), cause = null) to causeHistory(events, it.cause) }
          .groupingBy { it }
          .eachCount()
}
