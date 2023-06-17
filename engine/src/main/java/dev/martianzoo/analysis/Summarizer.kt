package dev.martianzoo.analysis

import dev.martianzoo.api.GameReader
import dev.martianzoo.api.Type
import dev.martianzoo.data.GameEvent.ChangeEvent.StateChange
import dev.martianzoo.engine.EventLog
import dev.martianzoo.engine.Game
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.Expression
import javax.inject.Inject

public class Summarizer @Inject constructor(val events: EventLog, val reader: GameReader) {
  constructor(game: Game) : this(game.events, game.reader)

  fun net(byType: String, ofType: String): Int = net(parse<Expression>(byType), parse(ofType))

  fun net(byType: Expression, ofType: Expression): Int =
      net(reader.resolve(byType), reader.resolve(ofType))

  fun net(byType: Type, ofType: Type): Int {
    val changes: List<StateChange> =
        events
            .changesSinceSetup()
            .filter { e -> e.cause?.let { reader.resolve(it.context).narrows(byType) } ?: false }
            .map { it.change }

    fun extracted(expr: Expression?, change: StateChange) =
        expr?.let { if (reader.resolve(it).narrows(ofType)) change.count else 0 } ?: 0

    val pluses = changes.sumOf { extracted(it.gaining, it) }
    val minuses = changes.sumOf { extracted(it.removing, it) }
    return pluses - minuses
  }

  fun allTypesEver(): List<Expression> {
    return events
        .changesSinceSetup()
        .flatMap { listOfNotNull(it.change.gaining, it.change.removing) }
        .distinct()
        .sortedBy { it.toString() }
  }
}
