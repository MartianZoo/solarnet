package dev.martianzoo.api

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn

/**
 * [ClassName] instances for all component classes of special significance to the implementation
 * code. Of course, a particular class name does not *have* to be in this list to be usable from the
 * code, but this habit makes it easier to "find usages" etc. to see how the component types are
 * being used. Or rename, etc.
 *
 * Class names specific to TfM should ideally not go here, but are still better here than nowhere.
 */
public object SystemClasses {
  // A special fake class name that doesn't actually point to a class
  public val THIS: ClassName = cn("This")

  // Classes defined in system.pets

  public val ATOMIZED: ClassName = cn("Atomized")
  public val ACTOR: ClassName = cn("Actor")
  public val AUTO_LOAD: ClassName = cn("AutoLoad")
  public val CLASS: ClassName = cn("Class")
  public val CUSTOM: ClassName = cn("Custom")
  public val COMPONENT: ClassName = cn("Component")
  public val DIE: ClassName = cn("Die")
  public val HIDDEN: ClassName = cn("Hidden")
  public val OK: ClassName = cn("Ok")
  public val SIGNAL: ClassName = cn("Signal")
  public val SYSTEM: ClassName = cn("System")
  public val TEMPORARY: ClassName = cn("Temporary")
  public val ANYONE: ClassName = cn("Anyone")
  public val OWNED: ClassName = cn("Owned")
  public val OWNER: ClassName = cn("Owner")

  // Classes not defined in system.pets but which need to be defined by the game somewhere

  public val PLAYER: ClassName = cn("Player")
  public val USE_ACTION: ClassName = cn("UseAction")
}
