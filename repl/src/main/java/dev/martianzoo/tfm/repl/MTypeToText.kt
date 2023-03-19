package dev.martianzoo.tfm.repl

import dev.martianzoo.tfm.engine.Component
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.Instruction.Remove
import dev.martianzoo.tfm.pets.ast.ScaledExpression
import dev.martianzoo.tfm.types.MClass
import dev.martianzoo.tfm.types.MClassLoader
import dev.martianzoo.tfm.types.Transformers.InsertDefaults
import dev.martianzoo.util.iff

object MTypeToText { // TODO refactor to ClassInfo / TypeInfo type dealies
  /** A detailed multi-line description of the class. */
  public fun describe(expression: Expression, loader: MClassLoader): String {
    fun descendingBySubclassCount(classes: Iterable<MClass>) =
        classes.sortedWith(compareBy({ -it.allSubclasses.size }, { it.className }))

    val mtype = loader.resolve(expression)
    val mclass = mtype.mclass

    val subs = descendingBySubclassCount(mclass.allSubclasses - mclass)
    val substring =
        when (subs.size) {
          0 -> "(none)"
          in 1..7 -> subs.joinToString { "${it.className}" }
          else -> subs.take(6).joinToString { "${it.className}" } + " (${subs.size - 6} others)"
        }

    val names =
        "${mclass.className}" + "[${mclass.shortName}]".iff(mclass.shortName != mclass.className)

    fun sequenceCount(seq: Sequence<Any>, limit: Int): String {
      val partial = seq.take(limit + 1).count()
      return if (partial == limit + 1) "$limit+" else "$partial"
    }

    val concTypes = sequenceCount(mclass.baseType.concreteSubtypesSameClass(), 100)

    // BIGTODO invariants seemingly not working?
    val classStuff =
        """
          Class $names:
              subclasses:  $substring
              invariants:  ${mclass.invariants.joinToString("""
                           """)}
              base type:   ${mclass.baseType.expressionFull}
              c. types:    $concTypes
              raw fx:      ${mclass.declaration.effects.map { it.effect }.joinToString("""
                           """)}
              class fx:    ${mclass.classEffects.joinToString("""
                           """) { "${it.effect}" + if (it.linkages.any()) " ${it.linkages}" else "" } }
    
    
        """
            .trimIndent()

    val concSubs = sequenceCount(mtype.allConcreteSubtypes(), 100)

    val id = InsertDefaults(loader)

    val allCases = id.transform(expression)
    val gain = id.transform(Gain(ScaledExpression(1, expression)))
    val remove = id.transform(Remove(ScaledExpression(1, expression)))

    val typeStuff =
        """
          Expression $expression:
              std. form:   ${mtype.expression}
              short form:  ${mtype.expressionShort}
              long form:   ${mtype.expressionFull}
              supertypes:  ${mtype.supertypes().joinToString { "${it.className}" }}
              defaults:    $allCases / +$gain / $remove
              c. subtypes: $concSubs
        """
            .trimIndent()

    val componentStuff =
        if (mtype.abstract) {
          ""
        } else {
          val c = Component.ofType(mtype)
          """


        Component $c:
            effects:     ${
        c.effects().joinToString("""
                         """)
      }
      """
              .trimIndent()
        }
    return classStuff + typeStuff + componentStuff
  }
}
