package dev.martianzoo.script

import dev.martianzoo.data.Player
import dev.martianzoo.tfm.data.CardDefinition

internal class ScriptCompletionSources(private val repl: ScriptSession) {
  fun commandNames(): List<ScriptCompletion> =
      repl.commands.values.map { ScriptCompletion(it.name, "commands", it.usage) }

  fun playerNames(includeEngine: Boolean = true): List<ScriptCompletion> {
    val players = Player.players(repl.setup.players)
    val eligiblePlayers = if (includeEngine) players else players.filter { it != Player.ENGINE }
    val full = eligiblePlayers.map { ScriptCompletion(it.toString(), "players") }
    val short =
        eligiblePlayers.mapNotNull { player ->
          classShortName(player.toString())?.let { ScriptCompletion(it, "players", player.toString()) }
        }
    return full + short
  }

  fun classNames(): List<ScriptCompletion> =
      repl.game.classes.allClasses().flatMap {
        setOf(
            ScriptCompletion(it.className.toString(), "classes", it.docstring),
            ScriptCompletion(it.shortName.toString(), "classes", it.className.toString()),
        )
      }

  fun paymentWords(): List<ScriptCompletion> {
    val standards = setOf("Megacredit", "Steel", "Titanium", "Plant", "Energy", "Heat")
    return repl.game.classes
        .allClasses()
        .filter { it.className.toString() in standards }
        .flatMap {
          listOf(
              ScriptCompletion(it.className.toString(), "resources"),
              ScriptCompletion(it.shortName.toString(), "resources", it.className.toString()),
          )
        }
  }

  fun playableCardNames(): List<ScriptCompletion> =
      repl.setup
          .allDefinitions()
          .filterIsInstance<CardDefinition>()
          .map { ScriptCompletion(it.className.toString(), "cards", it.deck?.name?.lowercase()) }

  fun phaseNames(): List<ScriptCompletion> =
      classNames()
          .mapNotNull { it.value.removeSuffixIfPresent("Phase") }
          .filter { it != "Phase" }
          .map { ScriptCompletion(it, "phases") }

  fun checkpointIds(): List<ScriptCompletion> =
      (0..repl.game.timeline.checkpoint().toString().toInt()).map {
        ScriptCompletion(it.toString(), "checkpoints")
      }

  fun taskIds(): List<ScriptCompletion> =
      repl.game.tasks.extract {
        ScriptCompletion(it.id.toString(), "tasks", it.instruction.toString())
      }

  fun bundleSuggestions(): List<ScriptCompletion> {
    val bundles = repl.setup.authority.allBundles
    val maps = repl.setup.authority.marsMapDefinitions.map { it.bundle }.toSet()
    val nonMaps = bundles - maps
    val common = listOf("BM", "BRM", "BRMVX", "BRMVPX", "BRMVPXT", repl.setup.bundleString)
    val generated = maps.flatMap { map -> nonMaps.map { "$it$map" } }
    return (common + generated).map { ScriptCompletion(it, "bundle strings") }
  }

  fun broadPetsCandidates(): List<ScriptCompletion> =
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

  private fun scalarWords(): List<ScriptCompletion> =
      listOf("1", "2", "3", "X").map { ScriptCompletion(it, "Pets scalars") }

  private fun syntaxWords(vararg words: String): List<ScriptCompletion> =
      words.map { ScriptCompletion(it, "Pets syntax") }

  private fun classShortName(name: String): String? =
      repl.game.classes
          .allClasses()
          .firstOrNull { it.className.toString() == name }
          ?.shortName
          ?.toString()

  private fun String.removeSuffixIfPresent(suffix: String): String? =
      if (endsWith(suffix) && length > suffix.length) removeSuffix(suffix) else null
}
