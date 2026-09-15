package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.engine.World
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.types.Type
import dev.martianzoo.state.Component
import dev.martianzoo.state.ComponentChange
import dev.martianzoo.state.EventLog

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
            .filter { e -> e.cause?.let { reader.resolve(it.context).narrows(byType) } ?: false }
            .map { it.change }

    fun extracted(component: Component?, change: ComponentChange) =
        component?.let { if (it.type.narrows(ofType)) change.count else 0 } ?: 0

    val pluses = changes.sumOf { extracted(it.gaining, it) }
    val minuses = changes.sumOf { extracted(it.removing, it) }
    return pluses - minuses
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
