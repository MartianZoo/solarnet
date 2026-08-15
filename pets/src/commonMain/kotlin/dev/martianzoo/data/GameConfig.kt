package dev.martianzoo.data

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn

/**
 * Unresolved user intent expressed as positive and negative class-name spellings, plus concrete
 * player class names in seat order.
 *
 * An Authority applies defaults, implications, selection policies, and validation to cook this into
 * a complete [GamePremise].
 */
public data class GameConfig(
    public val includedClassNames: Set<ClassName>,
    public val excludedClassNames: Set<ClassName> = emptySet(),
    public val playerClassNames: List<ClassName> = emptyList(),
) {
  init {
    require(playerClassNames.distinct().size == playerClassNames.size) {
      "a game configuration cannot seat the same player class more than once"
    }
    require(
        includedClassNames.intersect(excludedClassNames).isEmpty() &&
            playerClassNames.none { it in includedClassNames || it in excludedClassNames }
    ) {
      "a game configuration cannot include and exclude the same class"
    }
  }

  private constructor(
      parsed: Pair<Set<ClassName>, Set<ClassName>>,
      playerClassNames: List<ClassName>,
  ) : this(parsed.first, parsed.second, playerClassNames)

  public constructor(
      source: String,
      vararg playerClassNames: String,
  ) : this(parse(source), playerClassNames.map(::cn))

  override fun toString(): String =
      (includedClassNames.map { "$it" } + excludedClassNames.map { "-$it" }).joinToString()

  public companion object {
    /** Creates a configuration from already-canonicalized positive and negative names. */
    public fun create(
        included: Iterable<ClassName>,
        excluded: Iterable<ClassName> = emptyList(),
        playerClassNames: Iterable<ClassName> = emptyList(),
    ): GameConfig {
      val (includedNames, excludedNames) = toSets(included.toList(), excluded.toList())
      return GameConfig(includedNames, excludedNames, playerClassNames.toList())
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
