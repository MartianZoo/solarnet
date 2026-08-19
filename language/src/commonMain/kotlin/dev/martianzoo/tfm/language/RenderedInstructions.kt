package dev.martianzoo.tfm.language

/** Ordered English clauses describing one instruction tree. */
internal data class RenderedInstructions(val clauses: List<String>) {
  init {
    require(clauses.isNotEmpty())
  }

  internal fun asSentences(): String = clauses.joinToString(" ") { completeSentence(it) }

  internal fun asCoordinatedClause(): String = clauses.joinToString(" and ")
}

internal fun completeSentence(clause: String): String =
    clause.replaceFirstChar(Char::uppercaseChar) + "."

internal fun englishList(parts: List<String>): String =
    when (parts.size) {
      1 -> parts.single()
      2 -> parts.joinToString(" and ")
      else -> parts.dropLast(1).joinToString(", ") + ", and " + parts.last()
    }
