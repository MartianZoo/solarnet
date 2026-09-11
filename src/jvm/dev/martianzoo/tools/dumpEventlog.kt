package dev.martianzoo.tools

import dev.martianzoo.agent.Agents
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.Timeline.Checkpoint
import dev.martianzoo.engine.World
import dev.martianzoo.pets.api.SystemClasses.HIDDEN
import dev.martianzoo.pets.api.SystemClasses.SYSTEM
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.pets.data.GameEvent.ChangeEvent
import dev.martianzoo.pets.data.Player
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.web.gameviewer.games.OtbGame20260828
import java.nio.file.Files
import java.nio.file.Path

private val gameOptions: Set<ClassName> =
    linkedSetOf(
        cn("TerraformingMars"),
        cn("TharsisMap"),
        cn("CorporateEraExpansion"),
        cn("VenusNextExpansion"),
        cn("Prelude2Expansion"),
        cn("ColoniesExpansion"),
        cn("TurmoilCardPack"),
        cn("PromoCardPack"),
        cn("FakeCardsCardPack"),
    )

private fun createGame(playerCount: Int): World {
  val colonyCount = if (playerCount == 1) 4 else if (playerCount == 2) 5 else playerCount + 2
  val colonies = Canon.colonyTileClassNames.sorted().take(colonyCount)
  val premise =
      Canon.gamePremise(
          GameConfig.create(
              included = gameOptions + colonies,
              playerNames = (1..playerCount).map { cn("Player$it") },
          )
      )
  return Engine.newGame(premise).also { game ->
    val agents = Agents(game)
    TfmWorkflow.Stepwise(agents).setupPhase()
    val players = game.actors.filterIsInstance<Player>()
    players.forEach { player -> agents[player].doTask("-6 ProjectCard<Hand>") }
    if (playerCount == 1) {
      agents.tfm(players.first()).doTask("-ColonyTileSelection<Class<${colonies.first()}>>")
    }
    TfmWorkflow.Stepwise(agents).corporationPhase()
    agents.tfm(players.first()).playCorp(cn("InterplanetaryCinematics"), buyCards = 4)
  }
}

private fun specialSupertypes(game: World, event: ChangeEvent): String {
  val hidden = game.classTable.getClass(HIDDEN)
  val system = game.classTable.getClass(SYSTEM)
  return listOfNotNull(event.change.gaining, event.change.removing)
      .flatMap { expression ->
        buildList {
          val changedClass = game.classTable.resolve(expression).rootClass
          if (changedClass.isSubtypeOf(system)) add("System")
          if (changedClass.isSubtypeOf(hidden)) add("Hidden")
        }
      }
      .distinct()
      .joinToString(",")
}

private fun tsv(expression: Expression?): String = expression?.toString().orEmpty()

private fun toTsv(game: World, event: ChangeEvent): String =
    listOf(
            event.ordinal,
            event.actor,
            event.change.count,
            tsv(event.change.gaining),
            tsv(event.change.removing),
            tsv(event.cause?.context),
            event.cause?.triggerEvent ?: "",
            specialSupertypes(game, event),
        )
        .joinToString("\t")

private fun dump(game: World, output: Path) {
  val changes = game.events.changesSince(Checkpoint(0))
  Files.createDirectories(output.parent)
  Files.newBufferedWriter(output).use { writer ->
    writer.appendLine(
        "ordinal\tactor\tcount\tgaining\tremoving\tcause_context\tcause_trigger\tspecial_supertypes"
    )
    changes.forEach { writer.appendLine(toTsv(game, it)) }
  }
  println("Wrote ${changes.size} change events to ${output.toAbsolutePath()}")
}

public fun main(args: Array<String>) {
  when (args.size) {
    1 -> dump(OtbGame20260828().record().world, Path.of(args.single()))
    2 -> {
      dump(createGame(playerCount = 3), Path.of(args[0]))
      dump(createGame(playerCount = 1), Path.of(args[1]))
    }
    else ->
        error(
            "Usage: dumpEventlog <otb-game.tsv> OR " + "dumpEventlog <three-player.tsv> <solo.tsv>"
        )
  }
}
