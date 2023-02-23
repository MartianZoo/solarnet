package dev.martianzoo.tfm.repl

import dev.martianzoo.tfm.pets.SpecialClassNames.COMPONENT
import dev.martianzoo.tfm.pets.ast.TypeExpr
import dev.martianzoo.tfm.types.PClass
import dev.martianzoo.tfm.types.PType

object PTypeToText {
  /** A detailed multi-line description of the class. */
  public fun describe(ptype: PType, expr: TypeExpr): String {
    fun descendingBySubclassCount(classes: Iterable<PClass>) =
        classes.sortedWith(compareBy({ -it.allSubclasses.size }, { it.className }))

    val pclass = ptype.pclass
    val supersButComponent = pclass.allSuperclasses.filter { it.className != COMPONENT }
    val supers = descendingBySubclassCount(supersButComponent - pclass)
    val superstring = if (supers.isEmpty()) "(none)" else supers.joinToString()

    val subs = descendingBySubclassCount(pclass.allSubclasses - pclass)
    val substring =
        when (subs.size) {
          0 -> "(none)"
          in 1..7 -> subs.joinToString()
          else -> subs.take(6).joinToString() + " (${subs.size - 6} others)"
        }
    val fx = pclass.classEffects.joinToString("\n                   ")
    return """
      Class ${pclass.className}:
        id:        ${pclass.id}
        abstract:  ${pclass.abstract}
        supers:    $superstring
        subs:      $substring
        base type: ${pclass.baseType.typeExprFull}
        class fx:  $fx

      Type ${expr}:
        canonical: ${ptype.typeExpr}
        full:      ${ptype.typeExprFull}
        abstract:  ${ptype.abstract}
        supers:    ${ptype.supertypes().joinToString { "${it.typeExpr}" }}
    """.trimIndent()
  }
}
