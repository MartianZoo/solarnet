package dev.martianzoo.pets.types

import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.SystemClasses.CLASS
import dev.martianzoo.pets.api.TypeInfo
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Expression.Refinement.Has
import dev.martianzoo.pets.ast.Expression.Refinement.Not
import dev.martianzoo.pets.ast.Expression.TypeVariableName.Declaration
import dev.martianzoo.pets.ast.Expression.TypeVariableName.ExpandedReference
import dev.martianzoo.pets.ast.Expression.TypeVariableName.Reference
import dev.martianzoo.pets.ast.Instruction.Or
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.startsTypeVariableObservation
import dev.martianzoo.pets.types.Dependency.TypeDependency
import dev.martianzoo.pets.types.TypeVariable.Occurrence
import dev.martianzoo.pets.types.TypeVariable.Site

/**
 * The type-variable declarations and uses visible within one authored choice scope. It preserves
 * occurrence identity through syntax transformations and supports the scoped capture and binding
 * operations specified by
 * [rules T13-10 and T13-11](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
 */
public class TypeVariableScope private constructor(private val entries: List<Entry>) {
  internal data class Entry(
      val variable: TypeVariable,
      val currentExpressions: Map<Occurrence, Expression>,
  )

  /**
   * Variables visible in this scope, in the declaration order required by
   * [rule T13-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
   */
  public val variables: List<TypeVariable> = entries.map(Entry::variable)

  /** Whether this scope contains no type variables. */
  public val isEmpty: Boolean
    get() = entries.isEmpty()

  /**
   * Returns every current spelling of [variable] after preprocessing. Preprocessed copies of an
   * occurrence remain occurrences under
   * [rule T13-10](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
   */
  public fun expressionsOf(variable: TypeVariable): Set<Expression> =
      entries.single { it.variable === variable }.currentExpressions.values.toSet()

  /** Returns the current expression for [occurrence] after preprocessing its owning syntax. */
  public fun expressionOf(occurrence: Occurrence): Expression =
      entries
          .single { it.variable === occurrence.typeVariable }
          .currentExpressions
          .getValue(occurrence)

  /**
   * Returns the variable whose supplying occurrence is this syntax node, if any; the supplying
   * occurrence is distinct from the others under
   * [rule T13-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
   */
  public fun variableDeclaredAt(expression: Expression): TypeVariable? {
    fun Entry.declarationExpression(): Expression? =
        currentExpressions.keys
            .singleOrNull { it is TypeVariable.Declaration }
            ?.let(currentExpressions::getValue)
    return entries.firstOrNull { it.declarationExpression() === expression }?.variable
        ?: entries
            .singleOrNull {
              it.declarationExpression() == expression
            }
            ?.variable
  }

  /** Returns the visible variable used or declared by [expression], if any. */
  public fun variableAt(expression: Expression): TypeVariable? =
      entries
          .firstOrNull { entry ->
            entry.currentExpressions.values.any { it === expression }
          }
          ?.variable
          ?: expression.typeVariableName?.let { marker ->
            entries
                .singleOrNull { entry ->
                  entry.currentExpressions.values.any {
                    it.typeVariableName?.identity == marker.identity
                  }
                }
                ?.variable
          }

  /**
   * Returns this scope with recorded occurrence spellings transformed alongside their owning
   * syntax, preserving the scoped identity required by
   * [rule T13-10](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
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

  /** Hides explicit names while retaining their occurrence identity outside the lexical scope. */
  public fun expandNames(): PetTransformer {
    val references =
        variables
            .mapNotNull { it.declaration.expression.typeVariableName as? Declaration }
            .associate { marker ->
              marker.identity to
                  ExpandedReference(
                      marker.name,
                      marker.boundClassName,
                      marker.resolution,
                  )
            }
    return object : PetTransformer() {
      override fun transformNode(node: PetNode): PetNode {
        if (node is Expression) {
          if (node.typeVariableName is ExpandedReference) return transformChildren(node)
          references[node.typeVariableName?.identity]?.let { reference ->
            return transformChildren(node.copy(typeVariableName = reference))
          }
        }
        return transformChildren(node)
      }
    }
  }

  internal fun retaining(variables: Set<TypeVariable>): TypeVariableScope =
      TypeVariableScope(entries.filter { it.variable in variables })

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
      info: TypeInfo,
      classTable: ClassTable = variable.bound.classTable,
  ): List<Expression> {
    fun structural(type: GroundType): GroundType =
        type.copy(
            refinement = type.refinement?.retaining { it !is Has },
            dependencies = type.dependencies.mapWithKey { _, bound -> structural(bound) },
        )
    val shapeInfo =
        object : TypeInfo by info {
          override fun ensureNarrows(wide: Expression, narrow: Expression) {
            structural(classTable.resolve(narrow))
                .ensureNarrows(structural(classTable.resolve(wide)), this)
          }

          override fun ensureSelectionNarrows(wide: Expression, narrow: Expression) =
              ensureNarrows(wide, narrow)

          override fun has(requirement: dev.martianzoo.pets.ast.Requirement): Boolean = true
        }
    fun collect(w: PetNode, n: PetNode): List<Expression> {
      if (w is Or) {
        val proposals = if (n is Or) n.instructions else listOf(n)
        return proposals.flatMap { proposal ->
          val instruction = proposal as? InstructionTree ?: return@flatMap emptyList()
          val alternatives =
              w.instructions
                  .filter { instruction.narrows(it, shapeInfo) }
                  .map { collect(it, proposal).distinct() }
          if (alternatives.map { it.toSet() }.distinct().size > 1) {
            throw NarrowingException("ambiguous `OR` binding for type variable `$variable`")
          }
          alternatives.firstOrNull().orEmpty()
        }
      }
      if (w is Expression && n is Expression) {
        // Retaining a reference leaves its declaration's choice open, including inserted defaults.
        if (w == n && w.descendantsOfType<Expression>().any { variableAt(it) === variable })
            return listOf(expressionOf(variable.declaration))
        return listOfNotNull(
            bindingsFrom(w, classTable.resolve(w), classTable.resolve(n), classTable)[variable]
                ?.expression
        )
      }
      if (w::class != n::class) return emptyList()
      return w.immediateChildren().zip(n.immediateChildren()).flatMap { (wc, nc) ->
        collect(wc, nc)
      }
    }
    return collect(wide, narrow)
  }

  private fun matchingExpressions(
      wide: PetNode,
      narrow: PetNode,
      info: TypeInfo,
      visit: (Expression, Expression) -> Unit,
  ) {
    if (wide is Or) {
      val proposals = if (narrow is Or) narrow.instructions else listOf(narrow)
      for (proposal in proposals) {
        val instruction = proposal as? InstructionTree ?: continue
        wide.instructions
            .filter { instruction.narrows(it, info) }
            .forEach {
              matchingExpressions(it, proposal, info, visit)
            }
      }
    } else if (wide is Expression && narrow is Expression) {
      visit(wide, narrow)
    } else if (wide::class == narrow::class) {
      wide.immediateChildren().zip(narrow.immediateChildren()).forEach { (w, n) ->
        matchingExpressions(w, n, info, visit)
      }
    }
  }

  /** Unsettled choices keep their aliases and predicates in a proposal that remains pending. */
  internal fun ensureChoicesRetained(
      wide: PetNode,
      narrow: PetNode,
      info: TypeInfo,
      classTable: ClassTable,
      liveVariables: Set<TypeVariable>,
  ) {
    fun check(w: Expression, n: Expression) {
      if (variableAt(w) in liveVariables && w.typeVariableName?.key != n.typeVariableName?.key) {
        throw NarrowingException("unsettled type-variable occurrence `$w` must retain its marker")
      }
      val wideType = classTable.resolve(w)
      val narrowType = classTable.resolve(n)
      val narrowClauses = n.refinement?.conjuncts().orEmpty()
      w.refinement?.conjuncts()?.forEach { clause ->
        val observesChoice =
            clause.descendantsOfType<Expression>().any {
              variableAt(it) in liveVariables
            }
        val retained = narrowClauses.firstOrNull {
          it.withoutChoiceNames() == clause.withoutChoiceNames()
        }
        if (observesChoice && retained == null) {
          throw NarrowingException("unsettled choice `$n` must retain `$clause`")
        }
        if (observesChoice) {
          retained?.let {
            matchingExpressions(clause, it, info, ::check)
          }
        }
      }
      val narrowArguments =
          n.arguments
              .zip(narrowType.rootClass.matchDependencyKeys(n.arguments, classTable))
              .associate { (argument, key) -> key to argument }
      w.arguments.zip(wideType.rootClass.matchDependencyKeys(w.arguments, classTable)).forEach {
          (argument, key) ->
        val corresponding =
            narrowArguments[key]
                ?: (narrowType.dependencies.getIfPresent(key) as? TypeDependency)
                    ?.boundType
                    ?.expression
                ?: return@forEach
        check(argument, corresponding)
      }
    }
    matchingExpressions(wide, narrow, info, ::check)
  }

  /**
   * Captures variables occurring in [authored] from corresponding structural positions in
   * [specific], relative to [general]. The walk follows the dependency keys selected while
   * resolving [authored]; it performs no class-name substitution or search for coincidentally
   * similar resolved types. This is structural capture from
   * [rule T13-11](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
   */
  public fun bindingsFrom(
      authored: Expression,
      general: GroundType,
      specific: GroundType,
      classTable: ClassTable =
          requireNotNull(general.classTable.commonTable(specific.classTable)) {
            "`$general` and `$specific` belong to unrelated class tables"
          },
  ): Map<TypeVariable, GroundType> {
    val captures = mutableMapOf<TypeVariable, MutableList<GroundType>>()

    fun record(expression: Expression, captured: GroundType) {
      variableAt(expression)?.let { variable ->
        captures.getOrPut(variable, ::mutableListOf) +=
            if (variable.selectsClass) captured.rootClass.baseType else captured
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

      val keys = wide.rootClass.matchDependencyKeys(expression.arguments, classTable)
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
          ?: throw NarrowingException(
              "type variable `$variable` has conflicting captures: `${values.distinct()}`"
          )
    }
  }

  /**
   * Returns a transformer that applies captured [bindings] only at recorded occurrences. Each
   * occurrence retains its own arguments. Checked supplier `HAS` predicates are consumed only at
   * matching paths with concrete chosen Classes and dependencies; other constraints remain, per
   * [rule T13-10](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
   * Callers with no [bindings] must supply [classTable] explicitly.
   */
  public fun bind(
      bindings: Map<TypeVariable, GroundType>,
      classTable: ClassTable =
          bindings.values.firstOrNull()?.classTable
              ?: error("empty bindings require an explicit class table"),
  ): PetTransformer {
    val selectedBindings = bindings.mapValues { (variable, value) ->
      if (variable.selectsClass) value.rootClass.baseType else value
    }
    fun settled(variable: TypeVariable, value: GroundType): Boolean =
        if (variable.selectsClass) !value.rootClass.abstract else !value.abstract

    fun capturedType(entry: Entry): GroundType =
        entry.currentExpressions[entry.variable.declaration]?.let(classTable::resolve)
            ?: entry.variable.bound

    fun consume(type: GroundType, supplier: GroundType, chosen: GroundType): GroundType =
        type.copy(
            refinement =
                type.refinement?.retaining {
                  it !is Has ||
                      it !in supplier.refinement?.conjuncts().orEmpty() ||
                      it in chosen.refinement?.conjuncts().orEmpty() ||
                      chosen.rootClass.abstract ||
                      chosen.dependencies.abstract
                },
            dependencies =
                type.dependencies.mapWithKey { key, bound ->
                  val supplierDependency =
                      supplier.dependencies.getIfPresent(key) as? TypeDependency
                  val chosenDependency = chosen.dependencies.getIfPresent(key) as? TypeDependency
                  if (supplierDependency != null && chosenDependency != null) {
                    consume(bound, supplierDependency.boundType, chosenDependency.boundType)
                  } else {
                    bound
                  }
                },
        )

    val replacements = entries.flatMap { entry ->
      val replacement = selectedBindings[entry.variable] ?: return@flatMap emptyList()
      val supplyingType = capturedType(entry)
      entry.currentExpressions.flatMap { (occurrence, source) ->
        val constraint = consume(classTable.resolve(source), supplyingType, replacement)
        val occurrenceBinding =
            classTable.glb(replacement, constraint)
                ?: throw NarrowingException(
                    "`$replacement` does not satisfy type-variable occurrence `$source`"
                )
        val target =
            occurrence.expressionFor(occurrenceBinding, source, classTable).let { applied ->
              val marker = source.typeVariableName
              if (!settled(entry.variable, replacement) && marker != null) {
                applied.copy(
                    typeVariableName =
                        ExpandedReference(marker.name, marker.boundClassName, marker.resolution)
                )
              } else applied
            }
        buildList {
          add(source to target)
          if (occurrence.expression != source) {
            runCatching {
                  occurrence.expression to
                      occurrence.expressionFor(replacement, occurrence.expression, classTable)
                }
                .getOrNull()
                ?.let(::add)
          }
        }
      }
    }
    val transformer =
        BindingTransformer(
            selectedBindings.filter { (variable, value) -> settled(variable, value) }.keys,
            replacements,
            classTable,
        )
    entries.forEach { entry ->
      val replacement = selectedBindings[entry.variable] ?: return@forEach
      val supplyingType = capturedType(entry)
      entry.currentExpressions.values.forEach { source ->
        val specializedSource = transformer.transformExpressionChildren(source)
        val constraint = consume(classTable.resolve(specializedSource), supplyingType, replacement)
        if (classTable.glb(replacement, constraint) == null) {
          throw NarrowingException(
              "`$replacement` does not satisfy specialized type-variable occurrence " +
                  "`$specializedSource`"
          )
        }
      }
    }
    return transformer
  }

  private class BindingTransformer(
      val boundVariables: Set<TypeVariable>,
      private val replacements: List<Pair<Expression, Expression>>,
      private val classTable: ClassTable,
  ) : PetTransformer() {
    fun transformExpressionChildren(node: Expression): Expression =
        transformChildren(node) as Expression

    override fun transformNode(node: PetNode): PetNode {
      if (node is Expression) {
        if (node.className == CLASS && node.arguments.singleOrNull()?.typeVariableName != null) {
          val transformed = transformChildren(node) as Expression
          val represented =
              transformed.arguments
                  .single()
                  .copy(
                      arguments = emptyList(),
                      argumentsSpecified = false,
                      refinement = null,
                  )
          return transformed.copy(arguments = listOf(represented))
        }
        replacements
            .firstOrNull { (source) -> source === node }
            ?.let {
              return transformChildren(it.second)
            }
        fun Expression.hasSameVariableIdentityAs(source: Expression): Boolean =
            source.typeVariableName != null &&
                typeVariableName?.identity == source.typeVariableName.identity

        val equal =
            replacements
                .filter { (source) ->
                  node.hasSameVariableIdentityAs(source) && source == node
                }
                .map { it.second }
                .distinct()
        if (equal.size == 1) return transformChildren(equal.single())
        val expanded =
            replacements
                .filter { (source) ->
                  node.hasSameVariableIdentityAs(source) && node.isExpandedFrom(source, classTable)
                }
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
        val identity = variable.declaration.expression.typeVariableName?.identity
        val recorded = variable.occurrences.map { it.expression }
        val present = expressions.filter { expression ->
          recorded.any { it === expression } ||
              (identity != null && expression.typeVariableName?.identity == identity)
        }
        if (present.isEmpty()) return@mapNotNull null
        if (present.all { expression -> recorded.any { it === expression } }) {
          val current =
              variable.occurrences
                  .filter { occurrence -> present.any { it === occurrence.expression } }
                  .associateWith { it.expression }
          Entry(variable, current)
        } else {
          // Elaboration may introduce a new use of an inherited header name. Its lexical
          // identity is the header marker's identity, even though no authored Site existed yet.
          val origin = variable.declaration
          val declarationExpression =
              present.firstOrNull {
                it.typeVariableName is Declaration && it.typeVariableName?.identity == identity
              } ?: origin.expression
          val usages = present.filterNot { it === declarationExpression }
          val extended =
              TypeVariable(
                  variable.bound,
                  TypeVariable.Site(
                      declarationExpression,
                      origin.region,
                      origin.ordinal,
                      interpretedGroundType = origin.groundType,
                      representedClass = variable.selectsClass,
                  ),
                  usages.mapIndexed { index, expression ->
                    TypeVariable.Site(expression, region = 1, ordinal = index)
                  },
              )
          Entry(
              extended,
              extended.occurrences
                  .filter { occurrence -> present.any { it === occurrence.expression } }
                  .associateWith { it.expression },
          )
        }
      }
      return if (entries.isEmpty()) EMPTY else TypeVariableScope(entries)
    }

    fun fromDeclarations(
        regions: List<PetNode>,
        classTable: ClassTable,
        markedDeclarations: List<Expression> = emptyList(),
    ): TypeVariableScope {
      data class Found(
          val expression: Expression,
          val region: Int,
          val ordinal: Int,
          val observing: Boolean,
          val representedClass: Boolean,
      )

      var ordinal = 0
      val occurrences = buildList {
        fun collect(
            node: PetNode,
            region: Int,
            observing: Boolean,
            representedClass: Boolean = false,
        ) {
          val expression = node as? Expression
          if (expression != null) {
            add(
                Found(
                    expression,
                    region,
                    ordinal++,
                    observing,
                    representedClass,
                )
            )
          }
          val childrenObserve = observing || node.startsTypeVariableObservation
          node.immediateChildren().forEach { child ->
            collect(
                child,
                region,
                childrenObserve,
                node is Expression &&
                    node.className == CLASS &&
                    child === node.arguments.singleOrNull(),
            )
          }
        }

        regions.forEachIndexed { index, region ->
          collect(region, index, false)
        }
      }

      fun interpretedGroundType(found: Found): GroundType {
        val expression = found.expression
        val nonStructuralRefinement = expression.refinement?.retaining { it !is Not }
        return classTable.resolve(expression.copy(refinement = nonStructuralRefinement))
      }

      val entries =
          markedDeclarations
              .map { expression ->
                val declaration = occurrences.single { it.expression === expression }
                if (declaration.observing) {
                  throw ExpressionException(
                      "A Type variable cannot be declared in an observing expression: $expression"
                  )
                }
                val declarationMarker = expression.typeVariableName as Declaration
                val usages =
                    occurrences
                        .filter { found ->
                          found !== declaration &&
                              (found.expression.typeVariableName as? Reference)?.identity ==
                                  declarationMarker.identity
                        }
                        .sortedBy(Found::ordinal)
                val declarationGroundType = interpretedGroundType(declaration)
                val variable =
                    TypeVariable(
                        declarationGroundType,
                        Site(
                            expression,
                            declaration.region,
                            declaration.ordinal,
                            interpretedGroundType = declarationGroundType,
                            representedClass = declaration.representedClass,
                        ),
                        usages.map { usage ->
                          Site(
                              usage.expression,
                              usage.region,
                              usage.ordinal,
                              interpretedGroundType = interpretedGroundType(usage),
                          )
                        },
                    )
                Entry(variable, variable.occurrences.associateWith { it.expression })
              }
              .sortedBy { it.variable.declaration.ordinal }
      return if (entries.isEmpty()) EMPTY else TypeVariableScope(entries)
    }
  }
}

internal fun Expression.isExpandedFrom(source: Expression, classTable: ClassTable): Boolean {
  if (className != source.className || refinement != source.refinement) {
    return false
  }
  val klass = classTable.findClass(className) ?: return false
  return try {
    val actualByKey =
        arguments.zip(klass.matchDependencyKeys(arguments, classTable)).associate {
          it.second to it.first
        }
    source.arguments.zip(klass.matchDependencyKeys(source.arguments, classTable)).all {
        (argument, key) ->
      actualByKey[key] == argument
    }
  } catch (_: ExpressionException) {
    false
  }
}
