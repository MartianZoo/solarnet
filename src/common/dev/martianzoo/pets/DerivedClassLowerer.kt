package dev.martianzoo.pets

import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.pets.data.ClassDeclaration.DefaultsDeclaration.OneDefault
import dev.martianzoo.pets.util.toSetStrict

/**
 * Lowers parsed owner-local Classes to ordinary, stably named Class declarations, as defined by
 * [section 11](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#11-owner-local-classes).
 * A card often needs a class of its own — one required action, one special tile, one remote area —
 * that no other card will ever mention; rather than force a name, the definition declares the class
 * where it is used ([rule
 * L11-1](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#11-owner-local-classes))
 * and the name is derived from [owner] ([rule
 * L11-2](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#11-owner-local-classes)).
 *
 * This is source-level lowering: it happens while the declaration file is parsed, so the type
 * system never sees anything but ordinary declarations. Naming a base class with no local body is
 * still just that base class ([rule
 * L11-8](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#11-owner-local-classes)).
 */
// TODO: Contract this temporary tfm-canon seam.
public class DerivedClassLowerer(private val owner: ClassName) : PetTransformer() {
  private val claimedBases = mutableSetOf<ClassName>()
  private val declarationsByBase = linkedMapOf<ClassName, ClassDeclaration>()

  /** The declarations generated so far, one per base class name claimed by [owner]. */
  public val declarations: List<ClassDeclaration>
    get() = declarationsByBase.values.toList()

  internal fun lowerDeclaration(declaration: ClassDeclaration): List<ClassDeclaration> =
      listOf(transformDeclaration(declaration)) + declarations

  override fun transformNode(node: PetNode): PetNode {
    if (node !is Expression) return transformChildren(node)
    val body = node.derivedClassBody ?: return transformChildren(node)

    // Rule L11-2: the generated name is the owner's name, an underscore, and the base class name,
    // so `SpecialTile<> {}` on MiningRights becomes `MiningRights_SpecialTile`.
    val base = node.className
    val generated = cn("${owner}_$base")
    val bodyNodes = body.asDerivedDeclaration(generated, base.expression).allNodes
    // Rule L11-5: owner-local Classes do not nest, in the body or in an argument of the occurrence.
    if (
        node.immediateChildren().any { it.containsDerivedClass() } ||
            bodyNodes.any { it.containsDerivedClass() }
    ) {
      throw PetSyntaxException("Owner-local Classes cannot contain owner-local Classes")
    }
    // Rule L11-6: one owner declares at most one unnamed local class per base name, so the derived
    // name stays stable rather than depending on source order.
    if (!claimedBases.add(base)) {
      throw PetSyntaxException(
          "Owner $owner has more than one unnamed derived $base Class; declare them explicitly"
      )
    }

    // Rule L11-3: arguments specialize both the occurrence and the generated supertype, while
    // refinements constrain only the occurrence — a refined type cannot be a supertype (L1-9).
    val loweredArguments = node.arguments.map(::transformExpression)
    val loweredRefinement = node.refinement?.let(::transformRefinement)
    val supertype =
        Expression(
            className = base,
            arguments = loweredArguments.map(::withoutRefinements),
        )
    val declaration = body.asDerivedDeclaration(generated, supertype)
    declarationsByBase[base] = transformDeclaration(declaration)
    return Expression(
        generated,
        loweredArguments,
        loweredRefinement,
        node.argumentsSpecified,
    )
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
        authoredEffects = declaration.authoredEffects.map(::transformEffect),
        authoredActions = declaration.authoredActions.map(::transformAction),
        executableEffects = declaration.executableEffects?.map(::transformEffect),
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
