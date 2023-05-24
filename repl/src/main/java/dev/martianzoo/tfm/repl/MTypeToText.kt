package dev.martianzoo.tfm.repl

import dev.martianzoo.tfm.engine.Component.Companion.toComponent
import dev.martianzoo.tfm.engine.PlayerSession
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.types.MClass
import dev.martianzoo.tfm.types.MType
import dev.martianzoo.util.iff

object MTypeToText {
  /** A detailed multi-line description of the class. */
  public fun describe(expression: Expression, session: PlayerSession): String {
    val mtype = session.reader.resolve(expression) as MType
    val mclass = mtype.root

    val classDisplay =
        "${mclass.className}" + "[${mclass.shortName}]".iff(mclass.shortName != mclass.className)

    val subs = descendingBySubclassCount(mclass.allSubclasses - mclass)
    val subclassesDisplay =
        when (subs.size) {
          0 -> "(none)"
          in 1..7 -> subs.joinToString { "${it.className}" }
          else -> subs.take(6).joinToString { "${it.className}" } + " (${subs.size - 6} others)"
        }

    val invars = mclass.typeInvariants + mclass.generalInvars
    val invariantsDisplay = invars.joinToString().ifEmpty { "(none)" }

    val baseTypeDisplay = mclass.baseType.expressionFull

    val concTypes = mclass.baseType.concreteSubtypesSameClass()
    val cmptTypesDisplay = "${sequenceCount(concTypes, 100)} ${mclass.className}<>"

    val rawFxDisplay = mclass.rawEffects()
    val classFxDisplay = mclass.classEffects

    val classStuff =
        """
          Class $classDisplay:
            subclasses: $subclassesDisplay
            invariants: $invariantsDisplay
            base type:  $baseTypeDisplay
            cmpt types: $cmptTypesDisplay
            raw fx:     ${rawFxDisplay.joinToString("""
                        """)}
            class fx:   ${classFxDisplay.joinToString("""
                        """)}


        """
            .trimIndent()

    val supertypesDisplay = mtype.supertypes().joinToString { "${it.className}" }

    val numComponentTypes = sequenceCount(mtype.allConcreteSubtypes(), 100)

    val typeStuff =
        """
          Expression `$expression`:
            std. form:  ${mtype.expression}
            long form:  ${mtype.expressionFull}
            supertypes: $supertypesDisplay
            cmpt types: $numComponentTypes
        """
            .trimIndent()

    val componentStuff =
        if (mtype.abstract) {
          ""
        } else {
          val c = mtype.toComponent()
          // val custom = if (mtype.root.custom != null) {
          //   val instructor = Instructor(session.writer as GameWriterImpl)
          //   instructor.prepareCustom(mtype).toString()
          // } else {
          //   "no"
          // }
          """


            Component $c:
              effects:    ${
            c.typeEffectsGetRidOf.joinToString(
                """
                          """,
            )
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
