package dev.martianzoo.script

public class ScriptCompletionContext
private constructor(
    private val repl: ScriptSession,
    private val parsedArgs: ScriptCompletionArgs,
) {
  private val sources = ScriptCompletionSources(repl)

  internal constructor(
      repl: ScriptSession,
      args: String,
  ) : this(repl, ScriptCompletionArgs(args))

  internal val args: String = parsedArgs.text
  internal val words: List<String> = parsedArgs.words
  internal val argIndex: Int = parsedArgs.argIndex
  public val currentWord: String = parsedArgs.currentWord
  internal val firstWord: String = parsedArgs.firstWord
  internal val restAfterFirstWord: String = parsedArgs.restAfterFirstWord
  internal val hasRestAfterFirstWord: Boolean = parsedArgs.hasRestAfterFirstWord

  internal fun commandArguments(command: String, args: String): List<ScriptCompletion> {
    return repl.commands[command.lowercase()]
        ?.completions(copy(ScriptCompletionArgs(args)))
        .orEmpty()
  }

  internal fun droppingLeadingWords(count: Int): ScriptCompletionContext {
    return copy(parsedArgs.droppingLeadingWords(count))
  }

  internal fun commandNames(): List<ScriptCompletion> = sources.commandNames()

  internal fun playerNames(includeEngine: Boolean = true): List<ScriptCompletion> =
      sources.playerNames(includeEngine)

  internal fun classNames(): List<ScriptCompletion> = sources.classNames()

  internal fun paymentWords(): List<ScriptCompletion> = sources.paymentWords()

  internal fun playableCardNames(): List<ScriptCompletion> = sources.playableCardNames()

  internal fun phaseNames(): List<ScriptCompletion> = sources.phaseNames()

  internal fun checkpointIds(): List<ScriptCompletion> = sources.checkpointIds()

  internal fun taskIds(): List<ScriptCompletion> = sources.taskIds()

  internal fun optionSuggestions(): List<ScriptCompletion> = sources.optionSuggestions()

  internal fun completions(vararg values: String, group: String): List<ScriptCompletion> =
      values.map {
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

  internal companion object {
    internal fun fragment(word: String): String {
      val start = word.indexOfLast { !it.isLetterOrDigit() && it != '_' } + 1
      return word.drop(start)
    }

    internal fun replaceFragment(word: String, value: String): String =
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
