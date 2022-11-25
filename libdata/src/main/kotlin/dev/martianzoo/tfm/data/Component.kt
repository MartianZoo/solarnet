package dev.martianzoo.tfm.data

import com.squareup.moshi.Json
import dev.martianzoo.tfm.petaform.api.Action
import dev.martianzoo.tfm.petaform.api.Effect
import dev.martianzoo.tfm.petaform.api.Expression
import dev.martianzoo.tfm.petaform.api.Instruction
import dev.martianzoo.tfm.petaform.parser.PetaformParser
import dev.martianzoo.tfm.petaform.parser.PetaformParser.parse

/**
 * The declaration of a component class, such as GreeneryTile. Models the declaration textually as
 * it was provided.
 */
data class Component(
    /** Unique name for this component class. */
    val name: String,

    /** If `true`, types are always abstract, even when all dependencies are concrete. Not inherited. */
    val abstract: Boolean = false,

    /**
     * Zero or more direct supertypes, including specializations, as Petaform Expressions. Don't
     * include `Component` or any types that are already indirect supertypes (unless specializing them).
     */
    @Json(name = "supertypes")
    val supertypesPetaform: Set<String> = setOf(),

    /**
     * Zero or more direct dependencies; a dependency is a Petaform Expression with the requirement
     * that every instance of this class must relate to *exactly one* instance of the dependency
     * type. This relationship may be many-to-one. Supertype dependencies are inherited so should
     * never be restated here.
     */
    @Json(name = "dependencies")
    val dependenciesPetaform: List<String> = listOf(),

    @Json(name = "immediate")
    val immediatePetaform: String? = null,

    @Json(name = "actions")
    val actionsPetaform: Set<String> = setOf(),

    /**
     * Zero or more unordered effects that belong to each *instance* of this component class,
     * expressed in Petaform. If the exact name of a dependency type is used in an effect, and a
     * subtype of this type specializes that dependency, then its inherited copy of this effect will
     * have that type specialized in the same way. These petaform expressions can (and should) make
     * use of `This` and `Me` and can rely on type defaults.
     */
    @Json(name = "effects")
    val effectsPetaform: Set<String> = setOf(),
) : TfmObject {

  init {
    require(name !in RESERVED_NAMES)
    require(name.matches(NAME_PATTERN))
  }

  override val asComponent = this
}

private val NAME_PATTERN = Regex("^[A-Z][a-z][A-Za-z0-9_]*$") // TODO: it's repeated 3 times
private val RESERVED_NAMES = setOf("This", "It", "Always")
