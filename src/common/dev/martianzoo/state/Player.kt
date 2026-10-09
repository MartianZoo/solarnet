package dev.martianzoo.state

import dev.martianzoo.catalog.conventionalPlayerClassNames
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression

/** One occupied seat, represented by an [Actor]. */
public data class Player(override val className: ClassName) : Actor {
  init {
    require(className != Actor.ADMIN.className) { "`Admin` is not a Player" }
  }

  override val expression: Expression = className.expression

  override fun toString(): String = className.toString()

  public companion object {
    /** Returns the conventional `Player1` through `PlayerN` identities in seat order. */
    public fun players(upTo: Int): List<Player> = conventionalPlayerClassNames(upTo).map(::Player)
  }
}
