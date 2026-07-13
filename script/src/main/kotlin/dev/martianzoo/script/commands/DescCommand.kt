package dev.martianzoo.script.commands

import dev.martianzoo.api.SystemClasses.CLASS
import dev.martianzoo.engine.Gameplay.Companion.parse
import dev.martianzoo.pets.HasExpression.Companion.expressions
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.script.PetsCompletionRoot
import dev.martianzoo.script.ScriptCommand
import dev.martianzoo.script.ScriptCompletion
import dev.martianzoo.script.ScriptCompletionContext
import dev.martianzoo.script.ScriptSession
import dev.martianzoo.types.MType
import dev.martianzoo.types.TypeDescription
import dev.martianzoo.util.iff
import dev.martianzoo.util.random

internal class DescCommand(private val repl: ScriptSession) : ScriptCommand("desc") {
  override val usage = "desc <Expression>"
  override val help =
      """
        Put any type expression after `desc` and it will tell you everything it knows about that
        type. A page on github somewhere will explain what all the output means, but it doesn't
        exist yet.
      """
  override val isReadOnly = true

  override fun completions(context: ScriptCompletionContext): List<ScriptCompletion> =
      context.petsWords(PetsCompletionRoot.EXPRESSION)

  override fun withArgs(args: String): List<String> {
    val (expression, type) =
        if (args == "random") {
          val type =
              repl.gameplay
                  .resolve("$CLASS")
                  .let(repl.game.reader::getComponents)
                  .expressions()
                  .map { it.arguments.single() }
                  .random()
                  .let { repl.game.reader.resolve(it) as MType }
                  .concreteSubtypesSameClass()
                  .random()
          type.expressionFull to type
        } else {
          val expression: Expression = repl.gameplay.parse(args)
          expression to repl.gameplay.resolve(args) as MType
        }
    return listOf(MTypeToText.describe(expression, type))
  }

  object MTypeToText {
    /** A detailed multi-line description of a type. */
    internal fun describe(expression: Expression, mtype: MType): String {

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
            docstring:   ${desc.docstring}
            subclasses:  $subclassesDisplay
            subclasses:  ${desc.superclassNames}
            invariants:  ${desc.classInvariants.joinToString().ifEmpty { "(none)" }}
            base type:   ${desc.baseType.expressionFull}
            cmpt types:  ${desc.concreteTypesForThisClassCount}
            raw fx:      ${desc.rawClassEffects.joinToString("""
                         """)}
            class fx:    ${desc.classEffects.joinToString("""
                         """)}


        """
              .trimIndent()

      val typeStuff =
          """
          Expression `$expression`:
            std. form:   ${mtype.expression}
            long form:   ${mtype.expressionFull}
            supertypes:  ${desc.supertypes.joinToString { "${it.expressionFull}" }}
            cmpt types:  ${desc.componentTypesCount}
            subs:        ${desc.substitutions}
        """
              .trimIndent()

      val componentStuff =
          if (mtype.abstract) {
            ""
          } else {
            """


            Component `${mtype.expressionFull}`:
              effects:     ${desc.componentEffects.joinToString("""
                           """)}
          """
                .trimIndent()
          }

      return classStuff + typeStuff + componentStuff
    }
  }
}
