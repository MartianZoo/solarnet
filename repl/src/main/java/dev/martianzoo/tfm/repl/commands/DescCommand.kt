package dev.martianzoo.tfm.repl.commands

import dev.martianzoo.api.SystemClasses
import dev.martianzoo.engine.Gameplay.Companion.parse
import dev.martianzoo.pets.HasExpression.Companion.expressions
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.tfm.repl.ReplCommand
import dev.martianzoo.tfm.repl.ReplSession
import dev.martianzoo.types.MType
import dev.martianzoo.types.TypeDescription
import dev.martianzoo.util.iff
import dev.martianzoo.util.random

internal class DescCommand(val repl: ReplSession) : ReplCommand("desc") {
  override val usage = "desc <Expression>"
  override val help =
      """
        Put any type expression after `desc` and it will tell you everything it knows about that
        type. A page on github somewhere will explain what all the output means, but it doesn't
        exist yet.
      """
  override val isReadOnly = true

  override fun withArgs(args: String): List<String> {
    val (expression, type) =
        if (args == "random") {
          val type =
              repl.tfm.reader
                  .resolve(SystemClasses.CLASS.expression)
                  .let(repl.tfm.reader::getComponents)
                  .expressions()
                  .map { it.arguments.single() }
                  .random()
                  .let { repl.tfm.reader.resolve(it) as MType }
                  .concreteSubtypesSameClass()
                  .random()
          type.expressionFull to type
        } else {
          val expression: Expression = repl.tfm.parse(args)
          expression to repl.game.reader.resolve(expression) as MType
        }
    return listOf(MTypeToText.describe(expression, type))
  }

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

}