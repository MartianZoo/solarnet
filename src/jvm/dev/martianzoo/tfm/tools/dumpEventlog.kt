package dev.martianzoo.tfm.tools

import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.engine.World
import dev.martianzoo.pets.api.SystemClasses.ICONIC
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.Checkpoint
import dev.martianzoo.state.EventLog
import dev.martianzoo.state.GameEvent.ChangeEvent
import dev.martianzoo.state.GameFileJson
import dev.martianzoo.state.GameReader
import dev.martianzoo.state.Player
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.engine.TfmEngine
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.fake.FakeCanon
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.runBlocking

private val gameOptions: Set<ClassName> =
    linkedSetOf(
        cn("TerraformingMars"),
        cn("TharsisMap"),
        cn("CorporateEraExpansion"),
        cn("VenusNextExpansion"),
        cn("PreludeExpansion"),
        cn("ColoniesExpansion"),
        cn("TurmoilExpansion"),
    )

private fun createGame(playerCount: Int): World {
  val colonyCount = if (playerCount == 1) 4 else if (playerCount == 2) 5 else playerCount + 2
  val colonies = Canon.colonyTileClassNames.sorted().take(colonyCount)
  val premise =
      TfmCatalog(Canon, FakeCanon)
          .gamePremise(
              GameConfig.create(
                  included = gameOptions + colonies,
                  includedCardBundles =
                      listOf("promos", "prelude2") + FakeCanon.bundles.map { it.bundleName },
                  playerNames = (1..playerCount).map { cn("Player$it") },
              )
          )
  return TfmEngine.newGame(premise).also { game ->
    val agents = TfmEngine.agents(game)
    agents.tfm(ADMIN).startGame()
    val players = game.actors.filterIsInstance<Player>()
    if (playerCount == 1) {
      agents.tfm(players.first()).doTask("-SelectedColonyTile<Class<${colonies.first()}>>")
    }
    players.forEachIndexed { index, player ->
      agents.tfm(player).keepStartingProjects(if (index == 0) 4 else 0)
    }
    agents.tfm(players.first()).playCorp(cn("InterplanetaryCinematics"))
  }
}

private fun isIconic(reader: GameReader, event: ChangeEvent): Boolean {
  val iconic = reader.classTable.getClass(ICONIC)
  return listOfNotNull(event.change.gaining, event.change.removing).any {
    it.type.rootClass.isSubtypeOf(iconic)
  }
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
            isIconic(reader, event),
        )
        .joinToString("\t")

private fun dump(reader: GameReader, events: EventLog, output: Path) {
  val changes = events.changesSince(Checkpoint(0))
  Files.createDirectories(output.parent)
  Files.newBufferedWriter(output).use { writer ->
    writer.appendLine(
        "ordinal\tactor\tcount\tgaining\tremoving\tcause_context\tcause_trigger\tIconic?"
    )
    changes.forEach { writer.appendLine(toTsv(reader, it)) }
  }
  println("Wrote ${changes.size} change events to ${output.toAbsolutePath()}")
}

public fun main(args: Array<String>) {
  when {
    args.size == 2 && args[0].endsWith(".json") -> {
      val text = Files.readString(Path.of(args[0]))
      val document = GameFileJson.parse(text)
      val config = document.config
      val catalog =
          if (
              FakeCanon.bundles.any { it.bundleName in config.includedCardBundles } ||
                  config.includedClassNames.any { it in FakeCanon.allClassNames }
          ) {
            TfmCatalog(Canon, FakeCanon)
          } else {
            Canon
          }
      val world = runBlocking { document.open(catalog).world }
      dump(world.reader, world.events, Path.of(args[1]))
    }
    args.size == 2 -> {
      createGame(playerCount = 3).let { dump(it.reader, it.events, Path.of(args[0])) }
      createGame(playerCount = 1).let { dump(it.reader, it.events, Path.of(args[1])) }
    }
    else ->
        error(
            "Usage: dumpEventlog <game.json> <game.tsv> OR " +
                "dumpEventlog <three-player.tsv> <solo.tsv>"
        )
  }
}
