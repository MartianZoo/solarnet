package dev.martianzoo.catalog

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn

/** Returns the conventional `Player1` through `PlayerN` Class Names in seat order. */
public fun conventionalPlayerClassNames(upTo: Int): List<ClassName> {
  require(upTo >= 0) { "player count cannot be negative: `$upTo`" }
  return (1..upTo).map { seat -> cn("Player$seat") }
}
