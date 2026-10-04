package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.engine.World
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.SystemClasses.CUSTOM_INSTRUCTION
import dev.martianzoo.pets.api.SystemClasses.SIGNAL
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.types.Type
import dev.martianzoo.state.Component
import dev.martianzoo.state.ComponentChange
import dev.martianzoo.state.EventLog
import dev.martianzoo.state.GameEvent.ChangeEvent
import dev.martianzoo.state.GameReader

internal class Summarizer
internal constructor(
    private val events: EventLog,
    private val reader: GameReader,
) {
  internal constructor(game: World) : this(game.events, game.reader)

  internal fun net(byType: String, ofType: String): Int =
      net(parse<Expression>(byType), parse<Expression>(ofType))

  private fun net(byType: Expression, ofType: Expression): Int =
      net(reader.resolve(byType), reader.resolve(ofType))

  private fun net(byType: Type, ofType: Type): Int {
    val changes: List<ComponentChange> =
        events
            .changesSinceSetup()
            .filter { e -> sourceContext(e)?.narrows(byType) == true }
            .map { it.change }

    fun extracted(component: Component?, change: ComponentChange) =
        component?.let { if (it.type.narrows(ofType)) change.count else 0 } ?: 0

    val pluses = changes.sumOf { extracted(it.gaining, it) }
    val minuses = changes.sumOf { extracted(it.removing, it) }
    return pluses - minuses
  }

  /**
   * A computed Signal adds an event without changing which authored component supplied the rule.
   */
  private fun sourceContext(event: ChangeEvent): Type? {
    val computedSignal = reader.resolve(CUSTOM_INSTRUCTION.expression)
    var current = event
    while (true) {
      val cause = current.cause ?: return null
      val context = reader.resolve(cause.context)
      if (!context.narrows(computedSignal)) return context
      current = events.changeAt(cause.triggerEvent) ?: return context
    }
  }

  internal fun signalCount(ofType: String): Int = signalCount(null, reader.resolve(parse(ofType)))

  internal fun signalCount(byType: String, ofType: String): Int =
      signalCount(reader.resolve(parse(byType)), reader.resolve(parse(ofType)))

  private fun signalCount(byType: Type?, ofType: Type): Int =
      events.changesSinceSetup().sumOf { event ->
        val change = event.change
        val signal = change.gaining ?: return@sumOf 0
        if (!signal.type.narrows(reader.resolve(SIGNAL.expression))) return@sumOf 0
        val causeMatches =
            byType == null ||
                event.cause?.let { reader.resolve(it.context).narrows(byType) } == true
        if (causeMatches && signal.type.narrows(ofType)) change.count else 0
      }

  private fun Type.narrows(supertype: Type): Boolean = narrows(supertype, reader)

  private fun allTypesEver(): List<Component> {
    return events
        .changesSinceSetup()
        .flatMap { listOfNotNull(it.change.gaining, it.change.removing) }
        .distinct()
        .sortedBy { it.toString() }
  }
}
