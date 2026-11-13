package dev.martianzoo.tfm.tools

import dev.martianzoo.agent.Agents
import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.engine.World
import dev.martianzoo.pets.api.SystemClasses.ICONIC
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.EventLog
import dev.martianzoo.state.GameEvent.ChangeEvent
import dev.martianzoo.state.GameFileJson
import dev.martianzoo.state.GameReader
import dev.martianzoo.state.Player
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.canon.soloTileSetup
import dev.martianzoo.tfm.engine.TfmEngine
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
  val preparedPremise =
      if (playerCount == 1) {
        soloTileSetup(
            premise,
            listOf("Tharsis_4_1", "Tharsis_5_1", "Tharsis_5_8", "Tharsis_5_7").map(::cn),
        )
      } else premise
  val deck = TfmEngine.globalEventDeck(preparedPremise).shuffled(kotlin.random.Random(0))
  return TfmEngine.newGame(preparedPremise.copy(drawOrder = deck)).also { game ->
    val agents = Agents(game)
    agents[ADMIN].beginOperation("SetupPhase FROM BootstrapPhase")
    val players = game.actors.filterIsInstance<Player>()
    if (playerCount == 1) {
      agents[players.first()].doTask("-SelectedColonyTile<Class<${colonies.first()}>>")
    }
    players.forEachIndexed { index, player ->
      agents[player].doTask(
          if (index == 0) "-6 ProjectCard<Selecting>" else "-10 ProjectCard<Selecting>"
      )
    }
    agents[players.first()].doTask(
        "PlayCard<Class<CorporationCard>, Class<InterplanetaryCinematics>>"
    )
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
  val changes = events.changesSince(0)
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
