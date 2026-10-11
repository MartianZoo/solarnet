package dev.martianzoo.pets.api

/**
 * An authored source span. [offset] is zero-based within [source], while [line] and [column] are
 * one-based. [length] selects the offending text and defaults to a single character; a span that
 * reaches past the end of [source] is clipped by [text]. Callers supply a nonnegative offset within
 * the source and a nonnegative length.
 */
public data class SourceLocation(
    public val source: String,
    public val offset: Int,
    public val length: Int = 1,
) {
  /** Line containing [offset], counting newline characters before it. */
  public val line: Int
    get() = source.take(offset).count { it == '\n' } + 1

  /** Column containing [offset], measured after the most recent newline. */
  public val column: Int
    get() = offset - source.lastIndexOf('\n', offset - 1)

  /** Source substring selected by this span, clipped to the source's end. */
  public val text: String
    get() = source.substring(offset, (offset + length).coerceAtMost(source.length))

  internal fun describe(message: String): String {
    val lineStart = offset - column + 1
    val lineEnd = source.indexOf('\n', offset).takeIf { it >= 0 } ?: source.length
    val excerpt = source.substring(lineStart, lineEnd).trimEnd('\r')
    val indent = excerpt.take(column - 1).map { if (it == '\t') '\t' else ' ' }.joinToString("")
    return "$message at $line:$column\n$excerpt\n$indent^"
  }
}
