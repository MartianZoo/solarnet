package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.api.GameReader
import dev.martianzoo.data.GameEvent.ChangeEvent.StateChange
import dev.martianzoo.engine.EventLog
import dev.martianzoo.engine.World
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Vocabulary
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.types.Type

internal class Summarizer
internal constructor(
    private val events: EventLog,
    private val reader: GameReader,
    private val vocabulary: Vocabulary,
) {
  internal constructor(game: World) : this(game.events, game.reader, game.vocabulary)

  public fun net(byType: String, ofType: String): Int =
      net(
          vocabulary.canonicalize(parse<Expression>(byType)),
          vocabulary.canonicalize(parse<Expression>(ofType)),
      )

  private fun net(byType: Expression, ofType: Expression): Int =
      net(reader.resolve(byType), reader.resolve(ofType))

  private fun net(byType: Type, ofType: Type): Int {
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

  private fun Type.narrows(supertype: Type): Boolean = narrows(supertype, reader)

  private fun allTypesEver(): List<Expression> {
    return events
        .changesSinceSetup()
        .flatMap { listOfNotNull(it.change.gaining, it.change.removing) }
        .distinct()
        .sortedBy { it.toString() }
  }
}
