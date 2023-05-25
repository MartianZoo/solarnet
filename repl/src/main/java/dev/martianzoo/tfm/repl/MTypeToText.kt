package dev.martianzoo.tfm.repl

import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.types.MType
import dev.martianzoo.tfm.types.TypeDescription
import dev.martianzoo.util.iff

object MTypeToText {
  /** A detailed multi-line description of a type. */
  public fun describe(expression: Expression, mtype: MType): String {

    val desc = TypeDescription(mtype)

    val long = mtype.className
    val short = desc.classShortName
    val classDisplay = "$long" + "[$short]".iff(short != long)

    val subs = desc.subclassNames - long
    val subclassesDisplay =
        when (subs.size) {
          0 -> "(none)"
          in 1..7 -> subs.joinToString()
          else -> subs.take(6).joinToString() + " (${subs.size - 6} others)"
        }

    val classStuff =
        """
          Class `$classDisplay`:
            subclasses:  $subclassesDisplay
            subclasses:  ${desc.superclassNames}
            invariants:  ${desc.classInvariants.joinToString().ifEmpty { "(none)" }}
            base type:   ${desc.baseType.expressionFull}
            cmpt types:  ${desc.concreteTypesForThisClassCount}
            raw fx:      ${desc.rawClassEffects.joinToString("""
                         """)}
            class fx:    ${desc.classEffects.joinToString("""
                         """)}


        """.trimIndent()

    val typeStuff =
        """
          Expression `$expression`:
            std. form:   ${mtype.expression}
            long form:   ${mtype.expressionFull}
            supertypes:  ${desc.supertypes.joinToString { "${it.expressionFull}" }}
            cmpt types:  ${desc.componentTypesCount}
        """.trimIndent()

    val componentStuff =
        if (mtype.abstract) {
          ""
        } else {
          """


            Component `[${mtype.expressionFull}]`:
              effects:     ${desc.componentEffects.joinToString("""
                           """)}
          """.trimIndent()
        }

    return classStuff + typeStuff + componentStuff
  }
}
