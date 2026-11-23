package dev.martianzoo.tfm.script.commands

import dev.martianzoo.agent.Agent.Companion.parse
import dev.martianzoo.engine.compiledComponentEffects
import dev.martianzoo.pets.HasClassName.Companion.classNames
import dev.martianzoo.pets.HasExpression.Companion.expressions
import dev.martianzoo.pets.PetElaborator
import dev.martianzoo.pets.api.SystemClasses.CLASS
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.pets.types.Type
import dev.martianzoo.pets.util.random
import dev.martianzoo.pets.util.toSetStrict
import dev.martianzoo.tfm.script.PetsCompletionRoot
import dev.martianzoo.tfm.script.ScriptCommand
import dev.martianzoo.tfm.script.ScriptCompletion
import dev.martianzoo.tfm.script.ScriptCompletionContext
import dev.martianzoo.tfm.script.ScriptSession

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
          val expression: Expression = repl.parse(args)
          expression to repl.resolve(args)
        }
    return listOf(TypeToText.describe(expression, type, repl.game.classTable))
  }

  private object TypeToText {
    /** A detailed multi-line description of a type. */
    internal fun describe(
        expression: Expression,
        type: Type,
        classTable: ClassTable,
    ): String {
      val rootClass = type.rootClass
      val long = type.className
      val superclasses = rootClass.allSuperclasses()
      val subs =
          classTable
              .allSubclasses(rootClass)
              .sortedWith(compareBy({ -classTable.allSubclasses(it).size }, { it.className }))
              .classNames()
              .toSetStrict() - long
      val classEffects =
          if (classTable.isInhabited(type)) PetElaborator(classTable).classEffects(rootClass)
          else emptyList()
      val concreteTypesForThisClassCount =
          classTable.concreteSubtypesSameClass(rootClass.baseType).take(100).count()
      val supertypes = superclasses.map { it.withAllDependencies(type.dependencies) }
      val variableBindings =
          type.variableBindingsFrom(rootClass.defaultType, rootClass.typeVariables)
      val componentTypesCount = classTable.allConcreteSubtypes(type).take(100).count()
      val componentEffects = compiledComponentEffects(classTable, type)
      val subclassesDisplay =
          when (subs.size) {
            0 -> "(none)"
            in 1..7 -> subs.joinToString()
            else -> subs.take(6).joinToString() + " (${subs.size - 6} others)"
          }

      val classStuff =
          """
          Class `$long`:
            docstring:   ${rootClass.docstring}
            subclasses:  $subclassesDisplay
            superclasses: ${superclasses.classNames().joinToString()}
            invariants:  ${rootClass.invariants.joinToString().ifEmpty { "(none)" }}
            base type:   ${rootClass.baseType.expressionFull}
            cmpt types:  ${concreteTypesForThisClassCount}
            raw fx:      ${rootClass.declaration.effects.joinToString("""
                         """)}
            class fx:    ${classEffects.joinToString("""
                         """)}


        """
              .trimIndent()

      val typeStuff =
          """
          Expression `$expression`:
            std. form:   ${type.expression}
            long form:   ${type.expressionFull}
            supertypes:  ${supertypes.joinToString { it.expressionFull.toString() }}
            cmpt types:  ${componentTypesCount}
            variables:   ${variableBindings.entries.joinToString(prefix = "{", postfix = "}") { (variable, value) -> "${variable.declaration.expression}=${value.expression}" }}
        """
              .trimIndent()

      val componentStuff =
          if (type.abstract) {
            ""
          } else {
            """


            Component `${type.expressionFull}`:
              effects:     ${componentEffects.joinToString("""
                           """)}
          """
                .trimIndent()
          }

      return classStuff + typeStuff + componentStuff
    }
  }
}
