package dev.martianzoo.pets.data

import dev.martianzoo.pets.api.Exceptions.InvalidGameConfigException
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn

/**
 * Unresolved user intent expressed as unordered positive and negative class-name selections,
 * positive counts of concrete setup Components, and concrete user-facing player names in seat
 * order. A counted entry is written as a positive integer followed by a Class Name, such as `4
 * CorporationOption`.
 *
 * A Catalog-specific premise factory applies defaults, selection policies, and validation to cook
 * this into a complete [GamePremise]. The configuration itself never records inferred selections.
 *
 * @throws InvalidGameConfigException if the configuration contains invalid or contradictory user
 *   input
 */
public data class GameConfig(
    public val includedClassNames: Set<ClassName>,
    public val excludedClassNames: Set<ClassName> = emptySet(),
    public val playerNames: List<ClassName> = emptyList(),
    public val componentCounts: Map<ClassName, Int> = emptyMap(),
) {
  init {
    if (playerNames.distinct().size != playerNames.size) {
      throw InvalidGameConfigException("duplicate player names: `$playerNames`")
    }
    val multiplyConfiguredNames =
        (includedClassNames intersect excludedClassNames) +
            (includedClassNames intersect componentCounts.keys) +
            (excludedClassNames intersect componentCounts.keys)
    if (multiplyConfiguredNames.isNotEmpty()) {
      throw InvalidGameConfigException(
          "class names cannot have multiple configuration entries: `$multiplyConfiguredNames`"
      )
    }
    val invalidCounts = componentCounts.filterValues { it <= 0 }
    if (invalidCounts.isNotEmpty()) {
      throw InvalidGameConfigException("component counts must be positive: `$invalidCounts`")
    }
    val playerClassSelections = playerNames.filter {
      it in includedClassNames || it in excludedClassNames || it in componentCounts
    }
    if (playerClassSelections.isNotEmpty()) {
      throw InvalidGameConfigException(
          "player names cannot also be class selections: `$playerClassSelections`"
      )
    }
  }

  private constructor(
      parsed: Parsed,
      playerNames: List<ClassName>,
  ) : this(parsed.included, parsed.excluded, playerNames, parsed.componentCounts)

  public constructor(
      source: String,
      vararg playerNames: String,
  ) : this(parse(source), parsePlayerNames(playerNames.toList()))

  override fun toString(): String =
      (includedClassNames.map { "$it" } +
              componentCounts.map { (name, count) -> "$count $name" } +
              excludedClassNames.map { "-$it" })
          .joinToString()

  public companion object {
    private val COUNTED_ENTRY = Regex("(\\d+)\\s+(.+)")

    private data class Parsed(
        val included: Set<ClassName>,
        val excluded: Set<ClassName>,
        val componentCounts: Map<ClassName, Int>,
    )

    /** Creates a configuration from already-canonicalized selections and component counts. */
    public fun create(
        included: Iterable<ClassName>,
        excluded: Iterable<ClassName> = emptyList(),
        playerNames: Iterable<ClassName> = emptyList(),
        componentCounts: Map<ClassName, Int> = emptyMap(),
    ): GameConfig {
      val (includedNames, excludedNames) = toSets(included.toList(), excluded.toList())
      return GameConfig(
          includedNames,
          excludedNames,
          playerNames.toList(),
          componentCounts.toMap(LinkedHashMap()),
      )
    }

    private fun parse(source: String): Parsed {
      try {
        val included = mutableListOf<ClassName>()
        val excluded = mutableListOf<ClassName>()
        val componentCounts = linkedMapOf<ClassName, Int>()
        source.split(',', '\n').map(String::trim).filter(String::isNotEmpty).forEach { token ->
          val selected = !token.startsWith('-')
          val entry = if (selected) token else token.drop(1)
          val counted = COUNTED_ENTRY.matchEntire(entry)
          if (counted != null) {
            if (!selected) {
              throw InvalidGameConfigException("a counted component cannot be excluded: `$token`")
            }
            val count =
                counted.groupValues[1].toIntOrNull()
                    ?: throw InvalidGameConfigException("component count is too large: `$token`")
            val name = counted.groupValues[2]
            if (count <= 0 || name.any(Char::isWhitespace)) {
              throw InvalidGameConfigException("invalid counted component entry: `$token`")
            }
            val className = cn(name)
            if (componentCounts.put(className, count) != null) {
              throw InvalidGameConfigException("duplicate configuration entry: `$className`")
            }
          } else {
            if (entry.isEmpty() || entry.any(Char::isWhitespace)) {
              throw InvalidGameConfigException(
                  "expected a comma-or-newline-separated configuration entry, found `$token`"
              )
            }
            (if (selected) included else excluded).add(cn(entry))
          }
        }
        val (includedNames, excludedNames) = toSets(included, excluded)
        return Parsed(includedNames, excludedNames, componentCounts)
      } catch (e: IllegalArgumentException) {
        throw InvalidGameConfigException("invalid game configuration: `$source`", e)
      }
    }

    private fun parsePlayerNames(playerNames: List<String>): List<ClassName> =
        try {
          playerNames.map(::cn)
        } catch (e: IllegalArgumentException) {
          throw InvalidGameConfigException("invalid player names: `$playerNames`", e)
        }

    private fun toSets(
        included: List<ClassName>,
        excluded: List<ClassName>,
    ): Pair<Set<ClassName>, Set<ClassName>> {
      if ((included + excluded).distinct().size != included.size + excluded.size) {
        throw InvalidGameConfigException("duplicate class selections: `${included + excluded}`")
      }
      return included.toCollection(linkedSetOf()) to excluded.toCollection(linkedSetOf())
    }
  }
}
