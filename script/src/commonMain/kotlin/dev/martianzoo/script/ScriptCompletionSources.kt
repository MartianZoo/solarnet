package dev.martianzoo.script

import dev.martianzoo.pets.data.Actor.Companion.ENGINE
import dev.martianzoo.pets.data.Player
import dev.martianzoo.tfm.canon.tfmCatalog

internal class ScriptCompletionSources(private val repl: ScriptSession) {
  fun commandNames(): List<ScriptCompletion> =
      repl.commands.values.map { ScriptCompletion(it.name, "commands", it.usage) }

  fun playerNames(includeEngine: Boolean = true): List<ScriptCompletion> {
    val players = repl.game.actors.filterIsInstance<Player>()
    val eligiblePlayers = if (includeEngine) players + ENGINE else players
    return eligiblePlayers.map {
      ScriptCompletion(repl.game.vocabulary.petsName(it.className).toString(), "players")
    }
  }

  fun classNames(): List<ScriptCompletion> =
      repl.game.classTable.allClasses().map {
        ScriptCompletion(
            repl.game.vocabulary.petsName(it.className).toString(),
            "classes",
            it.docstring,
        )
      }

  fun paymentWords(): List<ScriptCompletion> {
    val standards = setOf("Megacredit", "Steel", "Titanium", "Plant", "Energy", "Heat")
    return repl.game.classTable
        .allClasses()
        .filter { it.className.toString() in standards }
        .map {
          ScriptCompletion(repl.game.vocabulary.petsName(it.className).toString(), "resources")
        }
  }

  fun playableCardNames(): List<ScriptCompletion> =
      repl.game.reader.tfmCatalog.cardDefinitions.map { sourceCard ->
        val card = repl.game.reader.tfmCatalog.card(sourceCard.className)
        ScriptCompletion(
            repl.game.vocabulary.petsName(card.className).toString(),
            "cards",
            card.deck?.name?.lowercase(),
        )
      }

  fun actionCardNames(): List<ScriptCompletion> =
      repl.game.reader.tfmCatalog.cardDefinitions
          .map { repl.game.reader.tfmCatalog.card(it.className) }
          .filter { card -> card.actions.isNotEmpty() }
          .map { card ->
            ScriptCompletion(
                repl.game.vocabulary.petsName(card.className).toString(),
                "action cards",
            )
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

  fun optionSuggestions(): List<ScriptCompletion> {
    return OptionCodeTranslation.suggestions(repl.optionCodes).map {
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

  private fun String.removeSuffixIfPresent(suffix: String): String? =
      if (endsWith(suffix) && length > suffix.length) removeSuffix(suffix) else null
}
