package dev.martianzoo.tfm.repl

import dev.martianzoo.tfm.engine.Component
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.Instruction.Remove
import dev.martianzoo.tfm.pets.ast.ScaledExpression
import dev.martianzoo.tfm.types.PClass
import dev.martianzoo.tfm.types.PClassLoader
import dev.martianzoo.util.iff

object PTypeToText { // TODO refactor to ClassInfo / TypeInfo type dealies
  /** A detailed multi-line description of the class. */
  public fun describe(expression: Expression, loader: PClassLoader): String {
    fun descendingBySubclassCount(classes: Iterable<PClass>) =
        classes.sortedWith(compareBy({ -it.allSubclasses.size }, { it.className }))

    val ptype = loader.resolve(expression)
    val pclass = ptype.pclass

    val subs = descendingBySubclassCount(pclass.allSubclasses - pclass)
    val substring =
        when (subs.size) {
          0 -> "(none)"
          in 1..7 -> subs.joinToString()
          else -> subs.take(6).joinToString() + " (${subs.size - 6} others)"
        }

    val nameId = "${pclass.className}" + "[${pclass.id}]".iff(pclass.id != pclass.className)

    fun sequenceCount(seq: Sequence<Any>, limit: Int): String {
      val partial = seq.take(limit + 1).count()
      return if (partial == limit + 1) "$limit+" else "$partial"
    }

    val concTypes = sequenceCount(pclass.concreteTypesThisClass(), 100)

    // BIGTODO invariants seemingly not working?
    // TODO show linkages we already have
    val classStuff = """
      Class $nameId:
          subclasses:  $substring
          invariants:  ${
      pclass.invariants.joinToString("""
                       """)
    }
          base type:   ${pclass.baseType.expressionFull}
          c. types:    $concTypes
          class fx:    ${
      pclass.classEffects.joinToString("""
                       """)
    }


    """.trimIndent().iff(expression.simple)

    val concSubs = sequenceCount(ptype.allConcreteSubtypes(), 100)

    val allCases = loader.transformer.insertDefaults(expression)
    val gain = loader.transformer.insertDefaults(Gain(ScaledExpression(1, expression)))
    val remove = loader.transformer.insertDefaults(Remove(ScaledExpression(1, expression)))

    val typeStuff = """
      Expression $expression:
          std. form:   $ptype
          long form:   ${ptype.expressionFull}
          supertypes:  ${ptype.supertypes().joinToString()}
          defaults:    $allCases / +$gain / $remove
          c. subtypes: $concSubs
    """.trimIndent()

    return classStuff + typeStuff + try {
      """


        Component [$ptype]:
            effects:     ${
        Component(ptype).effects().joinToString("""
                         """)
      }
      """.trimIndent()
    } catch (e: Exception) {
      ""
    }
  }
}
