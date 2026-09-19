package dev.martianzoo.pets.types

import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.api.TypeInfo
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Expression.Refinement.Not
import dev.martianzoo.pets.ast.Expression.TypeVariableName.Declaration
import dev.martianzoo.pets.ast.Expression.TypeVariableName.ExpandedReference
import dev.martianzoo.pets.ast.Expression.TypeVariableName.Reference
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

  /**
   * Whether this scope contains no type variables, one of the scope queries specified by
   * [rule T13-11](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
   */
  public val isEmpty: Boolean
    get() = entries.isEmpty()

  /**
   * Returns every current spelling of [variable] after preprocessing, as specified by the scope
   * queries in
   * [rule T13-11](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
   */
  public fun expressionsOf(variable: TypeVariable): Set<Expression> =
      entries.single { it.variable === variable }.currentExpressions.values.toSet()

  /**
   * Returns the current expression for [occurrence] after preprocessing its owning syntax, as
   * specified by
   * [rule T13-11](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
   */
  public fun expressionOf(occurrence: Occurrence): Expression =
      entries
          .single { it.variable === occurrence.typeVariable }
          .currentExpressions
          .getValue(occurrence)

  /**
   * Returns the variable declared by this syntax node, if any; declaration is distinct from usage
   * under
   * [rules T13-1 and T13-11](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
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

  /**
   * Returns the visible variable used or declared by [expression], if any, following the occurrence
   * query of
   * [rule T13-11](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
   */
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
      if (
          wideNode is Expression &&
              sources.any { source ->
                wideNode === source ||
                    (source.typeVariableName != null &&
                        wideNode.typeVariableName?.identity == source.typeVariableName.identity)
              }
      ) {
        (narrowNode as? Expression)
            ?.takeUnless {
              wideNode.typeVariableName is Reference && it == wideNode
            }
            ?.let(::add)
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
        .filter {
          it.copy(typeVariableName = null) != declaration.copy(typeVariableName = null) &&
              it.narrows(variable.bound.expressionFull, info)
        }
        .map { expression ->
          ((info as? GameReader)?.resolve(expression)
                  ?: variable.bound.classTable.resolve(expression))
              .groundType
        }
        .distinct()
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
            "$general and $specific belong to unrelated class tables"
          },
  ): Map<TypeVariable, GroundType> {
    val captures = mutableMapOf<TypeVariable, MutableList<GroundType>>()

    fun Expression.matchesRecordedOccurrence(that: Expression): Boolean =
        typeVariableName != null &&
            typeVariableName.identity == that.typeVariableName?.identity &&
            copy(typeVariableName = null) == that.copy(typeVariableName = null)

    fun Entry.matchesRecordedOccurrence(expression: Expression): Boolean =
        currentExpressions.values.any { it.matchesRecordedOccurrence(expression) } ||
            currentExpressions.keys.any { occurrence ->
              occurrence.expression.matchesRecordedOccurrence(expression)
            }

    fun record(expression: Expression, captured: GroundType) {
      val identical = entries.filter { entry ->
        entry.currentExpressions.values.any { it === expression }
      }
      val matching = identical.ifEmpty {
        entries.filter { it.matchesRecordedOccurrence(expression) }
      }
      matching.forEach { entry ->
        captures.getOrPut(entry.variable, ::mutableListOf) += captured
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
          ?: error("Type variable $variable has conflicting captures: ${values.distinct()}")
    }
  }

  /**
   * Returns a transformer that applies captured [bindings] only at recorded occurrences. Each
   * occurrence retains its own arguments, and a declaration refinement already checked during
   * capture is consumed, exactly as specified by
   * [rule T13-10](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#13-type-variables).
   */
  public fun bind(
      bindings: Map<TypeVariable, GroundType>,
      classTable: ClassTable =
          bindings.values.firstOrNull()?.classTable
              ?: entries.firstOrNull()?.variable?.bound?.classTable
              ?: error("an empty Type-variable scope has no class table"),
  ): PetTransformer {
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
        val constraint = classTable.resolve(source)
        val occurrenceBinding =
            classTable.glb(captured, constraint.consumeCapturedRefinement())
                ?: throw NarrowingException(
                    "$replacement does not satisfy Type-variable occurrence $source"
                )
        val target = occurrence.expressionFor(occurrenceBinding, source, classTable)
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
    return BindingTransformer(bindings.keys, replacements, classTable)
  }

  private class BindingTransformer(
      val boundVariables: Set<TypeVariable>,
      private val replacements: List<Pair<Expression, Expression>>,
      private val classTable: ClassTable,
  ) : PetTransformer() {
    override fun transformNode(node: PetNode): PetNode {
      if (node is Expression) {
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

    fun fromDeclarations(
        regions: List<PetNode>,
        classTable: ClassTable,
        namedDeclarations: List<Expression> = emptyList(),
    ): TypeVariableScope {
      data class Found(
          val expression: Expression,
          val region: Int,
          val ordinal: Int,
          val observing: Boolean,
      )

      var ordinal = 0
      val occurrences = buildList {
        fun collect(
            node: PetNode,
            region: Int,
            observing: Boolean,
        ) {
          val expression = node as? Expression
          if (expression != null) {
            add(
                Found(
                    expression,
                    region,
                    ordinal++,
                    observing,
                )
            )
          }
          val childrenObserve = observing || node.startsTypeVariableObservation
          node.immediateChildren().forEach { child ->
            collect(
                child,
                region,
                childrenObserve,
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
          namedDeclarations
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
