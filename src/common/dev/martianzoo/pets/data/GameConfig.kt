package dev.martianzoo.pets.data

import dev.martianzoo.pets.api.Exceptions.InvalidGameConfigException
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn

/**
 * Unresolved user intent expressed as positive and negative class-name spellings, plus concrete
 * user-facing player names in seat order.
 *
 * A Catalog applies defaults, selection policies, and validation to cook this into a complete
 * [GamePremise].
 *
 * @throws InvalidGameConfigException if the configuration contains invalid or contradictory user
 *   input
 */
public data class GameConfig(
    public val includedClassNames: Set<ClassName>,
    public val excludedClassNames: Set<ClassName> = emptySet(),
    public val playerNames: List<ClassName> = emptyList(),
) {
  init {
    if (playerNames.distinct().size != playerNames.size) {
      throw InvalidGameConfigException(
          "a game configuration cannot seat the same player name more than once"
      )
    }
    if (
        includedClassNames.intersect(excludedClassNames).isNotEmpty() ||
            playerNames.any { it in includedClassNames || it in excludedClassNames }
    ) {
      throw InvalidGameConfigException(
          "a game configuration cannot include and exclude the same class"
      )
    }
  }

  private constructor(
      parsed: Pair<Set<ClassName>, Set<ClassName>>,
      playerNames: List<ClassName>,
  ) : this(parsed.first, parsed.second, playerNames)

  public constructor(
      source: String,
      vararg playerNames: String,
  ) : this(parse(source), parsePlayerNames(playerNames.toList()))

  override fun toString(): String =
      (includedClassNames.map { "$it" } + excludedClassNames.map { "-$it" }).joinToString()

  public companion object {
    /** Creates a configuration from already-canonicalized positive and negative names. */
    public fun create(
        included: Iterable<ClassName>,
        excluded: Iterable<ClassName> = emptyList(),
        playerNames: Iterable<ClassName> = emptyList(),
    ): GameConfig {
      val (includedNames, excludedNames) = toSets(included.toList(), excluded.toList())
      return GameConfig(includedNames, excludedNames, playerNames.toList())
    }

    private fun parse(source: String): Pair<Set<ClassName>, Set<ClassName>> {
      try {
        val entries =
            source.split(',', '\n').map(String::trim).filter(String::isNotEmpty).map { token ->
              val included = !token.startsWith('-')
              val name = if (included) token else token.drop(1)
              if (name.isEmpty() || name.any(Char::isWhitespace)) {
                throw InvalidGameConfigException(
                    "expected a comma-or-newline-separated class name, got: $token"
                )
              }
              cn(name) to included
            }
        return toSets(
            entries.filter { it.second }.map { it.first },
            entries.filterNot { it.second }.map { it.first },
        )
      } catch (e: IllegalArgumentException) {
        throw InvalidGameConfigException("invalid game configuration: $source", e)
      }
    }

    private fun parsePlayerNames(playerNames: List<String>): List<ClassName> =
        try {
          playerNames.map(::cn)
        } catch (e: IllegalArgumentException) {
          throw InvalidGameConfigException("invalid player names: ${playerNames.joinToString()}", e)
        }

    private fun toSets(
        included: List<ClassName>,
        excluded: List<ClassName>,
    ): Pair<Set<ClassName>, Set<ClassName>> {
      if ((included + excluded).distinct().size != included.size + excluded.size) {
        throw InvalidGameConfigException(
            "a game configuration cannot mention the same class more than once"
        )
      }
      return included.toCollection(linkedSetOf()) to excluded.toCollection(linkedSetOf())
    }
  }
}
