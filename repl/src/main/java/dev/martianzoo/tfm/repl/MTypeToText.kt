package dev.martianzoo.tfm.repl

import dev.martianzoo.tfm.api.SpecialClassNames.THIS
import dev.martianzoo.tfm.engine.Component
import dev.martianzoo.tfm.pets.Raw
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.Instruction.Remove
import dev.martianzoo.tfm.pets.ast.ScaledExpression.Companion.scaledEx
import dev.martianzoo.tfm.types.MClass
import dev.martianzoo.util.iff

object MTypeToText { // TODO refactor to ClassInfo / TypeInfo type dealies
  /** A detailed multi-line description of the class. */
  public fun describe(expr: Raw<Expression>, session: InteractiveSession): String {
    val expression = expr.unprocessed // TODO
    val mtype = session.game.resolve(expression) // TODO
    val mclass = mtype.mclass

    val classDisplay =
        "${mclass.className}" + "[${mclass.shortName}]".iff(mclass.shortName != mclass.className)

    val subs = descendingBySubclassCount(mclass.allSubclasses - mclass)
    val subclassesDisplay =
        when (subs.size) {
          0 -> "(none)"
          in 1..7 -> subs.joinToString { "${it.className}" }
          else -> subs.take(6).joinToString { "${it.className}" } + " (${subs.size - 6} others)"
        }

    val invars = mclass.declaration.invariants
    val invariantsDisplay = invars.joinToString().ifEmpty { "(none)" }

    val baseTypeDisplay = mclass.baseType.expressionFull

    val concTypes = mclass.baseType.concreteSubtypesSameClass()
    val cmptTypesDisplay = "${sequenceCount(concTypes, 100)} ${mclass.className}<>"

    val rawFxDisplay = mclass.declaration.effects.map { it.effect.unprocessed }

    val classFxDisplay =
        mclass.classEffects.map {
          "${it.effect.unprocessed}" + if (it.depLinkages.any()) " ${it.depLinkages}" else ""
        }

    val classStuff =
        """
          Class $classDisplay:
            subclasses: $subclassesDisplay
            invariants: $invariantsDisplay 
            base type:  $baseTypeDisplay
            cmpt types: $cmptTypesDisplay
            raw fx:     ${
          rawFxDisplay.joinToString("""
                        """)
        }
            class fx:   ${
          classFxDisplay.joinToString("""
                        """)
        }


        """
            .trimIndent()

    val supertypesDisplay = mtype.supertypes().joinToString { "${it.className}" }

    val id = session.game.loader.transformers.insertDefaults(THIS.expr) // TODO context??
    val allCases = id.transform(expression)
    val gain = id.transform(Gain(scaledEx(1, expression)))
    val remove = id.transform(Remove(scaledEx(1, expression)))

    val numComponentTypes = sequenceCount(mtype.allConcreteSubtypes(), 100)

    val typeStuff =
        """
          Expression `$expression`:
            std. form:  ${mtype.expression}
            short form: ${mtype.expressionShort}
            long form:  ${mtype.expressionFull}
            supertypes: $supertypesDisplay
            defaults:   *$allCases / +$gain / $remove
            cmpt types: $numComponentTypes
        """
            .trimIndent()

    val componentStuff =
        if (mtype.abstract) {
          ""
        } else {
          val c = Component.ofType(mtype)
          """


            Component $c:
              effects:    ${
            c.effects(session.game).map { it.original }.joinToString("""
                          """)
          }
              current ct: ${session.countComponent(c)}
          """
              .trimIndent()
        }
    return classStuff + typeStuff + componentStuff
  }

  fun descendingBySubclassCount(classes: Iterable<MClass>) =
      classes.sortedWith(compareBy({ -it.allSubclasses.size }, { it.className }))

  fun sequenceCount(seq: Sequence<Any>, limit: Int): String {
    val partial = seq.take(limit + 1).count()
    return if (partial == limit + 1) "$limit+" else "$partial"
  }
}
