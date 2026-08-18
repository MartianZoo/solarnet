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
    if (!claimedBases.add(base)) {
      throw PetSyntaxException(
          "Owner $owner has more than one unnamed derived $base Class; declare them explicitly"
      )
    }

    val declaration = body.asDerivedDeclaration(generated, base)
    declarationsByBase[base] = transformDeclaration(declaration)
    return transformChildren(node.copy(className = generated))
  }

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
