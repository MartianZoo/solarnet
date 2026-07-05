package dev.martianzoo.repl

import dev.martianzoo.data.Player
import dev.martianzoo.pets.Parsing
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.tfm.data.CardDefinition
import java.io.File
import org.jline.reader.Candidate
import kotlin.reflect.KClass

internal class ReplCompletionContext(
    private val repl: ReplSession,
    val args: String,
    val words: List<String>,
    val argIndex: Int,
) {
  fun commandArguments(command: String, args: String): List<ReplCompletion> {
    val words = args.splitWordsForCompletion()
    val argIndex =
        if (args.endsWithWhitespace()) words.size else words.lastIndex.coerceAtLeast(0)
    return repl.commands[command.lowercase()]?.completions(copy(args, words, argIndex)) ?: emptyList()
  }

  fun droppingLeadingWords(count: Int): ReplCompletionContext {
    var rest = args.trimStart()
    repeat(count) { rest = rest.substringAfterWhitespace().trimStart() }
    val words = rest.splitWordsForCompletion()
    val argIndex =
        if (rest.endsWithWhitespace()) words.size else words.lastIndex.coerceAtLeast(0)
    return copy(rest, words, argIndex)
  }

  fun commandNames(): List<ReplCompletion> =
      repl.commands.values.map { ReplCompletion(it.name, "commands", it.usage) } +
          listOf(
              ReplCompletion("exit", "commands", "exit"),
              ReplCompletion("rebuild", "commands", "rebuild and restart"),
          )

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

  fun fileArguments(): List<ReplCompletion> {
    val pathText = args.substringAfterLastWhitespace()
    val path = File(pathText.ifEmpty { "." })
    val dir =
        if (pathText.endsWith(File.separator) || path.isDirectory) {
          path
        } else {
          path.parentFile ?: File(".")
        }
    val prefix = if (pathText.endsWith(File.separator) || path.isDirectory) "" else path.name
    val base =
        if (pathText.endsWith(File.separator)) {
          pathText
        } else {
          pathText.substringBeforeLast(File.separator, "")
        }
    val prefixWithSlash = if (base.isEmpty()) "" else "$base${File.separator}"

    return dir
        .listFiles()
        ?.filter { it.name.startsWith(prefix, ignoreCase = true) }
        ?.map {
          val value = prefixWithSlash + it.name + if (it.isDirectory) File.separator else ""
          ReplCompletion(
              value,
              if (it.isDirectory) "directories" else "files",
              replaceFragment = false,
              complete = !it.isDirectory,
          )
        }
        ?: emptyList()
  }

  fun bundleSuggestions(): List<ReplCompletion> {
    val bundles = repl.setup.authority.allBundles
    val maps = repl.setup.authority.marsMapDefinitions.map { it.bundle }.toSet()
    val nonMaps = bundles - maps
    val common = listOf("BM", "BRM", "BRMVX", "BRMVPX", "BRMVPXT", repl.setup.bundleString)
    val generated = maps.flatMap { map -> nonMaps.map { "$it$map" } }
    return (common + generated).map { ReplCompletion(it, "bundle strings") }
  }

  fun completions(vararg values: String, group: String): List<ReplCompletion> =
      values.map { ReplCompletion(it, group) }

  internal fun petsWords(root: PetsCompletionRoot): List<ReplCompletion> {
    val prefix = fragment(args.substringAfterLastWhitespace())
    val sourceBeforePrefix = args.dropLast(prefix.length)
    return broadPetsCandidates()
        .filter { it.startsWith(prefix, ignoreCase = true) }
        .filter { root.accepts(sourceBeforePrefix, it.value) }
  }

  private fun broadPetsCandidates(): List<ReplCompletion> =
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

  private fun PetsCompletionRoot.accepts(sourceBeforePrefix: String, candidate: String): Boolean =
      parserTypes.any { it.acceptsPetsCompletion(sourceBeforePrefix, candidate) }

  private fun KClass<out PetNode>.acceptsPetsCompletion(
      sourceBeforePrefix: String,
      candidate: String,
  ): Boolean {
    val base = sourceBeforePrefix + candidate
    return completionFillers.any { filler ->
      val filled = base + filler
      val probe = filled + closingSuffix(filled)
      parses(probe)
    }
  }

  private fun KClass<out PetNode>.parses(source: String): Boolean =
      try {
        Parsing.parse(this, source)
        true
      } catch (e: Exception) {
        false
      }

  private fun closingSuffix(source: String): String {
    val closers = ArrayDeque<Char>()
    for (c in source) {
      when (c) {
        '(' -> closers.addFirst(')')
        '[' -> closers.addFirst(']')
        '<' -> closers.addFirst('>')
        ')' -> closers.removeFirstIf(')')
        ']' -> closers.removeFirstIf(']')
        '>' -> closers.removeFirstIf('>')
      }
    }
    return closers.joinToString("")
  }

  private fun ArrayDeque<Char>.removeFirstIf(c: Char) {
    if (firstOrNull() == c) removeFirst()
  }

  private fun scalarWords(): List<ReplCompletion> =
      listOf("1", "2", "3", "X").map { ReplCompletion(it, "Pets scalars") }

  private fun syntaxWords(vararg words: String): List<ReplCompletion> =
      words.map { ReplCompletion(it, "Pets syntax") }

  private fun copy(args: String, words: List<String>, argIndex: Int) =
      ReplCompletionContext(repl, args, words, argIndex)

  private fun classShortName(name: String): String? =
      repl.game.classes
          .allClasses()
          .firstOrNull { it.className.toString() == name }
          ?.shortName
          ?.toString()

  private fun String.substringAfterLastWhitespace(): String {
    val lastWhitespace = indexOfLast { it.isWhitespace() }
    return if (lastWhitespace == -1) this else drop(lastWhitespace + 1)
  }

  private fun String.substringAfterWhitespace(): String {
    val firstWhitespace = indexOfFirst { it.isWhitespace() }
    return if (firstWhitespace == -1) "" else drop(firstWhitespace)
  }

  private fun String.splitWordsForCompletion(): List<String> =
      trimStart().let { if (it.isEmpty()) listOf() else it.split(Regex("\\s+")) }

  private fun String.endsWithWhitespace() = lastOrNull()?.isWhitespace() == true

  private fun String.removeSuffixIfPresent(suffix: String): String? =
      if (endsWith(suffix) && length > suffix.length) removeSuffix(suffix) else null

  companion object {
    fun fragment(word: String): String {
      val start = word.indexOfLast { !it.isLetterOrDigit() && it != '_' } + 1
      return word.drop(start)
    }

    fun replaceFragment(word: String, value: String): String =
        word.dropLast(fragment(word).length) + value
  }

  private enum class PetsCompletionRoot {
    ANY,
    EXPRESSION,
    INSTRUCTION,
    METRIC,
    REQUIREMENT,
    ;

    val parserTypes: List<KClass<out PetNode>>
      get() =
          when (this) {
            ANY -> listOf(Instruction::class, Expression::class, Metric::class, Requirement::class)
            EXPRESSION -> listOf(Expression::class)
            INSTRUCTION -> listOf(Instruction::class)
            METRIC -> listOf(Metric::class)
            REQUIREMENT -> listOf(Requirement::class)
          }
  }

  private val completionFillers =
      listOf(
          "",
          " Plant",
          " 1",
          " 1 Plant",
          "[Plant]",
          " FROM Plant",
          " OR Plant",
          " THEN Plant",
          " MAX 1",
      )
}

internal data class ReplCompletion(
    val value: String,
    val group: String? = null,
    val description: String? = null,
    val replaceFragment: Boolean = true,
    val complete: Boolean = true,
) {
  fun startsWith(prefix: String, ignoreCase: Boolean): Boolean = value.startsWith(prefix, ignoreCase)

  fun toCandidate(parsedWord: String): Candidate {
    val candidateValue =
        if (replaceFragment) ReplCompletionContext.replaceFragment(parsedWord, value) else value
    return Candidate(candidateValue, candidateValue, group, description, null, null, complete)
  }
}
