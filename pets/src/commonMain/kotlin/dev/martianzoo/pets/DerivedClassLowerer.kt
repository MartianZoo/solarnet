package dev.martianzoo.pets

import dev.martianzoo.api.Exceptions.PetSyntaxException
import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.data.ClassDeclaration.DefaultsDeclaration.OneDefault
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.util.toSetStrict

/** Lowers parsed owner-local Classes to ordinary, stably named Class declarations. */
internal class DerivedClassLowerer(private val owner: ClassName) : PetTransformer() {
  private val claimedBases = mutableSetOf<ClassName>()
  private val declarationsByBase = linkedMapOf<ClassName, ClassDeclaration>()

  val declarations: List<ClassDeclaration>
    get() = declarationsByBase.values.toList()

  override fun transformNode(node: PetNode): PetNode {
    if (node !is Expression) return transformChildren(node)
    val body = node.derivedClassBody ?: return transformChildren(node)

    val base = node.className
    val generated = cn("${owner}_$base")
    val bodyNodes = body.asDerivedDeclaration(generated, base.expression).allNodes
    if (
        node.immediateChildren().any { it.containsDerivedClass() } ||
            bodyNodes.any { it.containsDerivedClass() }
    ) {
      throw PetSyntaxException("Owner-local Classes cannot contain owner-local Classes")
    }
    if (!claimedBases.add(base)) {
      throw PetSyntaxException(
          "Owner $owner has more than one unnamed derived $base Class; declare them explicitly"
      )
    }

    val loweredArguments = node.arguments.map(::transformExpression)
    val loweredRefinement = node.refinement?.let(::transformRefinement)
    val supertype =
        Expression(
            className = base,
            arguments = loweredArguments.map(::withoutRefinements),
            complement = node.complement,
        )
    val declaration = body.asDerivedDeclaration(generated, supertype)
    declarationsByBase[base] = transformDeclaration(declaration)
    return Expression(generated, loweredArguments, loweredRefinement, node.complement)
  }

  private fun withoutRefinements(expression: Expression): Expression =
      expression.copy(
          arguments = expression.arguments.map(::withoutRefinements),
          refinement = null,
      )

  private fun PetNode.containsDerivedClass(): Boolean =
      descendantsOfType<Expression>().any { it.derivedClassBody != null }

  private fun transformDeclaration(declaration: ClassDeclaration): ClassDeclaration {
    fun transformDefault(one: OneDefault) = one.copy(specs = one.specs.map(::transformExpression))

    val defaults = declaration.defaultsDeclaration
    return declaration.copy(
        dependencies = declaration.dependencies.map(::transformExpression),
        supertypes = declaration.supertypes.map(::transformExpression).toSetStrict(),
        invariants = declaration.invariants.map(::transformRequirement).toSetStrict(),
        effects = declaration.effects.map(::transformEffect),
        defaultsDeclaration =
            defaults.copy(
                universal = transformDefault(defaults.universal),
                gainOnly = transformDefault(defaults.gainOnly),
                removeOnly = transformDefault(defaults.removeOnly),
            ),
        properties =
            declaration.properties.entries.associate {
              transformPropertyName(it.key) to transformPropertyValue(it.value)
            },
        extraNodes = declaration.extraNodes.map(::transformWithoutKindCheck).toSetStrict(),
    )
  }
}
