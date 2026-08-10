package dev.martianzoo.data

import dev.martianzoo.api.SystemClasses.CUSTOM
import dev.martianzoo.data.ClassDeclaration.ClassKind.ABSTRACT
import dev.martianzoo.data.ClassDeclaration.DefaultsDeclaration.DefaultKind.ALL_USAGES
import dev.martianzoo.data.ClassDeclaration.DefaultsDeclaration.DefaultKind.GAIN_ONLY
import dev.martianzoo.data.ClassDeclaration.DefaultsDeclaration.DefaultKind.REMOVE_ONLY
import dev.martianzoo.pets.HasClassName
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction.Intensity
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.Requirement

/**
 * A direct representation of the *declaration* of a component class, such as GreeneryTile. Some of
 * these are written explicitly in `*.pets` source files, but others are converted programmatically
 * from [Definition] objects.
 *
 * The information provided here is not very "cooked"; that cooking happens in
 * `dev.martianzoo.types`.
 */
public data class ClassDeclaration(
    /** The stable engine-facing name for the class. No other name is part of the declaration. */
    override val className: ClassName,

    /** Is this class declared to be `ABSTRACT`, `CUSTOM`, or regular? */
    public val kind: ClassKind,

    /** Any "new" dependencies being declared by this class (not inherited from a supertype). */
    public val dependencies: List<Expression> = emptyList(),

    /** This class's listed direct supertypes, as they were expressed in the source. */
    public val supertypes: Set<Expression> = emptySet(),

    /** Any class invariants declared with `HAS` in the class body. */
    public val invariants: Set<Requirement> = emptySet(),

    /** The class's effects, in declaration order. Duplicate effects are preserved. */
    public val effects: List<Effect> = emptyList(),

    /** The merged contents of any `DEFAULT` clauses in the class body. */
    public val defaultsDeclaration: DefaultsDeclaration = DefaultsDeclaration(),
    public val docstring: String? = null,
    /**
     * Any additional Pets elements belonging to this class that aren't given for the previous
     * arguments.
     */
    internal val extraNodes: Set<PetNode> = emptySet(),
) : HasClassName {
  public val custom: Boolean = CUSTOM.expression in supertypes

  init {
    fun hasRefinement(it: Expression) = it.descendantsOfType<Requirement>().any()
    require(supertypes.none(::hasRefinement)) { supertypes }

    if (custom) {
      require(invariants.none())
      require(effects.none())
      require(defaultsDeclaration == DefaultsDeclaration())
    }
  }

  public enum class ClassKind {
    CONCRETE,
    ABSTRACT,
  }

  public val abstract: Boolean = kind == ABSTRACT

  public data class DefaultsDeclaration(
      val universal: OneDefault = OneDefault(),
      val gainOnly: OneDefault = OneDefault(),
      val removeOnly: OneDefault = OneDefault(),
      val forClass: ClassName? = null,
  ) {
    public data class OneDefault(
        val specs: List<Expression> = emptyList(),
        val intensity: Intensity? = null,
    )

    internal enum class DefaultKind {
      ALL_USAGES,
      GAIN_ONLY,
      REMOVE_ONLY,
    }

    internal fun default(kind: DefaultKind) =
        when (kind) {
          ALL_USAGES -> universal
          GAIN_ONLY -> gainOnly
          REMOVE_ONLY -> removeOnly
        }

    internal companion object {
      internal fun merge(defs: Collection<DefaultsDeclaration>): DefaultsDeclaration {
        return DefaultsDeclaration(
            universal = merge(defs.map { it.universal }),
            gainOnly = merge(defs.map { it.gainOnly }),
            removeOnly = merge(defs.map { it.removeOnly }),
            forClass = defs.mapNotNull { it.forClass }.singleOrNull(),
        )
      }

      private fun merge(ones: Collection<OneDefault>): OneDefault {
        val deps = ones.map { it.specs }.firstOrNull { it.isNotEmpty() }.orEmpty()
        val intensity = ones.firstNotNullOfOrNull { it.intensity }
        return OneDefault(deps, intensity)
      }
    }

    internal val allNodes: Set<PetNode> =
        listOf(universal, gainOnly, removeOnly).flatMap { it.specs }.toSet()
  }

  /**
   * This declaration stripped of the rules it contributes by itself (its invariants, effects, and
   * defaults), leaving only its name, hierarchy, and dependencies. This is the form a phantom class
   * is loaded from, since an inactive class contributes no rules.
   */
  public fun withoutDeclaredBehavior(): ClassDeclaration =
      copy(
          invariants = emptySet(),
          effects = emptyList(),
          defaultsDeclaration = DefaultsDeclaration(),
      )

  public val allNodes: Set<PetNode> by lazy {
    setOf<PetNode>() +
        className +
        supertypes +
        dependencies +
        invariants +
        effects +
        defaultsDeclaration.allNodes +
        extraNodes
  }
}
