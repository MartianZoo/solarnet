package dev.martianzoo.repl

import org.jline.reader.Candidate

internal class ReplCompletionContext(
    private val repl: ReplSession,
    private val parsedArgs: ReplCompletionArgs,
) {
  private val sources = ReplCompletionSources(repl)

  constructor(
      repl: ReplSession,
      args: String,
  ) : this(repl, ReplCompletionArgs(args))

  val args: String = parsedArgs.text
  val words: List<String> = parsedArgs.words
  val argIndex: Int = parsedArgs.argIndex
  val firstWord: String = parsedArgs.firstWord
  val restAfterFirstWord: String = parsedArgs.restAfterFirstWord
  val hasRestAfterFirstWord: Boolean = parsedArgs.hasRestAfterFirstWord

  fun commandArguments(command: String, args: String): List<ReplCompletion> {
    return repl.commands[command.lowercase()]?.completions(copy(ReplCompletionArgs(args)))
        ?: emptyList()
  }

  fun droppingLeadingWords(count: Int): ReplCompletionContext {
    return copy(parsedArgs.droppingLeadingWords(count))
  }

  fun commandNames(): List<ReplCompletion> = sources.commandNames()
  fun playerNames(includeEngine: Boolean = true): List<ReplCompletion> =
      sources.playerNames(includeEngine)
  fun classNames(): List<ReplCompletion> = sources.classNames()
  fun paymentWords(): List<ReplCompletion> = sources.paymentWords()
  fun playableCardNames(): List<ReplCompletion> = sources.playableCardNames()
  fun phaseNames(): List<ReplCompletion> = sources.phaseNames()
  fun checkpointIds(): List<ReplCompletion> = sources.checkpointIds()
  fun taskIds(): List<ReplCompletion> = sources.taskIds()
  fun fileArguments(): List<ReplCompletion> = ReplPathCompletions.arguments(parsedArgs.currentWord)
  fun bundleSuggestions(): List<ReplCompletion> = sources.bundleSuggestions()

  fun completions(vararg values: String, group: String): List<ReplCompletion> =
      values.map { ReplCompletion(it, group) }

  internal fun petsWords(root: PetsCompletionRoot): List<ReplCompletion> {
    val prefix = fragment(parsedArgs.currentWord)
    val sourceBeforePrefix = args.dropLast(prefix.length)
    return PetsCompletionProbe.words(root, sourceBeforePrefix, prefix, sources.broadPetsCandidates())
  }

  private fun copy(args: ReplCompletionArgs) = ReplCompletionContext(repl, args)

  companion object {
    fun fragment(word: String): String {
      val start = word.indexOfLast { !it.isLetterOrDigit() && it != '_' } + 1
      return word.drop(start)
    }

    fun replaceFragment(word: String, value: String): String =
        word.dropLast(fragment(word).length) + value
  }

}

internal data class ReplCompletionArgs(val text: String) {
  val words: List<String> =
      text.trimStart().let { if (it.isEmpty()) listOf() else it.split(WHITESPACE) }
  val argIndex: Int =
      if (text.endsWithWhitespace()) words.size else words.lastIndex.coerceAtLeast(0)
  val currentWord: String = text.substringAfterLastWhitespace()

  private val trimmed: String = text.trimStart()
  val firstWord: String = trimmed.substringBeforeWhitespace()
  private val restWithLeadingWhitespace: String = trimmed.drop(firstWord.length)
  val hasRestAfterFirstWord: Boolean = restWithLeadingWhitespace.isNotEmpty()
  val restAfterFirstWord: String = restWithLeadingWhitespace.trimStart()

  fun droppingLeadingWords(count: Int): ReplCompletionArgs {
    var rest = text.trimStart()
    repeat(count) { rest = rest.substringAfterWhitespace().trimStart() }
    return ReplCompletionArgs(rest)
  }

  companion object {
    private val WHITESPACE = Regex("\\s+")
  }
}

private fun String.endsWithWhitespace(): Boolean = lastOrNull()?.isWhitespace() == true

private fun String.substringBeforeWhitespace(): String =
    substringBefore(' ').substringBefore('\t')

private fun String.substringAfterWhitespace(): String {
  val firstWhitespace = indexOfFirst { it.isWhitespace() }
  return if (firstWhitespace == -1) "" else drop(firstWhitespace)
}

private fun String.substringAfterLastWhitespace(): String {
  val lastWhitespace = indexOfLast { it.isWhitespace() }
  return if (lastWhitespace == -1) this else drop(lastWhitespace + 1)
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
