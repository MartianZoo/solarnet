package dev.martianzoo.pets.ast

import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.ast.Expression.TypeVariableName.Declaration
import dev.martianzoo.pets.ast.Expression.TypeVariableName.Reference
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.ast.Instruction.Transmute

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
  fun collect(node: PetNode, excluded: Set<Expression>) {
    if (node is Then) return
    val nextExcluded =
        if (node is Transmute) excluded + node.localTypeVariableDeclarations() else excluded
    if (node is Expression && node.typeVariableName is Declaration && node !in nextExcluded) {
      add(node)
    }
    node.immediateChildren().forEach { collect(it, nextExcluded) }
  }

  immediateChildren().forEach { collect(it, emptySet()) }
}

/** The first named declaration beneath an observing requirement, metric, or refinement. */
internal fun PetNode.observingTypeVariableDeclaration(): Expression? {
  fun find(node: PetNode, observing: Boolean): Expression? {
    if (observing && node is Expression && node.typeVariableName is Declaration) return node
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
  val declarationsByName = declarations.associateBy { it.typeVariableName!!.name }
  if (declarationsByName.size != declarations.size) {
    throw PetSyntaxException("$scopeDescription cannot declare the same Type-variable name twice")
  }
  if (declarations.isEmpty()) return root

  val references = mutableMapOf<ClassName, Int>()
  val resolving = mutableSetOf<ClassName>()
  val resolver =
      object : PetTransformer() {
        override fun transformNode(node: PetNode): PetNode {
          if (node is Expression && node.typeVariableName == null) {
            declarationsByName[node.className]?.let { declaration ->
              if (!node.simple) {
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
                transformChildren(declaration.copy(typeVariableName = Reference(node.className)))
              } finally {
                resolving.remove(node.className)
              }
            }
          }
          return transformChildren(node)
        }
      }
  @Suppress("UNCHECKED_CAST") val resolved = resolver.transformWithoutKindCheck(root) as P
  declarationsByName.keys
      .firstOrNull { references[it] == null }
      ?.let { throw PetSyntaxException("Type-variable $it is declared but never used") }
  return resolved
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
          ?.filter { it.typeVariableName is Declaration }
          .orEmpty()
  val declarationNames = declarations.mapTo(mutableSetOf()) { it.typeVariableName!!.name }
  usageRegion
      .descendantsOfType<Expression>()
      .filter { it.typeVariableName is Declaration }
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
    if (node is Expression && node.typeVariableName is Declaration && node !in nextExcluded) {
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
