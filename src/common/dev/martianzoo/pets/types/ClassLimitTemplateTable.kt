package dev.martianzoo.pets.types

import dev.martianzoo.pets.api.Exceptions.InvalidPetDefinitionException
import dev.martianzoo.pets.api.SystemClasses.THIS
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Expression.Refinement.Not
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Requirement.Counting

/** Stable interpretations of master-universe component-limit declarations. */
internal class ClassLimitTemplateTable(private val masterTable: ClassTable) {
  internal data class Template(
      val expression: Expression,
      val range: IntRange,
      val fixedType: Type?,
  )

  private val masterTemplates = mutableMapOf<Class, List<Template>>()

  internal fun templatesFor(klass: Class): List<Template> =
      if (klass.classTable === masterTable) {
        masterTemplates.getOrPut(klass) { compile(klass, masterTable) }
      } else {
        compile(klass, klass.classTable)
      }

  private fun compile(klass: Class, resolutionTable: ClassTable): List<Template> =
      klass.invariants.map { invariant ->
        val counting =
            invariant as? Counting
                ?: throw InvalidPetDefinitionException(
                    "class invariant on `${klass.className}` is not a counting requirement: " +
                        "`$invariant`"
                )
        val expression =
            (counting.metric as? Metric.Count)?.expression
                ?: throw InvalidPetDefinitionException(
                    "class invariant on `${klass.className}` must count one component expression: " +
                        "`$invariant`"
                )
        Template(
            expression,
            counting.range,
            expression
                .takeUnless {
                  THIS in it.descendantsOfType<ClassName>() ||
                      it.descendantsOfType<Not>().isNotEmpty()
                }
                ?.let(resolutionTable::resolve),
        )
      }
}
