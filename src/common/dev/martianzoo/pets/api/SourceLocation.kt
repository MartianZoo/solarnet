package dev.martianzoo.pets.api

/** An authored source span; offsets are zero-based, lines and columns are one-based. */
public data class SourceLocation(
    public val source: String,
    public val offset: Int,
    public val length: Int = 1,
) {
  public val line: Int
    get() = source.take(offset).count { it == '\n' } + 1

  public val column: Int
    get() = offset - source.lastIndexOf('\n', offset - 1)

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
