package dev.martianzoo.data

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn

/** Raw user intent expressed as positive and negative canonical class names. */
public data class GameConfig(
    public val includedClassNames: Set<ClassName>,
    public val excludedClassNames: Set<ClassName> = emptySet(),
) {
  init {
    require(includedClassNames.intersect(excludedClassNames).isEmpty()) {
      "a game configuration cannot include and exclude the same class"
    }
  }

  private constructor(
      parsed: Pair<Set<ClassName>, Set<ClassName>>
  ) : this(parsed.first, parsed.second)

  public constructor(source: String) : this(parse(source))

  override fun toString(): String =
      (includedClassNames.map { "$it" } + excludedClassNames.map { "-$it" }).joinToString()

  public companion object {
    /** Creates a configuration from already-canonicalized positive and negative names. */
    public fun create(
        included: Iterable<ClassName>,
        excluded: Iterable<ClassName> = emptyList(),
    ): GameConfig {
      val (includedNames, excludedNames) = toSets(included.toList(), excluded.toList())
      return GameConfig(includedNames, excludedNames)
    }

    private fun parse(source: String): Pair<Set<ClassName>, Set<ClassName>> {
      val entries =
          source.split(',', '\n').map(String::trim).filter(String::isNotEmpty).map { token ->
            val included = !token.startsWith('-')
            val name = if (included) token else token.drop(1)
            require(name.isNotEmpty() && name.none(Char::isWhitespace)) {
              "expected a comma-or-newline-separated class name, got: $token"
            }
            cn(name) to included
          }
      return toSets(
          entries.filter { it.second }.map { it.first },
          entries.filterNot { it.second }.map { it.first },
      )
    }

    private fun toSets(
        included: List<ClassName>,
        excluded: List<ClassName>,
    ): Pair<Set<ClassName>, Set<ClassName>> {
      require((included + excluded).distinct().size == included.size + excluded.size) {
        "a game configuration cannot mention the same class more than once"
      }
      return included.toCollection(linkedSetOf()) to excluded.toCollection(linkedSetOf())
    }
  }
}
