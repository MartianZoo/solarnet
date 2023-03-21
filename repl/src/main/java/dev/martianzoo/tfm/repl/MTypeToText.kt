package dev.martianzoo.tfm.repl

import dev.martianzoo.tfm.api.SpecialClassNames.THIS
import dev.martianzoo.tfm.engine.Component
import dev.martianzoo.tfm.engine.Game
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.Instruction.Remove
import dev.martianzoo.tfm.pets.ast.ScaledExpression.Companion.scaledEx
import dev.martianzoo.tfm.types.MClass
import dev.martianzoo.util.iff

object MTypeToText { // TODO refactor to ClassInfo / TypeInfo type dealies
  /** A detailed multi-line description of the class. */
  public fun describe(expression: Expression, game: Game): String {
    fun descendingBySubclassCount(classes: Iterable<MClass>) =
        classes.sortedWith(compareBy({ -it.allSubclasses.size }, { it.className }))

    val mtype = game.resolve(expression)
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
    val effex = mclass.classEffects.joinToString("""
                           """) {
      "${it.effect}" + if (it.depLinkages.any()) " ${it.depLinkages}" else ""
    }
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
              class fx:    $effex
        """
            .trimIndent()

    val concSubs = sequenceCount(mtype.allConcreteSubtypes(), 100)

    val id = game.loader.transformers.insertDefaults(THIS.expr) // TODO context??

    val allCases = id.transform(expression)
    val gain = id.transform(Gain(scaledEx(1, expression)))
    val remove = id.transform(Remove(scaledEx(1, expression)))

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
            effects:     ${c.effects(game).map { it.original }.joinToString("""
                         """)}
      """
              .trimIndent()
        }
    return classStuff + typeStuff + componentStuff
  }
}
