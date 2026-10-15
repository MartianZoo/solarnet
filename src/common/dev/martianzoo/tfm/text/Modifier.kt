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

  data class Steps(val amount: Amount, val distributed: Boolean = false) : Modifier {
    constructor(count: Int, distributed: Boolean = false) : this(Amount.Fixed(count), distributed)

    override fun linearize(): String =
        listOfNotNull(amount.linearize(), "each".takeIf { distributed }).joinToString(" ")

    sealed interface Amount {
      fun linearize(): String

      public data class Fixed(public val count: Int) : Amount {
        override fun linearize(): String = stepCount(count)
      }

      public data object OneOrMore : Amount {
        override fun linearize(): String = "1 or more steps"
      }

      public data object SameNumber : Amount {
        override fun linearize(): String = "the same number of steps"
      }
    }
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
      is Modifier.Steps,
      is Modifier.Supplement -> emptyList()
    }
