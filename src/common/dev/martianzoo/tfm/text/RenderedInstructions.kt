package dev.martianzoo.tfm.text

/** Ordered English clauses describing one instruction tree. */
internal data class RenderedInstructions(val clauses: List<Clause>) {
  init {
    require(clauses.isNotEmpty())
  }

  internal fun asSentences(): EnglishText = EnglishText.join(clauses.map { Sentence(it).asText() })

  internal fun asCoordinatedClause(): Clause = clauses.reduce { preceding, next ->
    Clause.Coordinated(Coordination(listOf(preceding, next), Conjunction.AND))
  }

  internal val unresolved: List<Unresolved>
    get() = clauses.flatMap(Clause::unresolved)
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
