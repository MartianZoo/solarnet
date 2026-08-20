package dev.martianzoo.tfm.language

/** Ordered English clauses describing one instruction tree. */
internal data class RenderedInstructions(val clauses: List<Clause>) {
  init {
    require(clauses.isNotEmpty())
  }

  internal fun asSentences(): String = clauses.joinToString(" ") { Sentence(it).linearize() }

  internal fun asCoordinatedClause(): String = clauses.joinToString(" and ") { it.linearize() }
}

internal fun completeSentence(clause: String, punctuation: String = "."): String =
    clause.replaceFirstChar(Char::uppercaseChar) + punctuation

internal fun englishList(parts: List<String>): String =
    when (parts.size) {
      1 -> parts.single()
      2 -> parts.joinToString(" and ")
      else -> parts.dropLast(1).joinToString(", ") + ", and " + parts.last()
    }

internal fun englishAlternatives(parts: List<String>): String =
    when (parts.size) {
      2 -> parts.joinToString(" or ")
      else -> parts.dropLast(1).joinToString(", ") + ", or " + parts.last()
    }
