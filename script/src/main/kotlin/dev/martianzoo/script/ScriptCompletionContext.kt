package dev.martianzoo.script

public class ScriptCompletionContext
private constructor(
    private val repl: ScriptSession,
    private val parsedArgs: ScriptCompletionArgs,
) {
  private val sources = ScriptCompletionSources(repl)

  constructor(
      repl: ScriptSession,
      args: String,
  ) : this(repl, ScriptCompletionArgs(args))

  val args: String = parsedArgs.text
  val words: List<String> = parsedArgs.words
  val argIndex: Int = parsedArgs.argIndex
  val firstWord: String = parsedArgs.firstWord
  val restAfterFirstWord: String = parsedArgs.restAfterFirstWord
  val hasRestAfterFirstWord: Boolean = parsedArgs.hasRestAfterFirstWord

  fun commandArguments(command: String, args: String): List<ScriptCompletion> {
    return repl.commands[command.lowercase()]
        ?.completions(copy(ScriptCompletionArgs(args)))
        .orEmpty()
  }

  fun droppingLeadingWords(count: Int): ScriptCompletionContext {
    return copy(parsedArgs.droppingLeadingWords(count))
  }

  fun commandNames(): List<ScriptCompletion> = sources.commandNames()

  fun playerNames(includeEngine: Boolean = true): List<ScriptCompletion> =
      sources.playerNames(includeEngine)

  fun classNames(): List<ScriptCompletion> = sources.classNames()

  fun paymentWords(): List<ScriptCompletion> = sources.paymentWords()

  fun playableCardNames(): List<ScriptCompletion> = sources.playableCardNames()

  fun phaseNames(): List<ScriptCompletion> = sources.phaseNames()

  fun checkpointIds(): List<ScriptCompletion> = sources.checkpointIds()

  fun taskIds(): List<ScriptCompletion> = sources.taskIds()

  fun fileArguments(): List<ScriptCompletion> =
      ScriptPathCompletions.arguments(parsedArgs.currentWord)

  fun bundleSuggestions(): List<ScriptCompletion> = sources.bundleSuggestions()

  fun completions(vararg values: String, group: String): List<ScriptCompletion> = values.map {
    ScriptCompletion(it, group)
  }

  internal fun petsWords(root: PetsCompletionRoot): List<ScriptCompletion> {
    val prefix = fragment(parsedArgs.currentWord)
    val sourceBeforePrefix = args.dropLast(prefix.length)
    return PetsCompletionProbe.words(
        root,
        sourceBeforePrefix,
        prefix,
        sources.broadPetsCandidates(),
    )
  }

  private fun copy(args: ScriptCompletionArgs) = ScriptCompletionContext(repl, args)

  companion object {
    fun fragment(word: String): String {
      val start = word.indexOfLast { !it.isLetterOrDigit() && it != '_' } + 1
      return word.drop(start)
    }

    fun replaceFragment(word: String, value: String): String =
        word.dropLast(fragment(word).length) + value
  }
}

internal data class ScriptCompletionArgs(val text: String) {
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

  fun droppingLeadingWords(count: Int): ScriptCompletionArgs {
    var rest = text.trimStart()
    repeat(count) { rest = rest.substringAfterWhitespace().trimStart() }
    return ScriptCompletionArgs(rest)
  }

  companion object {
    private val WHITESPACE = Regex("\\s+")
  }
}

private fun String.endsWithWhitespace(): Boolean = lastOrNull()?.isWhitespace() == true

private fun String.substringBeforeWhitespace(): String = substringBefore(' ').substringBefore('\t')

private fun String.substringAfterWhitespace(): String {
  val firstWhitespace = indexOfFirst { it.isWhitespace() }
  return if (firstWhitespace == -1) "" else drop(firstWhitespace)
}

private fun String.substringAfterLastWhitespace(): String {
  val lastWhitespace = indexOfLast { it.isWhitespace() }
  return if (lastWhitespace == -1) this else drop(lastWhitespace + 1)
}

public data class ScriptCompletion(
    val value: String,
    val group: String? = null,
    val description: String? = null,
    val replaceFragment: Boolean = true,
    val complete: Boolean = true,
) {
  public fun startsWith(prefix: String, ignoreCase: Boolean): Boolean =
      value.startsWith(prefix, ignoreCase)

  public fun replacingFragment(parsedWord: String): ScriptCompletion {
    val candidateValue =
        if (replaceFragment) ScriptCompletionContext.replaceFragment(parsedWord, value) else value
    return copy(value = candidateValue, replaceFragment = false)
  }
}
