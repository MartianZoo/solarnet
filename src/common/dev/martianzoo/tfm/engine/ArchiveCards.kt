package dev.martianzoo.tfm.engine

import dev.martianzoo.agent.Agents
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.state.Component
import dev.martianzoo.state.GameEvent.ChangeEvent
import dev.martianzoo.state.GameRecording
import dev.martianzoo.state.Player

/** Named movements in a Cards header. Absent headers leave ordinary play untracked. */
internal class ArchiveCards(lines: List<String>) {
  private val movements = linkedMapOf<String, MutableList<List<ClassName>>>()

  init {
    for (line in lines) {
      val (movement, names) = line.split(": ", limit = 2).also { require(it.size == 2) }
      require(movement.first() in "+-") { "Cards entries need a movement sign" }
      parse<Expression>(movement.drop(1))
      val cards = names.split(",").map { parse<ClassName>(it.trim()) }
      require(cards.isNotEmpty())
      movements.getOrPut(movement) { mutableListOf() }.add(cards)
    }
  }

  private val selecting = movements.keys.any { "Selecting" in it }

  private lateinit var tracker: CardTracking

  fun start(agents: Agents) {
    tracker =
        CardTracking.start(agents, requireSelectingNames = selecting) { missing ->
          val component =
              if (missing.arriving) missing.event.change.gaining else missing.event.change.removing
          val key = key(checkNotNull(component), missing.arriving)
          val occurrence =
              agents.world.events
                  .changesSinceSetup()
                  .takeWhile { it.ordinal <= missing.event.ordinal }
                  .count {
                    val candidate = if (missing.arriving) it.change.gaining else it.change.removing
                    candidate != null && key(candidate, missing.arriving) == key
                  } - 1
          val cards =
              movements[key]?.getOrNull(occurrence)
                  ?: error("Missing Cards entry for $key at occurrence ${occurrence + 1}")
          require(cards.size == missing.event.change.count) { "Wrong card count for $key" }
          cards.takeLast(missing.count)
        }
  }

  fun finish(agents: Agents) {
    tracker.requireComplete()
    check(movements == fromEvents(agents.world.events.changesSinceSetup(), selecting)) {
      "Cards header does not match the played card movements"
    }
  }

  companion object {
    private fun key(component: Component, arriving: Boolean): String =
        (if (arriving) "+" else "-") + component.expressionFull.toString()

    fun export(recording: GameRecording): List<String> {
      val events = recording.events.filterIsInstance<ChangeEvent>()
      val selecting = events.any {
        it.change.gaining?.expressionFull?.arguments?.any { it.className == cn("Selecting") } ==
            true && it.change.removing == null && it.notes?.contains("Cards: ") == true
      }
      return fromEvents(events, selecting).flatMap { (key, entries) ->
        entries.map { "$key: ${it.joinToString()}" }
      }
    }

    private fun fromEvents(
        events: List<ChangeEvent>,
        selecting: Boolean,
    ): Map<String, MutableList<List<ClassName>>> {
      val result = linkedMapOf<String, MutableList<List<ClassName>>>()
      for (event in events) {
        val names =
            event.notes
                ?.lineSequence()
                ?.singleOrNull { it.startsWith("Cards: ") }
                ?.removePrefix("Cards: ")
                ?.split(", ")
                ?.map { parse<ClassName>(it) } ?: continue
        for ((component, arriving) in
            listOf(event.change.gaining to true, event.change.removing to false)) {
          if (component == null || component.owningPlayer !is Player) continue
          val args = component.expressionFull.arguments
          if (
              args.none {
                it.className == cn("Hand") || (selecting && it.className == cn("Selecting"))
              }
          )
              continue
          result.getOrPut(key(component, arriving)) { mutableListOf() }.add(names)
        }
      }
      return result
    }
  }
}
