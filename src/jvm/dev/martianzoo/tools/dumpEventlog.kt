package dev.martianzoo.tools

import dev.martianzoo.agent.Agents
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.World
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.api.SystemClasses.HIDDEN
import dev.martianzoo.pets.api.SystemClasses.SYSTEM
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.pets.data.Player
import dev.martianzoo.state.Checkpoint
import dev.martianzoo.state.EventLog
import dev.martianzoo.state.GameEvent.ChangeEvent
import dev.martianzoo.state.GameRecordingJson
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.fake.FakeCanon
import java.nio.file.Files
import java.nio.file.Path

private val gameOptions: Set<ClassName> =
    linkedSetOf(
        cn("TerraformingMars"),
        cn("TharsisMap"),
        cn("CorporateEraExpansion"),
        cn("VenusNextExpansion"),
        cn("PreludeExpansion"),
        cn("Prelude2CardPack"),
        cn("ColoniesExpansion"),
        cn("TurmoilExpansion"),
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

private fun specialSupertypes(reader: GameReader, event: ChangeEvent): String {
  val hidden = reader.classTable.getClass(HIDDEN)
  val system = reader.classTable.getClass(SYSTEM)
  return listOfNotNull(event.change.gaining, event.change.removing)
      .flatMap { component ->
        buildList {
          val changedClass = component.type.rootClass
          if (changedClass.isSubtypeOf(system)) add("System")
          if (changedClass.isSubtypeOf(hidden)) add("Hidden")
        }
      }
      .distinct()
      .joinToString(",")
}

private fun tsv(value: Any?): String = value?.toString().orEmpty()

private fun toTsv(reader: GameReader, event: ChangeEvent): String =
    listOf(
            event.ordinal,
            event.actor,
            event.change.count,
            tsv(event.change.gaining),
            tsv(event.change.removing),
            tsv(event.cause?.context),
            event.cause?.triggerEvent ?: "",
            specialSupertypes(reader, event),
        )
        .joinToString("\t")

private fun dump(reader: GameReader, events: EventLog, output: Path) {
  val changes = events.changesSince(Checkpoint(0))
  Files.createDirectories(output.parent)
  Files.newBufferedWriter(output).use { writer ->
    writer.appendLine(
        "ordinal\tactor\tcount\tgaining\tremoving\tcause_context\tcause_trigger\tspecial_supertypes"
    )
    changes.forEach { writer.appendLine(toTsv(reader, it)) }
  }
  println("Wrote ${changes.size} change events to ${output.toAbsolutePath()}")
}

public fun main(args: Array<String>) {
  when {
    args.size == 2 && args[0].endsWith(".json") -> {
      val text = Files.readString(Path.of(args[0]))
      val config = GameRecordingJson.config(text)
      val catalog =
          if (cn("FakeStuffBundle") in config.includedClassNames) {
            TfmCatalog.compose(Canon, FakeCanon)
          } else {
            Canon
          }
      val premise = catalog.gamePremise(config)
      val world = GameRecordingJson.decode(text, premise).open().world
      dump(world.reader, world.events, Path.of(args[1]))
    }
    args.size == 2 -> {
      createGame(playerCount = 3).let { dump(it.reader, it.events, Path.of(args[0])) }
      createGame(playerCount = 1).let { dump(it.reader, it.events, Path.of(args[1])) }
    }
    else ->
        error(
            "Usage: dumpEventlog <recording.json> <game.tsv> OR " +
                "dumpEventlog <three-player.tsv> <solo.tsv>"
        )
  }
}
