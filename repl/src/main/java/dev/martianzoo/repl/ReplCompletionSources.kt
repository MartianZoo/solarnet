package dev.martianzoo.repl

import dev.martianzoo.data.Player
import dev.martianzoo.tfm.data.CardDefinition

internal class ReplCompletionSources(private val repl: ReplSession) {
  fun commandNames(): List<ReplCompletion> =
      repl.commands.values.map { ReplCompletion(it.name, "commands", it.usage) }

  fun playerNames(includeEngine: Boolean = true): List<ReplCompletion> {
    val players = Player.players(repl.setup.players)
    val eligiblePlayers = if (includeEngine) players else players.filter { it != Player.ENGINE }
    val full = eligiblePlayers.map { ReplCompletion(it.toString(), "players") }
    val short =
        eligiblePlayers.mapNotNull { player ->
          classShortName(player.toString())?.let { ReplCompletion(it, "players", player.toString()) }
        }
    return full + short
  }

  fun classNames(): List<ReplCompletion> =
      repl.game.classes.allClasses().flatMap {
        setOf(
            ReplCompletion(it.className.toString(), "classes", it.docstring),
            ReplCompletion(it.shortName.toString(), "classes", it.className.toString()),
        )
      }

  fun paymentWords(): List<ReplCompletion> {
    val standards = setOf("Megacredit", "Steel", "Titanium", "Plant", "Energy", "Heat")
    return repl.game.classes
        .allClasses()
        .filter { it.className.toString() in standards }
        .flatMap {
          listOf(
              ReplCompletion(it.className.toString(), "resources"),
              ReplCompletion(it.shortName.toString(), "resources", it.className.toString()),
          )
        }
  }

  fun playableCardNames(): List<ReplCompletion> =
      repl.setup
          .allDefinitions()
          .filterIsInstance<CardDefinition>()
          .map { ReplCompletion(it.className.toString(), "cards", it.deck?.name?.lowercase()) }

  fun phaseNames(): List<ReplCompletion> =
      classNames()
          .mapNotNull { it.value.removeSuffixIfPresent("Phase") }
          .filter { it != "Phase" }
          .map { ReplCompletion(it, "phases") }

  fun checkpointIds(): List<ReplCompletion> =
      (0..repl.game.timeline.checkpoint().toString().toInt()).map {
        ReplCompletion(it.toString(), "checkpoints")
      }

  fun taskIds(): List<ReplCompletion> =
      repl.game.tasks.extract {
        ReplCompletion(it.id.toString(), "tasks", it.instruction.toString())
      }

  fun bundleSuggestions(): List<ReplCompletion> {
    val bundles = repl.setup.authority.allBundles
    val maps = repl.setup.authority.marsMapDefinitions.map { it.bundle }.toSet()
    val nonMaps = bundles - maps
    val common = listOf("BM", "BRM", "BRMVX", "BRMVPX", "BRMVPXT", repl.setup.bundleString)
    val generated = maps.flatMap { map -> nonMaps.map { "$it$map" } }
    return (common + generated).map { ReplCompletion(it, "bundle strings") }
  }

  fun broadPetsCandidates(): List<ReplCompletion> =
      classNames() +
          playerNames() +
          syntaxWords(
              "ANY",
              "Anyone",
              "Class",
              "FROM",
              "HAS",
              "MAX",
              "OR",
              "PROD",
              "THEN",
              "This",
              "Ok",
          ) +
          scalarWords()

  private fun scalarWords(): List<ReplCompletion> =
      listOf("1", "2", "3", "X").map { ReplCompletion(it, "Pets scalars") }

  private fun syntaxWords(vararg words: String): List<ReplCompletion> =
      words.map { ReplCompletion(it, "Pets syntax") }

  private fun classShortName(name: String): String? =
      repl.game.classes
          .allClasses()
          .firstOrNull { it.className.toString() == name }
          ?.shortName
          ?.toString()

  private fun String.removeSuffixIfPresent(suffix: String): String? =
      if (endsWith(suffix) && length > suffix.length) removeSuffix(suffix) else null
}
