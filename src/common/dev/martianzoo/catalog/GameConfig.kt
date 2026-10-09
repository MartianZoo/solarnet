package dev.martianzoo.catalog

import dev.martianzoo.pets.api.Exceptions.InvalidGameConfigException
import dev.martianzoo.pets.api.SourceLocation
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn

/**
 * Unresolved user intent expressed as unordered positive and negative class-name selections, signed
 * adjustments to concrete setup Components, and concrete user-facing player names in seat order. An
 * adjustment is written as an integer followed by a Class Name, such as `2 Supply` or `-1
 * SelectablePreludeCount`.
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
    public val componentAdjustments: Map<ClassName, Int> = emptyMap(),
) {
  init {
    if (playerNames.distinct().size != playerNames.size) {
      throw InvalidGameConfigException(
          "duplicate player names: ${playerNames.joinToString { "`$it`" }}"
      )
    }
    val multiplyConfiguredNames =
        (includedClassNames intersect excludedClassNames) +
            (includedClassNames intersect componentAdjustments.keys) +
            (excludedClassNames intersect componentAdjustments.keys)
    if (multiplyConfiguredNames.isNotEmpty()) {
      throw InvalidGameConfigException(
          "class names cannot have multiple configuration entries: ${multiplyConfiguredNames.joinToString { "`$it`" }}"
      )
    }
    val zeroAdjustments = componentAdjustments.filterValues { it == 0 }
    if (zeroAdjustments.isNotEmpty()) {
      throw InvalidGameConfigException(
          "component adjustments must be nonzero: ${zeroAdjustments.entries.joinToString { "`${it.value} ${it.key}`" }}"
      )
    }
    val playerClassSelections = playerNames.filter {
      it in includedClassNames || it in excludedClassNames || it in componentAdjustments
    }
    if (playerClassSelections.isNotEmpty()) {
      throw InvalidGameConfigException(
          "player names cannot also be class selections: ${playerClassSelections.joinToString { "`$it`" }}"
      )
    }
  }

  private constructor(
      parsed: Parsed,
      playerNames: List<ClassName>,
  ) : this(parsed.included, parsed.excluded, playerNames, parsed.componentAdjustments)

  public constructor(
      source: String,
      vararg playerNames: String,
  ) : this(parse(source), parsePlayerNames(playerNames.toList()))

  override fun toString(): String =
      (includedClassNames.map { "$it" } +
              componentAdjustments.map { (name, adjustment) -> "$adjustment $name" } +
              excludedClassNames.map { "-$it" })
          .joinToString()

  public companion object {
    private val COUNTED_ENTRY = Regex("(\\d+)\\s+(.+)")

    private data class Parsed(
        val included: Set<ClassName>,
        val excluded: Set<ClassName>,
        val componentAdjustments: Map<ClassName, Int>,
    )

    /** Creates a configuration from canonicalized selections and setup-component adjustments. */
    public fun create(
        included: Iterable<ClassName>,
        excluded: Iterable<ClassName> = emptyList(),
        playerNames: Iterable<ClassName> = emptyList(),
        componentAdjustments: Map<ClassName, Int> = emptyMap(),
    ): GameConfig {
      val (includedNames, excludedNames) = toSets(included.toList(), excluded.toList())
      return GameConfig(
          includedNames,
          excludedNames,
          playerNames.toList(),
          componentAdjustments.toMap(LinkedHashMap()),
      )
    }

    private fun parse(source: String): Parsed {
      try {
        val included = mutableListOf<ClassName>()
        val excluded = mutableListOf<ClassName>()
        val componentAdjustments = linkedMapOf<ClassName, Int>()
        Regex("[^,\\n]+").findAll(source).forEach { match ->
          val token = match.value.trim()
          if (token.isEmpty()) return@forEach
          val location =
              SourceLocation(source, match.range.first + match.value.indexOf(token), token.length)
          try {
            val selected = !token.startsWith('-')
            val entry = if (selected) token else token.drop(1)
            val counted = COUNTED_ENTRY.matchEntire(entry)
            if (counted != null) {
              val count =
                  counted.groupValues[1].toIntOrNull()
                      ?: throw InvalidGameConfigException("setup adjustment is too large: `$token`")
              val name = counted.groupValues[2]
              if (count <= 0 || name.any(Char::isWhitespace)) {
                throw InvalidGameConfigException("invalid setup adjustment entry: `$token`")
              }
              val className = cn(name)
              val adjustment = if (selected) count else -count
              if (componentAdjustments.put(className, adjustment) != null) {
                throw InvalidGameConfigException("duplicate configuration entry: `$className`")
              }
            } else {
              if (entry.isEmpty() || entry.any(Char::isWhitespace)) {
                throw InvalidGameConfigException(
                    "expected a comma-or-newline-separated configuration entry; found `$token`"
                )
              }
              (if (selected) included else excluded).add(cn(entry))
            }
          } catch (e: InvalidGameConfigException) {
            e.sourceLocation = location
            throw e
          } catch (e: IllegalArgumentException) {
            throw InvalidGameConfigException(
                "invalid configuration entry `$token`: ${e.message}",
                e,
                location,
            )
          }
        }
        val (includedNames, excludedNames) = toSets(included, excluded)
        return Parsed(includedNames, excludedNames, componentAdjustments)
      } catch (e: IllegalArgumentException) {
        throw InvalidGameConfigException("invalid game configuration: `$source`", e)
      }
    }

    private fun parsePlayerNames(playerNames: List<String>): List<ClassName> =
        try {
          playerNames.map(::cn)
        } catch (e: IllegalArgumentException) {
          throw InvalidGameConfigException(
              "invalid player names: ${playerNames.joinToString { "`$it`" }}",
              e,
          )
        }

    private fun toSets(
        included: List<ClassName>,
        excluded: List<ClassName>,
    ): Pair<Set<ClassName>, Set<ClassName>> {
      if ((included + excluded).distinct().size != included.size + excluded.size) {
        throw InvalidGameConfigException(
            "duplicate class selections: ${(included + excluded).joinToString { "`$it`" }}"
        )
      }
      return included.toCollection(linkedSetOf()) to excluded.toCollection(linkedSetOf())
    }
  }
}
