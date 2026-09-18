package dev.martianzoo.pets.ast

import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.ast.Expression.TypeVariableName.Declaration
import dev.martianzoo.pets.ast.Expression.TypeVariableName.Reference
import dev.martianzoo.pets.ast.Expression.TypeVariableName.RepresentedClassReference
import dev.martianzoo.pets.ast.Instruction.Each
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.ast.Instruction.Transmute
import dev.martianzoo.pets.ast.Metric.Rank
import dev.martianzoo.pets.data.ClassDeclaration

/** Declarations whose names connect this transmutation's destination to its source. */
internal fun Transmute.localTypeVariableDeclarations(): List<Expression> {
  val sourceNames =
      removing.descendantsOfType<Expression>().mapNotNull { expression ->
        (expression.typeVariableName as? Reference)?.name
            ?: expression.className.takeIf { expression.simple }
      }
  return gaining.descendantsOfType<Expression>().filter {
    it.typeVariableName is Declaration && it.typeVariableName.name in sourceNames
  }
}

/** Declarations belonging to this sequence, excluding declarations owned by nested sequences. */
internal fun Then.localTypeVariableDeclarations(): List<Expression> = buildList {
  val selectorDeclarations = constructLocalTypeVariableDeclarations()
  fun collect(node: PetNode, excluded: Set<Expression>) {
    if (node is Then) return
    val nextExcluded =
        excluded +
            selectorDeclarations +
            if (node is Transmute) node.localTypeVariableDeclarations() else emptySet()
    if (node is Expression && node.typeVariableName is Declaration && node !in nextExcluded) {
      add(node)
    }
    node.immediateChildren().forEach { collect(it, nextExcluded) }
  }

  immediateChildren().forEach { collect(it, emptySet()) }
}

/** Declarations owned by selectors or refined class literals rather than an enclosing scope. */
internal fun PetNode.constructLocalTypeVariableDeclarations(): Set<Expression> = buildSet {
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

/** The first named declaration beneath an observing requirement, metric, or refinement. */
internal fun PetNode.observingTypeVariableDeclaration(): Expression? {
  val localDeclarations = constructLocalTypeVariableDeclarations()
  fun find(node: PetNode, observing: Boolean): Expression? {
    if (
        observing &&
            node is Expression &&
            node.typeVariableName is Declaration &&
            node !in localDeclarations
    ) {
      return node
    }
    val childrenObserve = observing || node.startsTypeVariableObservation
    return node.immediateChildren().firstNotNullOfOrNull { find(it, childrenObserve) }
  }

  return find(this, observing = false)
}

/**
 * Resolves bare references to [declarations] while retaining their shorter authored spelling. Type
 * interpretation later validates each declaration and records its scoped identity.
 */
internal fun <P : PetNode> resolveTypeVariableNames(
    root: P,
    declarations: List<Expression>,
    scopeDescription: String,
): P {
  val resolved =
      resolveTypeVariableNames(listOf(root), declarations, scopeDescription, false).single()
  @Suppress("UNCHECKED_CAST")
  return resolved as P
}

internal fun resolveTypeVariableNames(
    roots: List<PetNode>,
    declarations: List<Expression>,
    scopeDescription: String,
    allowSpecializedReferences: Boolean,
): List<PetNode> {
  val declarationsByName = declarations.associateBy { it.typeVariableName!!.name }
  if (declarationsByName.size != declarations.size) {
    throw PetSyntaxException("$scopeDescription cannot declare the same Type-variable name twice")
  }
  if (declarations.isEmpty()) return roots

  val references = mutableMapOf<ClassName, Int>()
  roots
      .flatMap { it.descendantsOfType<Expression>() }
      .mapNotNull { (it.typeVariableName as? Reference)?.name }
      .filter { it in declarationsByName }
      .forEach { name -> references[name] = references.getOrElse(name) { 0 } + 1 }
  val resolving = mutableSetOf<ClassName>()
  fun withoutNames(expression: Expression): Expression =
      expression.copy(
          arguments = expression.arguments.map(::withoutNames),
          typeVariableName = null,
      )
  val resolver =
      object : PetTransformer() {
        override fun transformNode(node: PetNode): PetNode {
          if (node is Expression && node.typeVariableName == null) {
            declarationsByName[node.className]?.let { declaration ->
              if (
                  !node.simple &&
                      (!allowSpecializedReferences ||
                          !declaration.simple ||
                          node.refinement != null)
              ) {
                throw PetSyntaxException(
                    "Type-variable reference ${node.className} cannot have arguments or a refinement"
                )
              }
              references[node.className] = references.getOrElse(node.className) { 0 } + 1
              if (!resolving.add(node.className)) {
                throw PetSyntaxException(
                    "Type-variable declarations cannot refer to each other cyclically"
                )
              }
              return try {
                val structuralDeclaration = withoutNames(declaration)
                val referenced =
                    structuralDeclaration.copy(
                        arguments =
                            if (node.simple && !allowSpecializedReferences) {
                              structuralDeclaration.arguments
                            } else {
                              node.arguments
                            },
                        argumentsSpecified =
                            if (node.simple && !allowSpecializedReferences) {
                              structuralDeclaration.argumentsSpecified
                            } else {
                              node.argumentsSpecified
                            },
                    )
                transformChildren(referenced.copy(typeVariableName = Reference(node.className)))
              } finally {
                resolving.remove(node.className)
              }
            }
          }
          return transformChildren(node)
        }
      }
  val resolved = roots.map(resolver::transformWithoutKindCheck)
  declarationsByName.keys
      .firstOrNull { references[it] == null }
      ?.let { throw PetSyntaxException("Type-variable $it is declared but never used") }
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

/** Resolves explicit selector names in the selector and the nodes evaluated for each selection. */
internal fun resolveSelectorTypeVariableNames(
    selector: Expression,
    scopedNodes: List<PetNode>,
    scopeDescription: String,
): List<PetNode> {
  val declarations = selector.selectorTypeVariableDeclarations()
  if (declarations.isEmpty()) return listOf(selector) + scopedNodes
  return resolveTypeVariableNames(
      listOf(selector) + scopedNodes,
      declarations,
      scopeDescription,
      allowSpecializedReferences = true,
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
              "A refined Class literal",
              allowSpecializedReferences = true,
          )
          .single()
  return resolved as Expression
}

/** Erases a refined class literal's lexical alias while retaining its candidate references. */
internal fun Expression.expandClassLiteralTypeVariableName(): Expression {
  if (className != dev.martianzoo.pets.api.SystemClasses.CLASS || refinement == null) return this
  val declaration =
      arguments.singleOrNull()?.takeIf { it.typeVariableName is Declaration } ?: return this
  val name = declaration.typeVariableName!!.name
  val representedClass = declaration.className
  val expander =
      object : PetTransformer() {
        override fun transformNode(node: PetNode): PetNode {
          if (node is Expression && (node.typeVariableName as? Reference)?.name == name) {
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
  data class Binding(val expression: Expression, val acceptsArguments: Boolean)

  val bindings = buildMap {
    (selector.typeVariableName as? Declaration)?.let {
      put(it.name, Binding(selected, selector.simple))
    }
    val representedDeclaration =
        selector.arguments.singleOrNull()?.takeIf {
          selector.className == dev.martianzoo.pets.api.SystemClasses.CLASS &&
              it.typeVariableName is Declaration
        }
    representedDeclaration?.let {
      require(selected.className == dev.martianzoo.pets.api.SystemClasses.CLASS)
      put(
          it.typeVariableName!!.name,
          Binding(selected.arguments.single(), it.simple),
      )
    }
  }
  if (bindings.isEmpty()) return PetTransformer.noOp()
  return object : PetTransformer() {
    override fun transformNode(node: PetNode): PetNode {
      if (node is Expression) {
        val name = (node.typeVariableName as? Reference)?.name
        bindings[name]?.let { binding ->
          val arguments =
              if (binding.acceptsArguments && node.argumentsSpecified) node.arguments
              else emptyList()
          return transformChildren(
              binding.expression.copy(
                  arguments = binding.expression.arguments + arguments,
                  argumentsSpecified =
                      binding.expression.argumentsSpecified ||
                          (binding.acceptsArguments && node.argumentsSpecified),
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
  if (declarations.isEmpty()) return declaration

  val names = declarations.mapTo(mutableSetOf()) { it.typeVariableName!!.name }
  val body = declaration.authoredEffects + declaration.authoredActions
  body
      .flatMap { it.descendantsOfType<Expression>() }
      .firstOrNull {
        it.typeVariableName is Declaration && it.typeVariableName.name in names
      }
      ?.let {
        throw PetSyntaxException(
            "Type-variable ${it.typeVariableName!!.name} cannot shadow a Class-header declaration"
        )
      }

  val resolved =
      resolveTypeVariableNames(
          header + body,
          declarations,
          "A Class header",
          allowSpecializedReferences = true,
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
    scopeDescription: String,
    declarationLocation: String,
): P {
  declarationRegion?.observingTypeVariableDeclaration()?.let {
    throw PetSyntaxException(
        "A Type-variable name cannot be declared in an observing expression: $it"
    )
  }
  val declarations =
      declarationRegion
          ?.descendantsOfType<Expression>()
          ?.filter {
            it.typeVariableName is Declaration &&
                it !in declarationRegion.constructLocalTypeVariableDeclarations()
          }
          .orEmpty()
  val declarationNames = declarations.mapTo(mutableSetOf()) { it.typeVariableName!!.name }
  val constructLocalDeclarations = usageRegion.constructLocalTypeVariableDeclarations()
  usageRegion
      .descendantsOfType<Expression>()
      .filter {
        it.typeVariableName is Declaration && it !in constructLocalDeclarations
      }
      .firstOrNull { it.typeVariableName!!.name in declarationNames }
      ?.let {
        throw PetSyntaxException(
            "Type-variable ${it.typeVariableName!!.name} cannot shadow an enclosing declaration"
        )
      }

  fun declarationOutsideThen(node: PetNode, excluded: Set<Expression>): Expression? {
    if (node is Then) return null
    val nextExcluded =
        if (node is Transmute) excluded + node.localTypeVariableDeclarations() else excluded
    if (
        node is Expression &&
            node.typeVariableName is Declaration &&
            node !in nextExcluded &&
            node !in constructLocalDeclarations
    ) {
      return node
    }
    return node.immediateChildren().firstNotNullOfOrNull {
      declarationOutsideThen(it, nextExcluded)
    }
  }
  declarationOutsideThen(usageRegion, emptySet())?.let {
    throw PetSyntaxException("A Type-variable name must be declared in $declarationLocation: $it")
  }
  return resolveTypeVariableNames(root, declarations, scopeDescription)
}
