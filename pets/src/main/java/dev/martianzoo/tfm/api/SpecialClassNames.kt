package dev.martianzoo.tfm.api

import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn

/**
 * [ClassName] instances for all component classes of special significance to the implementation
 * code. Of course, a particular class name does not *have* to be in this list to be usable from the
 * code, but this habit makes it easier to "find usages" etc. to see how the component types are
 * being used. Or rename, etc.
 *
 * Class names specific to TfM should ideally not go here, but are still better here than nowhere.
 */
public object SpecialClassNames {
  public val ANYONE = cn("Anyone")
  public val ATOMIZED = cn("Atomized")
  public val CLASS = cn("Class")
  public val COMPONENT = cn("Component")
  public val DIE = cn("Die")
  public val END = cn("End")
  public val OK = cn("Ok")
  public val OWNED = cn("Owned")
  public val OWNER = cn("Owner")
  public val THIS = cn("This")
  public val USE_ACTION = cn("UseAction")

  public fun player(seat: Int) = cn("Player$seat").also { require(seat in 1..5) }

  // Not class names, but...
  public val RAW = "RAW"

  public val PROD = "PROD" // TODO Mars-specific
}
