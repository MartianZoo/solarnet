package dev.martianzoo.tfm.text

/** Ordered conjunction or disjunction. */
internal data class Coordination<T>(
    val members: List<T>,
    private val conjunction: Conjunction? = null,
) {
  init {
    require(members.isNotEmpty())
    require(members.size == 1 || conjunction != null)
  }

  fun linearize(render: (T) -> String): String {
    val parts = members.map(render)
    return when (conjunction) {
      null -> parts.single()
      Conjunction.AND -> englishList(parts)
      Conjunction.OR -> englishAlternatives(parts)
      Conjunction.COMMA_OR -> parts.joinToString(", or ")
      Conjunction.EITHER_OR -> "either ${englishAlternatives(parts)}"
      Conjunction.THEN -> parts.joinToString(", then ")
    }
  }

  companion object {
    fun <T> one(member: T): Coordination<T> = Coordination(listOf(member))
  }
}
