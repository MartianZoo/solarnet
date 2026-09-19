package dev.martianzoo.pets.ast

import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.ast.Expression.TypeVariableName.Declaration
import dev.martianzoo.pets.ast.Expression.TypeVariableName.Reference
import dev.martianzoo.pets.ast.Expression.TypeVariableName.RepresentedClassReference
import dev.martianzoo.pets.ast.Expression.TypeVariableName.Resolution
import dev.martianzoo.pets.ast.Instruction.Each
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.ast.Instruction.Transmute
import dev.martianzoo.pets.ast.Metric.Rank
import dev.martianzoo.pets.data.ClassDeclaration

private fun containsInstance(expressions: Iterable<Expression>, candidate: Expression): Boolean =
    expressions.any {
      it === candidate
    }

private fun Expression.restoredDeclaration(): Expression {
  val marker = requireNotNull(typeVariableName)
  val bareReference = marker as? Reference
  return copy(
      className = marker.boundClassName,
      arguments = if (bareReference?.argumentsSpecified == false) emptyList() else arguments,
      refinement = if (bareReference?.argumentsSpecified == false) null else refinement,
      argumentsSpecified =
          if (bareReference?.argumentsSpecified == false) false else argumentsSpecified,
      typeVariableName = Declaration(marker.name, marker.boundClassName),
  )
}

/** Declarations whose names connect either side of this transmutation to the other. */
internal fun Transmute.localTypeVariableDeclarations(): List<Expression> {
  val destinationIdentities =
      gaining.descendantsOfType<Expression>().mapNotNull { expression ->
        expression.typeVariableName?.identity
      }
  val sourceIdentities =
      removing.descendantsOfType<Expression>().mapNotNull { expression ->
        expression.typeVariableName?.identity
      }
  return gaining.nonObservingTypeVariableDeclarations().filter {
    it.typeVariableName!!.identity in sourceIdentities
  } +
      removing.nonObservingTypeVariableDeclarations().filter {
        it.typeVariableName!!.identity in destinationIdentities
      }
}

/** Declarations belonging to this sequence, excluding declarations owned by nested sequences. */
internal fun Then.localTypeVariableDeclarations(): List<Expression> = buildList {
  data class Candidate(val region: Int, val expression: Expression, val observing: Boolean)
  val candidates = mutableListOf<Candidate>()
  fun collect(node: PetNode, region: Int, excluded: List<Expression>, observing: Boolean) {
    if (node is Then) return
    val constructDeclarations =
        when (node) {
          is Each -> node.selector.selectorTypeVariableDeclarations()
          is Rank -> node.selector.selectorTypeVariableDeclarations()
          is Expression ->
              if (
                  node.className == dev.martianzoo.pets.api.SystemClasses.CLASS &&
                      node.refinement != null
              ) {
                node.arguments
                    .singleOrNull()
                    ?.takeIf {
                      it.typeVariableName is Declaration
                    }
                    ?.let(::listOf)
                    .orEmpty()
              } else {
                emptyList()
              }
          else -> emptyList()
        }
    val nextExcluded =
        excluded +
            constructDeclarations +
            if (node is Transmute) node.localTypeVariableDeclarations() else emptySet()
    if (
        node is Expression &&
            (node.typeVariableName is Declaration || node.typeVariableName is Reference) &&
            nextExcluded.none {
              it.typeVariableName!!.identity == node.typeVariableName.identity
            }
    ) {
      candidates += Candidate(region, node, observing)
    }
    val childrenObserve = observing || node.startsTypeVariableObservation
    node.immediateChildren().forEach { collect(it, region, nextExcluded, childrenObserve) }
  }

  instructions.forEachIndexed { region, node ->
    collect(node, region, emptyList(), observing = false)
  }
  candidates
      .groupBy { it.expression.typeVariableName!!.identity }
      .values
      .filter { occurrences -> occurrences.map { it.region }.distinct().size >= 2 }
      .mapNotNullTo(this) { occurrences ->
        occurrences
            .firstOrNull {
              !it.observing && it.expression.typeVariableName is Declaration
            }
            ?.expression
      }
}

/** Declarations owned by selectors or refined class literals rather than an enclosing scope. */
internal fun PetNode.constructLocalTypeVariableDeclarations(): List<Expression> = buildList {
  visitDescendants { node ->
    when (node) {
      is Each -> addAll(node.selector.selectorTypeVariableDeclarations())
      is Rank -> addAll(node.selector.selectorTypeVariableDeclarations())
      is Expression ->
          if (
              node.className == dev.martianzoo.pets.api.SystemClasses.CLASS &&
                  node.refinement != null
          ) {
            node.arguments.singleOrNull()?.takeIf { it.typeVariableName is Declaration }?.let(::add)
          }
      else -> Unit
    }
    true
  }
}

/** Marked expressions outside observing requirements, metrics, and refinements. */
private fun PetNode.nonObservingTypeVariableDeclarations(): List<Expression> = buildList {
  fun collect(node: PetNode, observing: Boolean) {
    if (!observing && node is Expression && node.typeVariableName is Declaration) add(node)
    val childrenObserve = observing || node.startsTypeVariableObservation
    node.immediateChildren().forEach { collect(it, childrenObserve) }
  }
  collect(this@nonObservingTypeVariableDeclarations, observing = false)
}

/** Resolves references to [declarations] while retaining their authored name and argument list. */
internal fun <P : PetNode> resolveTypeVariableNames(
    root: P,
    declarations: List<Expression>,
): P {
  val resolved = resolveTypeVariableNames(listOf(root), declarations).single()
  @Suppress("UNCHECKED_CAST")
  return resolved as P
}

/**
 * Resolves one lexical scope. Local settlement variables expand a reference to their declared
 * structure; [expandReferences] is false when the value instead comes from a Class header or a
 * selector and binding supplies the selected structure later.
 */
internal fun resolveTypeVariableNames(
    roots: List<PetNode>,
    declarations: List<Expression>,
    expandReferences: Boolean = true,
): List<PetNode> {
  if (declarations.isEmpty()) return roots
  val selectedDeclarations = declarations.distinctBy { it.typeVariableName!!.key }
  val selectedByKey = selectedDeclarations.associateBy { it.typeVariableName!!.key }
  val resolutions = selectedByKey.mapValues { (_, declaration) ->
    declaration.typeVariableName!!.resolution ?: Resolution()
  }
  val markerNormalizer =
      object : PetTransformer() {
        override fun transformNode(node: PetNode): PetNode {
          if (node is Expression && node.typeVariableName is Declaration) {
            val marker = node.typeVariableName
            val selected = selectedByKey[marker.key]
            if (marker.resolved && node !== selected) return transformChildren(node)
            if (node === selected && !marker.resolved) {
              return transformChildren(
                  node.copy(typeVariableName = marker.resolved(resolutions.getValue(marker.key)))
              )
            }
            if (selected != null && node !== selected) {
              return transformChildren(
                  node.copy(
                      typeVariableName =
                          Reference(
                              marker.name,
                              marker.boundClassName,
                              argumentsSpecified = node.argumentsSpecified,
                          )
                  )
              )
            }
          }
          return transformChildren(node)
        }
      }
  val normalizedRoots = roots.map(markerNormalizer::transformWithoutKindCheck)
  val declarationsByKey =
      normalizedRoots
          .flatMap { it.descendantsOfType<Expression>() }
          .filter { it.typeVariableName is Declaration && it.typeVariableName.key in selectedByKey }
          .associateBy { it.typeVariableName!!.key }

  // `Class<Foo^1>` names the represented Class, so `Foo^1<Bar>` can instantiate the selected
  // Class with dependency constraints. No other kind of Type-variable declaration is applicable.
  val representedClassDeclarations =
      normalizedRoots
          .flatMap { it.descendantsOfType<Expression>() }
          .filter { it.className == dev.martianzoo.pets.api.SystemClasses.CLASS }
          .mapNotNull { expression ->
            expression.arguments.singleOrNull()?.takeIf {
              it.typeVariableName is Declaration
            }
          }

  val references = mutableMapOf<Pair<ClassName, String>, Int>()
  normalizedRoots
      .flatMap { it.descendantsOfType<Expression>() }
      .mapNotNull { (it.typeVariableName as? Reference)?.takeUnless(Reference::resolved)?.key }
      .filter { it in declarationsByKey }
      .forEach { key -> references[key] = references.getOrElse(key) { 0 } + 1 }
  val resolving = mutableSetOf<Pair<ClassName, String>>()
  fun withoutNames(expression: Expression): Expression =
      expression.copy(
          arguments = expression.arguments.map(::withoutNames),
          typeVariableName = null,
      )
  val resolver =
      object : PetTransformer() {
        override fun transformNode(node: PetNode): PetNode {
          if (node is Expression) {
            val reference = (node.typeVariableName as? Reference)?.takeUnless(Reference::resolved)
            reference?.let {
              declarationsByKey[reference.key]?.let { declaration ->
                val representedApplication =
                    representedClassDeclarations.any { it === declaration } &&
                        declaration.simple &&
                        node.refinement == null
                val repeatedStructure =
                    node.arguments == declaration.arguments &&
                        node.argumentsSpecified == declaration.argumentsSpecified &&
                        node.refinement == declaration.refinement
                if (!node.simple && !representedApplication && !repeatedStructure) {
                  throw PetSyntaxException(
                      "Type-variable reference $node cannot have arguments or a refinement"
                  )
                }
                references[reference.key] = references.getOrElse(reference.key) { 0 } + 1
                if (!resolving.add(reference.key)) {
                  throw PetSyntaxException(
                      "Type-variable declarations cannot refer to each other cyclically"
                  )
                }
                return try {
                  val structuralDeclaration = withoutNames(declaration)
                  val referenced =
                      structuralDeclaration.copy(
                          arguments =
                              if (node.simple && expandReferences) {
                                structuralDeclaration.arguments
                              } else {
                                node.arguments
                              },
                          argumentsSpecified =
                              if (node.simple && expandReferences) {
                                structuralDeclaration.argumentsSpecified
                              } else {
                                node.argumentsSpecified
                              },
                      )
                  transformChildren(
                      referenced.copy(
                          typeVariableName = reference.resolved(resolutions.getValue(reference.key))
                      )
                  )
                } finally {
                  resolving.remove(reference.key)
                }
              }
            }
          }
          return transformChildren(node)
        }
      }
  val resolved = normalizedRoots.map(resolver::transformWithoutKindCheck)
  declarationsByKey.keys
      .firstOrNull { references[it] == null }
      ?.let { (boundClass, handle) ->
        throw PetSyntaxException("Type-variable marker $boundClass^$handle is not shared")
      }
  return resolved
}

/** Names declared by a selector itself or by the class represented by a `Class<T>` selector. */
internal fun Expression.selectorTypeVariableDeclarations(): List<Expression> =
    listOfNotNull(
        takeIf { it.typeVariableName is Declaration },
        arguments.singleOrNull()?.takeIf {
          className == dev.martianzoo.pets.api.SystemClasses.CLASS &&
              it.typeVariableName is Declaration
        },
    )

/** Resolves selector markers in the selector and the nodes evaluated for each selection. */
internal fun resolveSelectorTypeVariableNames(
    selector: Expression,
    scopedNodes: List<PetNode>,
): List<PetNode> {
  val declarations = selector.selectorTypeVariableDeclarations()
  if (declarations.isEmpty()) return listOf(selector) + scopedNodes
  return resolveTypeVariableNames(
      listOf(selector) + scopedNodes,
      declarations,
      expandReferences = false,
  )
}

/** Resolves an explicit represented-class name inside one refined class literal. */
internal fun resolveClassLiteralTypeVariableNames(expression: Expression): Expression {
  if (
      expression.className != dev.martianzoo.pets.api.SystemClasses.CLASS ||
          expression.refinement == null
  ) {
    return expression
  }
  val declaration =
      expression.arguments.singleOrNull()?.takeIf { it.typeVariableName is Declaration }
          ?: return expression
  val resolved =
      resolveTypeVariableNames(
              listOf(expression),
              listOf(declaration),
              expandReferences = false,
          )
          .single()
  return resolved as Expression
}

/** Erases a refined class literal's lexical alias while retaining its candidate references. */
internal fun Expression.expandClassLiteralTypeVariableName(): Expression {
  if (className != dev.martianzoo.pets.api.SystemClasses.CLASS || refinement == null) return this
  val represented = arguments.singleOrNull() ?: return this
  val declaration = represented.takeIf { it.typeVariableName is Declaration }
  val identity = declaration?.typeVariableName?.identity
  val representedClass = represented.className
  val expander =
      object : PetTransformer() {
        override fun transformNode(node: PetNode): PetNode {
          if (
              node is Expression &&
                  ((identity != null &&
                      (node.typeVariableName as? Reference)?.identity == identity) ||
                      (identity == null &&
                          node.typeVariableName == null &&
                          node.className == representedClass))
          ) {
            return transformChildren(
                node.copy(typeVariableName = RepresentedClassReference(representedClass))
            )
          }
          return transformChildren(node)
        }
      }
  return copy(
      arguments = arguments.map { it.copy(typeVariableName = null) },
      refinement = expander.transformRefinement(refinement),
  )
}

/** Binds only explicitly named selector references to one selected concrete expression. */
internal fun selectorReferenceBinder(
    selector: Expression,
    selected: Expression,
): PetTransformer {
  data class Binding(val expression: Expression, val acceptsDependencyArguments: Boolean)

  val bindings = buildMap {
    (selector.typeVariableName as? Declaration)?.let {
      put(it.identity, Binding(selected, acceptsDependencyArguments = false))
    }
    val representedDeclaration =
        selector.arguments.singleOrNull()?.takeIf {
          selector.className == dev.martianzoo.pets.api.SystemClasses.CLASS &&
              it.typeVariableName is Declaration
        }
    representedDeclaration?.let {
      require(selected.className == dev.martianzoo.pets.api.SystemClasses.CLASS)
      put(
          it.typeVariableName!!.identity,
          Binding(selected.arguments.single(), acceptsDependencyArguments = it.simple),
      )
    }
  }
  if (bindings.isEmpty()) return PetTransformer.noOp()
  return object : PetTransformer() {
    override fun transformNode(node: PetNode): PetNode {
      if (node is Expression) {
        val identity = (node.typeVariableName as? Reference)?.identity
        bindings[identity]?.let { binding ->
          val arguments =
              if (binding.acceptsDependencyArguments && node.argumentsSpecified) node.arguments
              else emptyList()
          return transformChildren(
              binding.expression.copy(
                  arguments = binding.expression.arguments + arguments,
                  argumentsSpecified =
                      binding.expression.argumentsSpecified ||
                          (binding.acceptsDependencyArguments && node.argumentsSpecified),
              )
          )
        }
      }
      return transformChildren(node)
    }
  }
}

/**
 * Resolves names declared in a Class header throughout that header and the Class's authored body.
 */
internal fun resolveClassTypeVariableNames(declaration: ClassDeclaration): ClassDeclaration {
  val header = declaration.dependencies + declaration.supertypes
  val declarations =
      header
          .flatMap { it.descendantsOfType<Expression>() }
          .filter { it.typeVariableName is Declaration }
          .distinctBy { it.typeVariableName!!.key }
  if (declarations.isEmpty()) return declaration

  val headerKeys = declarations.mapTo(mutableSetOf()) { it.typeVariableName!!.key }
  val constructLocalIdentities =
      (declaration.authoredEffects + declaration.authoredActions)
          .flatMap(PetNode::constructLocalTypeVariableDeclarations)
          .mapTo(mutableSetOf()) { it.typeVariableName!!.identity }
  val headerMarkerRestorer =
      object : PetTransformer() {
        override fun transformNode(node: PetNode): PetNode {
          if (
              node is Expression &&
                  node.typeVariableName?.key in headerKeys &&
                  node.typeVariableName?.identity !in constructLocalIdentities
          ) {
            return transformChildren(node.restoredDeclaration())
          }
          return transformChildren(node)
        }
      }
  val body =
      (declaration.authoredEffects + declaration.authoredActions).map(
          headerMarkerRestorer::transformWithoutKindCheck
      )
  val resolved =
      resolveTypeVariableNames(
          header + body,
          declarations,
          expandReferences = false,
      )
  val dependencyCount = declaration.dependencies.size
  val headerCount = header.size
  val effectCount = declaration.authoredEffects.size
  return declaration.copy(
      dependencies = resolved.take(dependencyCount).map { it as Expression },
      supertypes =
          resolved
              .drop(dependencyCount)
              .take(headerCount - dependencyCount)
              .map {
                it as Expression
              }
              .toSet(),
      authoredEffects = resolved.drop(headerCount).take(effectCount).map { it as Effect },
      authoredActions = resolved.drop(headerCount + effectCount).map { it as Action },
  )
}

/** Resolves a scope whose declarations belong in [declarationRegion] and uses in [usageRegion]. */
internal fun <P : PetNode> resolveTypeVariableNames(
    root: P,
    declarationRegion: PetNode?,
    usageRegion: PetNode,
): P {
  val declarationConstructLocals =
      declarationRegion?.constructLocalTypeVariableDeclarations().orEmpty()
  val declarations =
      declarationRegion
          ?.nonObservingTypeVariableDeclarations()
          ?.filter {
            !containsInstance(declarationConstructLocals, it)
          }
          .orEmpty()
  val constructLocalIdentities =
      root.constructLocalTypeVariableDeclarations().mapTo(mutableSetOf()) {
        it.typeVariableName!!.identity
      }
  fun Expression.markerKeyOutsideConstruct(): Pair<ClassName, String>? =
      typeVariableName?.takeIf { it.identity !in constructLocalIdentities }?.key
  val usageKeys =
      usageRegion
          .descendantsOfType<Expression>()
          .mapNotNull(Expression::markerKeyOutsideConstruct)
          .toSet()
  val actorKeys =
      declarationRegion
          ?.descendantsOfType<Effect.Trigger.ByTrigger>()
          ?.mapNotNull { it.by.markerKeyOutsideConstruct() }
          ?.toSet()
          .orEmpty()
  val declarationKeyCounts =
      declarationRegion
          ?.descendantsOfType<Expression>()
          ?.mapNotNull(Expression::markerKeyOutsideConstruct)
          ?.groupingBy { it }
          ?.eachCount()
          .orEmpty()
  val selectedKeys =
      declarations
          .mapNotNull { declaration ->
            declaration.typeVariableName!!.key.takeIf { key ->
              key in usageKeys || (key in actorKeys && declarationKeyCounts.getValue(key) >= 2)
            }
          }
          .toSet()
  if (selectedKeys.isEmpty()) return root
  val outerMarkerRestorer =
      object : PetTransformer() {
        override fun transformNode(node: PetNode): PetNode {
          if (
              node is Expression &&
                  node.typeVariableName?.key in selectedKeys &&
                  node.typeVariableName?.identity !in constructLocalIdentities
          ) {
            return transformChildren(node.restoredDeclaration())
          }
          return transformChildren(node)
        }
      }
  val restoredRoot = outerMarkerRestorer.transformWithoutKindCheck(root)
  @Suppress("UNCHECKED_CAST") val typedRoot = restoredRoot as P
  val restoredDeclarationRegion =
      when (restoredRoot) {
        is Effect -> restoredRoot.trigger
        is Action -> restoredRoot.cost
        else -> error("Unexpected local Type-variable scope: $restoredRoot")
      }
  val restoredDeclarations =
      restoredDeclarationRegion
          ?.nonObservingTypeVariableDeclarations()
          ?.filter { it.typeVariableName!!.key in selectedKeys }
          .orEmpty()
  return resolveTypeVariableNames(
      typedRoot,
      restoredDeclarations,
  )
}
