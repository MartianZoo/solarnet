package dev.martianzoo.tfm.text

/** Document-level English syntax, linearized only at the [English] facade. */
internal sealed interface EnglishText {
  fun linearize(): String

  fun unresolved(): List<Unresolved>

  data object Empty : EnglishText {
    override fun linearize(): String = ""

    override fun unresolved(): List<Unresolved> = emptyList()
  }

  data class SentenceText(val sentence: Sentence) : EnglishText {
    override fun linearize(): String = sentence.linearize()

    override fun unresolved(): List<Unresolved> = sentence.unresolved()
  }

  data class Sequence(
      val members: List<EnglishText>,
      val separator: String,
  ) : EnglishText {
    init {
      require(members.isNotEmpty())
    }

    override fun linearize(): String =
        members.joinToString(separator, transform = EnglishText::linearize)

    override fun unresolved(): List<Unresolved> = members.flatMap(EnglishText::unresolved)
  }

  data class Labeled(
      val label: String,
      val body: EnglishText,
  ) : EnglishText {
    override fun linearize(): String = "$label${body.linearize()}"

    override fun unresolved(): List<Unresolved> = body.unresolved()
  }

  companion object {
    internal fun join(members: List<EnglishText>, separator: String = " "): EnglishText =
        when (members.size) {
          0 -> Empty
          1 -> members.single()
          else -> Sequence(members, separator)
        }
  }
}
