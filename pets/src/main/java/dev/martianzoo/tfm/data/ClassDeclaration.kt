package dev.martianzoo.tfm.data

import dev.martianzoo.api.SpecialClassNames.CUSTOM
import dev.martianzoo.tfm.data.ClassDeclaration.ClassKind.ABSTRACT
import dev.martianzoo.tfm.data.ClassDeclaration.DefaultsDeclaration.DefaultKind.ALL_USAGES
import dev.martianzoo.tfm.data.ClassDeclaration.DefaultsDeclaration.DefaultKind.GAIN_ONLY
import dev.martianzoo.tfm.data.ClassDeclaration.DefaultsDeclaration.DefaultKind.REMOVE_ONLY
import dev.martianzoo.tfm.pets.HasClassName
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.Requirement

/**
 * A direct representation of the *declaration* of a component class, such as GreeneryTile. Some of
 * these are written explicitly in `*.pets` source files, but others are converted programmatically
 * from [Definition] objects.
 *
 * The information provided here is not very "cooked"; that cooking happens in
 * `dev.martianzoo.tfm.types`.
 */
public data class ClassDeclaration
internal constructor(
    /**
     * The primary name for the class, as an upper camel case word. Class declarations having the
     * same [className] or [shortName] can't be used together in the same game.
     */
    override val className: ClassName,

    /**
     * A short name for the class (1-4 characters, all upper case letters or digits). All
     * [className]s and [shortNames] loaded for a game share the same single namespace.
     */
    public val shortName: ClassName = className,

    /** Is this class declared to be `ABSTRACT`, `CUSTOM`, or regular? */
    public val kind: ClassKind,

    /** Any "new" dependencies being declared by this class (not inherited from a supertype). */
    public val dependencies: List<Expression> = listOf(),

    /** This class's listed direct supertypes, as they were expressed in the source. */
    public val supertypes: Set<Expression> = setOf(),

    /** Any class invariants declared with `HAS` in the class body. */
    public val invariants: Set<Requirement> = setOf(),

    /** The class's effects. */
    public val effects: Set<Effect> = setOf(),

    /** The merged contents of any `DEFAULT` clauses in the class body. */
    public val defaultsDeclaration: DefaultsDeclaration = DefaultsDeclaration(),

    /**
     * Any additional Pets elements belonging to this class that aren't given for the previous
     * arguments.
     */
    internal val extraNodes: Set<PetNode> = setOf(),
) : HasClassName {
  val custom = CUSTOM.expression in supertypes

  init {
    fun hasRefinement(it: Expression) = it.descendantsOfType<Requirement>().any()
    require(supertypes.none(::hasRefinement)) { supertypes }

    if (custom) {
      require(invariants.none())
      require(effects.none())
      require(defaultsDeclaration == DefaultsDeclaration())
    }
  }

  enum class ClassKind {
    CONCRETE,
    ABSTRACT
  }

  public val abstract = kind == ABSTRACT

  public data class DefaultsDeclaration(
      val universal: OneDefault = OneDefault(),
      val gainOnly: OneDefault = OneDefault(),
      val removeOnly: OneDefault = OneDefault(),
      val forClass: ClassName? = null,
  ) {
    data class OneDefault(val specs: List<Expression> = listOf(), val intensity: Intensity? = null)

    enum class DefaultKind {
      ALL_USAGES,
      GAIN_ONLY,
      REMOVE_ONLY
    }

    fun default(kind: DefaultKind) =
        when (kind) {
          ALL_USAGES -> universal
          GAIN_ONLY -> gainOnly
          REMOVE_ONLY -> removeOnly
        }

    companion object {
      fun merge(ones: Collection<OneDefault>): OneDefault {
        val deps = ones.map { it.specs }.firstOrNull { it.any() } ?: listOf()
        val intensity = ones.firstNotNullOfOrNull { it.intensity }
        return OneDefault(deps, intensity)
      }
      fun merge(defs: Collection<DefaultsDeclaration>): DefaultsDeclaration {
        return DefaultsDeclaration(
            universal = merge(defs.map { it.universal }),
            gainOnly = merge(defs.map { it.gainOnly }),
            removeOnly = merge(defs.map { it.removeOnly }),
            forClass = defs.mapNotNull { it.forClass }.singleOrNull(),
        )
      }
    }

    internal val allNodes: Set<PetNode> =
        listOf(universal, gainOnly, removeOnly).flatMap { it.specs }.toSet()
  }

  public val allNodes: Set<PetNode> by lazy {
    setOf<PetNode>() +
        className +
        shortName +
        supertypes +
        dependencies +
        invariants +
        effects +
        defaultsDeclaration.allNodes +
        extraNodes
  }
}
