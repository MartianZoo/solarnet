package dev.martianzoo.tfm.pets

import dev.martianzoo.tfm.pets.ComponentDef.OneDefault
import dev.martianzoo.tfm.pets.SpecialComponent.COMPONENT
import dev.martianzoo.tfm.pets.SpecialComponent.THIS
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.TypeExpression

/**
 * The declaration of a component class, such as GreeneryTile. Models the declaration textually as
 * it was provided. DIRECT INFO ONLY; stuff is inherited among *loaded* classes (PetClasses).
 */
data class ComponentDef(
    val name: String, // TODO rename to className
    val abstract: Boolean = false,
    val supertypes: Set<TypeExpression> = setOf(),
    val dependencies: List<Dependency> = listOf(),
    val effectsRaw: () -> Set<Effect> = { setOf() },
    val rawDefaults: RawDefaults = RawDefaults()
) {
  init {
    if (name == "$COMPONENT") {
      require(supertypes.isEmpty())
      require(dependencies.isEmpty())
    } else {
      // require(supertypes.isNotEmpty()) // TODO
    }
  }

  // Canonicalize -- *currently* only spelling out QEs
  val effects by lazy { effectsRaw() }

  val superclassNames = supertypes.map { it.className }

  data class Dependency(val type: TypeExpression, val classDep: Boolean = false)

  data class RawDefaults(
      val allDefault: OneDefault? = null,
      val gainDefault: OneDefault? = null,
      val gainIntensity: Intensity? = null)
  data class OneDefault(val specializations: List<TypeExpression>, val requirement: Requirement?)
}

fun oneDefault(te: TypeExpression) =
    OneDefault(te.specializations, te.requirement)
        .also { require(te.className == "$THIS") }
