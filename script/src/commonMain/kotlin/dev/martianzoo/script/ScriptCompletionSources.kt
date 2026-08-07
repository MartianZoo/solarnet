package dev.martianzoo.script

import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.Player
import dev.martianzoo.tfm.api.tfmRuleset
import dev.martianzoo.tfm.data.CardDefinition

internal class ScriptCompletionSources(private val repl: ScriptSession) {
  fun commandNames(): List<ScriptCompletion> =
      repl.commands.values.map { ScriptCompletion(it.name, "commands", it.usage) }

  fun playerNames(includeEngine: Boolean = true): List<ScriptCompletion> {
    val players = Player.players(repl.setup.players)
    val eligiblePlayers = if (includeEngine) players + ENGINE else players
    val full = eligiblePlayers.map { ScriptCompletion(it.toString(), "players") }
    val short = eligiblePlayers.mapNotNull { player ->
      classShortName(player.toString())?.let { ScriptCompletion(it, "players", player.toString()) }
    }
    return full + short
  }

  fun classNames(): List<ScriptCompletion> =
      repl.game.classTable.allClasses().flatMap {
        listOfNotNull(
                ScriptCompletion(it.className.toString(), "classes", it.docstring),
                classShortName(it.className.toString())?.let { shortName ->
                  ScriptCompletion(shortName, "classes", it.className.toString())
                },
            )
            .distinct()
      }

  fun paymentWords(): List<ScriptCompletion> {
    val standards = setOf("Megacredit", "Steel", "Titanium", "Plant", "Energy", "Heat")
    return repl.game.classTable
        .allClasses()
        .filter { it.className.toString() in standards }
        .flatMap {
          listOfNotNull(
              ScriptCompletion(it.className.toString(), "resources"),
              classShortName(it.className.toString())?.let { shortName ->
                ScriptCompletion(shortName, "resources", it.className.toString())
              },
          )
        }
  }

  fun playableCardNames(): List<ScriptCompletion> =
      repl.game.reader.tfmRuleset.allDefinitions.filterIsInstance<CardDefinition>().map {
        ScriptCompletion(it.className.toString(), "cards", it.deck?.name?.lowercase())
      }

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
      repl.selectableTasks().map { (label, task) ->
        ScriptCompletion(label, "tasks", task.instruction.toString())
      }

  fun optionSuggestions(): List<ScriptCompletion> {
    return OptionCodeTranslation.suggestions(repl.setup).map {
      ScriptCompletion(it, "option codes")
    }
  }

  fun broadPetsCandidates(): List<ScriptCompletion> =
      classNames() +
          playerNames() +
          syntaxWords(
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

  private fun syntaxWords(vararg words: String): List<ScriptCompletion> = words.map {
    ScriptCompletion(it, "Pets syntax")
  }

  private fun classShortName(name: String): String? =
      repl.game.classSynonyms.mappings.entries
          .singleOrNull { it.value.toString() == name }
          ?.key
          ?.toString()
          ?: repl.game.classTable
              .allClasses()
              .firstOrNull { it.className.toString() == name && it.shortName != it.className }
              ?.shortName
              ?.toString()

  private fun String.removeSuffixIfPresent(suffix: String): String? =
      if (endsWith(suffix) && length > suffix.length) removeSuffix(suffix) else null
}
