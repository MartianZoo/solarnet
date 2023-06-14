package dev.martianzoo.api

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
  // A special fake class name that doesn't actually point to a class
  // TODO consider making Ok and Die like that too?
  public val THIS = cn("This")

  // Classes defined in system.pets

  public val ATOMIZED = cn("Atomized")
  public val AUTO_LOAD = cn("AutoLoad")
  public val CLASS = cn("Class")
  public val CUSTOM = cn("Custom")
  public val COMPONENT = cn("Component")
  public val DIE = cn("Die")
  public val OK = cn("Ok")
  public val SIGNAL = cn("Signal")
  public val SYSTEM = cn("System")
  public val TEMPORARY = cn("Temporary")

  // Classes not defined in system.pets but which need to be defined by the game somewhere

  public val ANYONE = cn("Anyone")
  public val OWNED = cn("Owned")
  public val OWNER = cn("Owner")
  public val USE_ACTION = cn("UseAction")
}
