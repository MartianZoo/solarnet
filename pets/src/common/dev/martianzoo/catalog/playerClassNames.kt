package dev.martianzoo.catalog

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn

/**
 * Returns `Player1` through `PlayerN` in seat order; zero yields an empty list and a negative
 * [upTo] throws [IllegalArgumentException].
 */
public fun conventionalPlayerClassNames(upTo: Int): List<ClassName> {
  require(upTo >= 0) { "player count cannot be negative: `$upTo`" }
  return (1..upTo).map { seat -> cn("Player$seat") }
}
