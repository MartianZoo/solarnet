package dev.martianzoo.tfm.text

/**
 * A clause modifier kept separate so factoring cannot cross different destinations or conditions.
 */
internal sealed interface Modifier {
  val separator: String
    get() = " "

  fun linearize(): String

  data class Phrase(private val text: String) : Modifier {
    override fun linearize(): String = text
  }

  data class Parenthetical(private val text: String) : Modifier {
    override fun linearize(): String = "($text)"
  }

  data class Supplement(val text: String) : Modifier {
    override val separator: String = ", "

    override fun linearize(): String = text
  }

  data class Relation(val phrase: String, val target: NounPhrase) : Modifier {
    override fun linearize(): String = "$phrase ${target.linearize()}"
  }

  data class Between(val first: NounPhrase, val second: NounPhrase) : Modifier {
    override fun linearize(): String = "between ${first.linearize()} and ${second.linearize()}"
  }

  data class Per(val metric: NounPhrase) : Modifier {
    override fun linearize(): String = "per ${metric.linearize()}"
  }

  data class Purpose(val action: Clause) : Modifier {
    override fun linearize(): String = "to ${action.linearize()}"
  }
}

internal fun Modifier.unresolved(): List<Unresolved> =
    when (this) {
      is Modifier.Between -> first.unresolved() + second.unresolved()
      is Modifier.Per -> metric.unresolved()
      is Modifier.Purpose -> action.unresolved()
      is Modifier.Relation -> target.unresolved()
      is Modifier.Parenthetical,
      is Modifier.Phrase,
      is Modifier.Supplement -> emptyList()
    }
