package dev.martianzoo.script

internal fun splitTrailingQuotedComment(args: String): Pair<String, String?> {
  val match = Regex("""^(.*\S)\s+"([^"]*)"$""").matchEntire(args)
  return if (match == null) args to null
  else match.groupValues[1] to match.groupValues[2].ifEmpty { null }
}

internal fun String.withTrailingQuotedComment(comment: String?): String =
    this + (comment?.let { " \"$it\"" } ?: "")
