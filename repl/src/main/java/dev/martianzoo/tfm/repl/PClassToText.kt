package dev.martianzoo.tfm.repl

import dev.martianzoo.tfm.pets.SpecialClassNames.COMPONENT
import dev.martianzoo.tfm.types.PClass

object PClassToText {
  /** A detailed multi-line description of the class. */
  // TODO this is a lot of presentation logic...
  public fun describe(pclass: PClass): String {
    fun descendingBySubclassCount(classes: Iterable<PClass>) =
        classes.sortedWith(compareBy({ -it.allSubclasses.size }, { it.className }))

    val supersButComponent = pclass.allSuperclasses.filter { it.className != COMPONENT }
    val supers = descendingBySubclassCount(supersButComponent - pclass)
    val superstring = if (supers.isEmpty()) "(none)" else supers.joinToString()

    val subs = descendingBySubclassCount(pclass.allSubclasses - pclass)
    val substring =
        when (subs.size) {
          0 -> "(none)"
          in 1..11 -> subs.joinToString()
          else -> subs.take(10).joinToString() + " (${subs.size - 10} others)"
        }
    val fx = pclass.classEffects.joinToString("\n                ")
    return """
      Name:     ${pclass.className}
      Id:       ${pclass.id}
      Abstract: ${pclass.abstract}
      Supers:   $superstring
      Subs:     $substring
      BaseType: ${pclass.baseType}
      Effects:  $fx
    """.trimIndent()
  }
}
