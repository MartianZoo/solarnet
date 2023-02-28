package dev.martianzoo.tfm.repl

import dev.martianzoo.tfm.engine.Component
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.Instruction.Remove
import dev.martianzoo.tfm.pets.ast.ScaledTypeExpr
import dev.martianzoo.tfm.pets.ast.TypeExpr
import dev.martianzoo.tfm.types.PClass
import dev.martianzoo.tfm.types.PClassLoader
import dev.martianzoo.util.iff

object PTypeToText { // TODO refactor to ClassInfo / TypeInfo type dealies
  /** A detailed multi-line description of the class. */
  public fun describe(expr: TypeExpr, loader: PClassLoader): String {
    fun descendingBySubclassCount(classes: Iterable<PClass>) =
        classes.sortedWith(compareBy({ -it.allSubclasses.size }, { it.className }))

    val ptype = loader.resolveType(expr)
    val pclass = ptype.pclass

    val subs = descendingBySubclassCount(pclass.allSubclasses - pclass)
    val substring =
        when (subs.size) {
          0 -> "(none)"
          in 1..7 -> subs.joinToString()
          else -> subs.take(6).joinToString() + " (${subs.size - 6} others)"
        }

    val gain = loader.transformer.applyDefaultsIn(Gain(ScaledTypeExpr(1, expr)))
    val remove = loader.transformer.applyDefaultsIn(Remove(ScaledTypeExpr(1, expr)))

    val nameId = "${pclass.className}" + "[${pclass.id}]".iff(pclass.id != pclass.className)

    // TODO linkages?
    val classStuff = """
      Class $nameId:
          subclasses: $substring
          invariants: ${pclass.invariants.joinToString("""
                      """)}
          base type:  ${pclass.baseType.typeExprFull}
          class fx:   ${pclass.classEffects.joinToString("""
                      """)}


    """.trimIndent().iff(expr.isTypeOnly)

    // TODO linkages?
    val typeStuff = """
      Type $expr:
          std form:   $ptype
          long form:  ${ptype.typeExprFull}
          supertypes: ${ptype.supertypes().joinToString()}
          defaults:   $gain / $remove
    """.trimIndent()

    return classStuff + typeStuff + try {
      """


        Component [$ptype]:
            effects:    ${Component(ptype).effects().joinToString("""
                        """)}
      """.trimIndent()
    } catch (e: Exception) {
      ""
    }
  }
}
