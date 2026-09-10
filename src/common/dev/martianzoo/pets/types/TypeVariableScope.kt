package dev.martianzoo.pets.types

import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.SystemClasses.THIS
import dev.martianzoo.pets.api.TypeInfo
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Expression.Refinement.Not
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.types.Dependency.TypeDependency
import dev.martianzoo.pets.types.TypeVariable.Occurrence
import dev.martianzoo.pets.types.TypeVariable.Site

/**
 * The type-variable declarations and uses visible within one authored choice scope. It preserves
 * occurrence identity through syntax transformations and supports the scoped capture and binding
 * operations specified by
 * [rules 13-10 and 13-11](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
 */
public class TypeVariableScope private constructor(private val entries: List<Entry>) {
  internal data class Entry(
      val variable: TypeVariable,
      val currentExpressions: Map<Occurrence, Expression>,
  )

  /**
   * Variables visible in this scope, in the declaration order required by
   * [rule 13-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
   */
  public val variables: List<TypeVariable> = entries.map(Entry::variable)

  /**
   * Whether this scope contains no type variables, one of the scope queries specified by
   * [rule 13-11](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
   */
  public val isEmpty: Boolean
    get() = entries.isEmpty()

  /**
   * Returns every current spelling of [variable] after preprocessing, as specified by the scope
   * queries in
   * [rule 13-11](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
   */
  public fun expressionsOf(variable: TypeVariable): Set<Expression> =
      entries.single { it.variable === variable }.currentExpressions.values.toSet()

  /**
   * Returns the current expression for [occurrence] after preprocessing its owning syntax, as
   * specified by
   * [rule 13-11](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
   */
  public fun expressionOf(occurrence: Occurrence): Expression =
      entries
          .single { it.variable === occurrence.typeVariable }
          .currentExpressions
          .getValue(occurrence)

  /**
   * Returns the variable declared by this syntax node, if any; declaration is distinct from usage
   * under
   * [rules 13-1 and 13-11](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
   */
  public fun variableDeclaredAt(expression: Expression): TypeVariable? {
    fun Entry.declarationExpression(): Expression? =
        currentExpressions.keys
            .singleOrNull { it is TypeVariable.Declaration }
            ?.let(currentExpressions::getValue)
    return entries.firstOrNull { it.declarationExpression() === expression }?.variable
        ?: entries
            .singleOrNull {
              it.declarationExpression()?.sameAuthoredTypeExpressionAs(expression) == true
            }
            ?.variable
  }

  /**
   * Returns the visible variable used or declared by [expression], if any, following the occurrence
   * query of
   * [rule 13-11](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
   */
  public fun variableAt(expression: Expression): TypeVariable? =
      entries
          .firstOrNull { entry ->
            entry.currentExpressions.values.any { it === expression }
          }
          ?.variable
          ?: entries
              .singleOrNull { entry ->
                entry.currentExpressions.values.any { it.sameAuthoredTypeExpressionAs(expression) }
              }
              ?.variable

  /**
   * Returns this scope with recorded occurrence spellings transformed alongside their owning
   * syntax, preserving the scoped identity required by
   * [rule 13-10](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
   */
  public fun transformedBy(transformer: PetTransformer): TypeVariableScope =
      TypeVariableScope(
          entries
              .filterNot { entry ->
                transformer is BindingTransformer && entry.variable in transformer.boundVariables
              }
              .map { entry ->
                entry.copy(
                    currentExpressions =
                        entry.currentExpressions.mapValues { (_, expression) ->
                          transformer.transformExpression(expression)
                        }
                )
              }
      )

  internal operator fun plus(that: TypeVariableScope): TypeVariableScope =
      when {
        isEmpty -> that
        that.isEmpty -> this
        else -> TypeVariableScope(entries + that.entries)
      }

  internal fun bindings(
      wide: PetNode,
      narrow: PetNode,
      variable: TypeVariable,
  ): List<Expression> = buildList {
    val sources = entries.single { it.variable === variable }.currentExpressions.values

    fun collect(wideNode: PetNode, narrowNode: PetNode) {
      if (wideNode is Expression && wideNode in sources) {
        (narrowNode as? Expression)?.let(::add)
        return
      }
      wideNode.immediateChildren().zip(narrowNode.immediateChildren()).forEach { (wide, narrow) ->
        collect(wide, narrow)
      }
    }

    collect(wide, narrow)
  }

  /** Ground Types supplied for [variable] by narrowing expressions inside [proposed]. */
  internal fun bindingsIn(
      proposed: PetNode,
      variable: TypeVariable,
      info: TypeInfo,
  ): List<GroundType> {
    val declaration = expressionOf(variable.declaration)
    return proposed
        .descendantsOfType<Expression>()
        .filter { it != declaration && it.narrows(variable.bound.expressionFull, info) }
        .map { variable.bound.classTable.resolve(it) }
        .distinct()
  }

  /**
   * Captures variables occurring in [authored] from corresponding structural positions in
   * [specific], relative to [general]. The walk follows the dependency keys selected while
   * resolving [authored]; it performs no class-name substitution or search for coincidentally
   * similar resolved types. This is structural capture from
   * [rule 13-11](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
   */
  public fun bindingsFrom(
      authored: Expression,
      general: GroundType,
      specific: GroundType,
  ): Map<TypeVariable, GroundType> {
    val captures = mutableMapOf<TypeVariable, MutableList<GroundType>>()

    fun record(expression: Expression, captured: GroundType) {
      entries.forEach { entry ->
        if (
            entry.currentExpressions.values.any { it === expression } ||
                entry.currentExpressions.keys.any { it.expression == expression }
        ) {
          captures.getOrPut(entry.variable, ::mutableListOf) += captured
        }
      }
      if (
          entries.none { entry ->
            entry.currentExpressions.values.any { it === expression } ||
                entry.currentExpressions.keys.any { it.expression == expression }
          }
      ) {
        val matching = entries.filter { entry -> expression in entry.currentExpressions.values }
        if (matching.size == 1)
            captures.getOrPut(matching.single().variable, ::mutableListOf) += captured
      }
    }

    fun walk(expression: Expression, wide: GroundType, narrow: GroundType) {
      record(expression, narrow)
      if (expression.arguments.isEmpty()) return

      if (wide.representedClass != null && narrow.representedClass != null) {
        check(expression.arguments.size == 1)
        walk(
            expression.arguments.single(),
            wide.representedClass!!.baseType,
            narrow.representedClass!!.baseType,
        )
        return
      }

      val keys = wide.rootClass.matchDependencyKeys(expression.arguments)
      expression.arguments.zip(keys).forEach { (argument, key) ->
        val wideDependency = wide.dependencies.get(key)
        val narrowDependency = narrow.dependencies.getIfPresent(key) ?: return@forEach
        val wideChild = (wideDependency as? TypeDependency)?.boundType ?: return@forEach
        val narrowChild = (narrowDependency as? TypeDependency)?.boundType ?: return@forEach
        walk(argument, wideChild, narrowChild)
      }
    }

    walk(authored, general, specific)
    return captures.mapValues { (variable, values) ->
      values.distinct().singleOrNull()
          ?: error("Type variable $variable has conflicting captures: ${values.distinct()}")
    }
  }

  /**
   * Returns a transformer that applies captured [bindings] only at recorded occurrences. Each
   * occurrence retains its own arguments, and a declaration refinement already checked during
   * capture is consumed, exactly as specified by
   * [rule 13-10](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
   */
  public fun bind(bindings: Map<TypeVariable, GroundType>): PetTransformer {
    val replacements = entries.flatMap { entry ->
      val replacement = bindings[entry.variable] ?: return@flatMap emptyList()
      val capturedRefinement =
          if (entry.variable.declaration in entry.currentExpressions) {
            entry.currentExpressions.getValue(entry.variable.declaration).refinement
          } else {
            entry.variable.bound.refinement
          }

      fun GroundType.consumeCapturedRefinement(): GroundType =
          if (refinement == capturedRefinement) copy(refinement = null) else this

      val captured = replacement.consumeCapturedRefinement()
      entry.currentExpressions.flatMap { (occurrence, source) ->
        val constraint = replacement.classTable.resolve(source)
        val occurrenceBinding =
            (captured glb constraint.consumeCapturedRefinement())
                ?: throw NarrowingException(
                    "$replacement does not satisfy Type-variable occurrence $source"
                )
        val target = occurrence.expressionFor(occurrenceBinding, source)
        buildList {
          add(source to target)
          if (occurrence.expression != source) {
            runCatching {
                  occurrence.expression to
                      occurrence.expressionFor(replacement, occurrence.expression)
                }
                .getOrNull()
                ?.let(::add)
          }
        }
      }
    }
    return BindingTransformer(
        bindings.keys,
        replacements,
        entries.firstOrNull()?.variable?.bound?.classTable,
    )
  }

  private class BindingTransformer(
      val boundVariables: Set<TypeVariable>,
      private val replacements: List<Pair<Expression, Expression>>,
      private val classTable: ClassTable?,
  ) : PetTransformer() {
    private fun Expression.isExpandedFrom(source: Expression): Boolean {
      if (className != source.className || refinement != source.refinement) {
        return false
      }
      val klass = classTable?.getClass(className) ?: return false
      val actualByKey =
          arguments.zip(klass.matchDependencyKeys(arguments)).associate { it.second to it.first }
      return source.arguments.zip(klass.matchDependencyKeys(source.arguments)).all { (argument, key)
        ->
        actualByKey[key] == argument
      }
    }

    override fun transformNode(node: PetNode): PetNode {
      if (node is Expression) {
        replacements
            .firstOrNull { (source) -> source === node }
            ?.let {
              return transformChildren(it.second)
            }
        val equal = replacements.filter { (source) -> source == node }.map { it.second }.distinct()
        if (equal.size == 1) return transformChildren(equal.single())
        val expanded =
            replacements
                .filter { (source) -> node.isExpandedFrom(source) }
                .map { it.second }
                .distinct()
        if (expanded.size == 1) return transformChildren(expanded.single())
      }
      return transformChildren(node)
    }
  }

  internal companion object {
    val EMPTY: TypeVariableScope = TypeVariableScope(emptyList())

    fun containing(
        variables: List<TypeVariable>,
        root: PetNode,
    ): TypeVariableScope {
      val expressions = root.descendantsOfType<Expression>().toList()
      val entries = variables.mapNotNull { variable ->
        val current =
            variable.occurrences
                .filter { occurrence ->
                  expressions.any { it === occurrence.expression }
                }
                .associateWith { it.expression }
        current.takeIf { it.isNotEmpty() }?.let { Entry(variable, it) }
      }
      return if (entries.isEmpty()) EMPTY else TypeVariableScope(entries)
    }

    fun infer(
        regions: List<PetNode>,
        classTable: ClassTable,
        includeRegionRoots: Boolean = true,
        explicitDeclarations: List<Expression> = emptyList(),
        visibleScope: TypeVariableScope = EMPTY,
    ): TypeVariableScope {
      data class Found(
          val expression: Expression,
          val region: Int,
          val ordinal: Int,
          val ancestors: Set<Expression>,
          val inRequirement: Boolean,
          val directlyCounted: Boolean,
      )

      var ordinal = 0
      val occurrences = buildList {
        fun collect(
            node: PetNode,
            region: Int,
            ancestors: Set<Expression>,
            inRequirement: Boolean,
            directlyCounted: Boolean,
            regionRoot: Boolean,
        ) {
          val expression = node as? Expression
          val nextAncestors = expression?.let { ancestors + it } ?: ancestors
          if (expression != null && (includeRegionRoots || !regionRoot)) {
            add(
                Found(
                    expression,
                    region,
                    ordinal++,
                    ancestors,
                    inRequirement,
                    directlyCounted,
                )
            )
          }
          node.immediateChildren().forEach { child ->
            collect(
                child,
                region,
                nextAncestors,
                inRequirement || node is Requirement,
                node is Metric.Count && child is Expression,
                false,
            )
          }
        }

        regions.forEachIndexed { index, region ->
          collect(region, index, emptySet(), false, false, true)
        }
      }

      fun interpretedGroundType(found: Found): GroundType {
        val expression = found.expression
        val nonStructuralRefinement = expression.refinement?.retaining { it !is Not }
        return classTable.resolve(expression.copy(refinement = nonStructuralRefinement))
      }

      val explicitIdentities = explicitDeclarations
      val explicitEntries = explicitDeclarations.map { expression ->
        val declaration = occurrences.single { it.expression === expression }
        val declarationIdentity = expression
        val usages =
            occurrences
                .filter { found ->
                  found !== declaration &&
                      found.expression.sameAuthoredTypeExpressionAs(declarationIdentity)
                }
                .sortedBy(Found::ordinal)
        val variable =
            TypeVariable(
                classTable.resolve(expression),
                Site(
                    expression,
                    declaration.region,
                    declaration.ordinal,
                    interpretedGroundType = classTable.resolve(expression),
                ),
                usages.map { usage ->
                  Site(
                      usage.expression,
                      usage.region,
                      usage.ordinal,
                      interpretedGroundType = classTable.resolve(usage.expression),
                  )
                },
            )
        Entry(variable, variable.occurrences.associateWith { it.expression })
      }

      val grouped =
          occurrences
              .filterNot(Found::directlyCounted)
              .filterNot { found ->
                explicitIdentities.any(found.expression::sameAuthoredTypeExpressionAs)
              }
              .filterNot { visibleScope.variableAt(it.expression) != null }
              .groupBy { it.expression.toString() }
      val candidates =
          grouped
              .filter { (_, found) ->
                val source = found.first().expression
                source.className != THIS &&
                    runCatching { classTable.resolve(source).abstract }.getOrDefault(false) &&
                    found.map(Found::region).distinct().size >= 2 &&
                    !found.all(Found::inRequirement)
              }
              .toList()
              .sortedBy { (_, found) -> found.minOf { it.ancestors.size } }

      val selected = linkedSetOf<String>()
      val entries = buildList {
        for ((_, found) in candidates) {
          val source = found.first().expression
          if (found.all { occurrence -> occurrence.ancestors.any { it.toString() in selected } }) {
            continue
          }
          selected += source.toString()

          val ordered = found.sortedBy(Found::ordinal)
          val declarationFound = ordered.first()
          val type = interpretedGroundType(declarationFound)
          val variable =
              TypeVariable(
                  type,
                  Site(
                      declarationFound.expression,
                      declarationFound.region,
                      declarationFound.ordinal,
                      interpretedGroundType = type,
                  ),
                  ordered.drop(1).map { occurrence ->
                    Site(
                        occurrence.expression,
                        occurrence.region,
                        occurrence.ordinal,
                        interpretedGroundType = interpretedGroundType(occurrence),
                    )
                  },
              )
          add(Entry(variable, variable.occurrences.associateWith { it.expression }))
        }
      }
      val allEntries = explicitEntries + entries
      return if (allEntries.isEmpty()) EMPTY else TypeVariableScope(allEntries)
    }
  }
}
