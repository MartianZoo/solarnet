package dev.martianzoo.state

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression

/** One occupied seat; both an [Actor] and an [Anyone]. */
public data class Player(override val className: ClassName) : Actor, Anyone {
  init {
    require(className != Actor.ADMIN.className) { "`Admin` is not a Player" }
  }

  override val expression: Expression = className.expression

  override val expressionFull: Expression
    get() = expression

  override fun toString(): String = className.toString()

  public companion object {
    /** Returns the conventional `Player1` through `PlayerN` identities in seat order. */
    public fun players(upTo: Int): List<Player> {
      require(upTo >= 0) { "player count cannot be negative: `$upTo`" }
      return (1..upTo).map { Player(player(it)) }
    }

    private fun player(seat: Int) = cn("Player$seat").also { require(seat > 0) }
  }
}
