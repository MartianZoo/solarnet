package dev.martianzoo.tfm.api

import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn

/**
 * [ClassName] instances for all component classes of special significance to the implementation
 * code. Of course, a particular class name does not *have* to be in this list to be usable from the
 * code, but this habit makes it easier to "find usages" etc. to see how the component types are
 * being used. Or rename, etc.
 */
public object SpecialClassNames {
  public val ANYONE = cn("Anyone")
  public val CLASS = cn("Class")
  public val COMPONENT = cn("Component")
  public val DIE = cn("Die")
  public val END = cn("End")
  public val GAME = cn("Game")
  public val MEGACREDIT = cn("Megacredit")
  public val OK = cn("Ok")
  public val OWNED = cn("Owned")
  public val OWNER = cn("Owner")
  public val PRODUCTION = cn("Production")
  public val STANDARD_RESOURCE = cn("StandardResource")
  public val THIS = cn("This")
  public val USE_ACTION = cn("UseAction")
}
