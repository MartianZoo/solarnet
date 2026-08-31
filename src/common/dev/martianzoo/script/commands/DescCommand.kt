package dev.martianzoo.script.commands

import dev.martianzoo.engine.Agent.Companion.parse
import dev.martianzoo.engine.TypeDescription
import dev.martianzoo.pets.HasExpression.Companion.expressions
import dev.martianzoo.pets.Vocabulary
import dev.martianzoo.pets.api.SystemClasses.CLASS
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.types.Type
import dev.martianzoo.pets.util.random
import dev.martianzoo.script.PetsCompletionRoot
import dev.martianzoo.script.ScriptCommand
import dev.martianzoo.script.ScriptCompletion
import dev.martianzoo.script.ScriptCompletionContext
import dev.martianzoo.script.ScriptSession

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
              repl.agent
                  .resolve("$CLASS")
                  .let(repl.game.reader::getComponents)
                  .expressions()
                  .map { it.arguments.single() }
                  .random()
                  .let(repl.game.reader::resolve)
                  .let(repl.game.classTable::concreteSubtypesSameClass)
                  .random()
          type.expressionFull to type
        } else {
          val expression: Expression = repl.agent.parse(args)
          expression to repl.agent.resolve(args)
        }
    return listOf(TypeToText.describe(expression, type, repl.game.classTable, repl.game.vocabulary))
  }

  private object TypeToText {
    /** A detailed multi-line description of a type. */
    internal fun describe(
        expression: Expression,
        type: Type,
        classTable: dev.martianzoo.pets.types.ClassTable,
        vocabulary: Vocabulary,
    ): String {

      val desc = TypeDescription(classTable, type)

      val long = type.className
      val altName = vocabulary.petsName(long)

      val subs = desc.subclassNames - long
      val subclassesDisplay =
          when (subs.size) {
            0 -> "(none)"
            in 1..7 -> subs.joinToString { vocabulary.petsName(it).toString() }
            else ->
                subs.take(6).joinToString { vocabulary.petsName(it).toString() } +
                    " (${subs.size - 6} others)"
          }

      val classStuff =
          """
          Class `$long`:
            alt name:    $altName
            docstring:   ${desc.docstring}
            subclasses:  $subclassesDisplay
            superclasses: ${desc.superclassNames.joinToString { vocabulary.petsName(it).toString() }}
            invariants:  ${desc.classInvariants.joinToString { vocabulary.renderPets(it) }.ifEmpty { "(none)" }}
            base type:   ${vocabulary.renderPets(desc.baseType.expressionFull)}
            cmpt types:  ${desc.concreteTypesForThisClassCount}
            raw fx:      ${desc.rawClassEffects.joinToString("""
                         """) { vocabulary.renderPets(it) }}
            class fx:    ${desc.classEffects.joinToString("""
                         """) { vocabulary.renderPets(it) }}


        """
              .trimIndent()

      val typeStuff =
          """
          Expression `${vocabulary.renderPets(expression)}`:
            std. form:   ${vocabulary.renderPets(type.expression)}
            long form:   ${vocabulary.renderPets(type.expressionFull)}
            supertypes:  ${desc.supertypes.joinToString { vocabulary.renderPets(it.expressionFull) }}
            cmpt types:  ${desc.componentTypesCount}
            subs:        ${desc.substitutions.entries.joinToString(prefix = "{", postfix = "}") { (key, value) -> "${vocabulary.petsName(key)}=${vocabulary.renderPets(value)}" }}
        """
              .trimIndent()

      val componentStuff =
          if (type.abstract) {
            ""
          } else {
            """


            Component `${vocabulary.renderPets(type.expressionFull)}`:
              effects:     ${desc.componentEffects.joinToString("""
                           """) { vocabulary.renderPets(it) }}
          """
                .trimIndent()
          }

      return classStuff + typeStuff + componentStuff
    }
  }
}
