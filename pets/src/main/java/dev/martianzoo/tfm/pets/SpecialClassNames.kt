package dev.martianzoo.tfm.pets

import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn

/**
 * [ClassName] instances for all component types of special significance to the implementation code.
 * Of course, a particular class name does not *have* to be in this list to be usable from the code,
 * but this habit makes it easier to "find usages" etc. to see how the component types are being
 * used. Or rename, etc.
 */
object SpecialClassNames {
  val ANYONE = cn("Anyone")
  val CLASS = cn("Class")
  val COMPONENT = cn("Component")
  val DIE = cn("Die")
  val END = cn("End")
  val MEGACREDIT = cn("Megacredit")
  val OK = cn("Ok")
  val OWNED = cn("Owned")
  val OWNER = cn("Owner")
  val PRODUCTION = cn("Production")
  val STANDARD_RESOURCE = cn("StandardResource")
  val THIS = cn("This")
  val USE_ACTION = cn("UseAction")
}
