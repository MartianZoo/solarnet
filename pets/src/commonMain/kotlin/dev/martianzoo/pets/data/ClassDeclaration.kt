package dev.martianzoo.pets.data

import dev.martianzoo.pets.HasClassName
import dev.martianzoo.pets.Transforming.actionListToEffects
import dev.martianzoo.pets.api.SystemClasses.CUSTOM
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction.Intensity
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.PropertyName
import dev.martianzoo.pets.ast.PropertyValue
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.data.ClassDeclaration.ClassKind.ABSTRACT
import dev.martianzoo.pets.data.ClassDeclaration.DefaultsDeclaration.DefaultKind.ALL_USAGES
import dev.martianzoo.pets.data.ClassDeclaration.DefaultsDeclaration.DefaultKind.GAIN_ONLY
import dev.martianzoo.pets.data.ClassDeclaration.DefaultsDeclaration.DefaultKind.REMOVE_ONLY
import dev.martianzoo.pets.data.ClassDeclaration.DefaultsDeclaration.DefaultKind.TRIGGER_ONLY
import dev.martianzoo.pets.data.ClassDeclaration.DefaultsDeclaration.OneDefault

/**
 * A direct representation of the *declaration* of a component class, such as GreeneryTile. Some of
 * these are written explicitly in `*.pets` source files, but others are converted programmatically
 * from [Definition] objects.
 *
 * The information provided here is not very "cooked"; that cooking happens in
 * `dev.martianzoo.pets.types`.
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

    /** Effects authored directly in this class body, in declaration order. */
    public val authoredEffects: List<Effect> = emptyList(),

    /** Actions authored directly in this class body. */
    public val authoredActions: List<Action> = emptyList(),

    /** An catalog-specific executable form, when it differs from the authored form. */
    internal val executableEffects: List<Effect>? = null,

    /** The merged contents of any `DEFAULT` clauses in the class body. */
    public val defaultsDeclaration: DefaultsDeclaration = DefaultsDeclaration(),

    /** Property bounds or values declared directly by this class. */
    public val properties: Map<PropertyName, PropertyValue> = emptyMap(),
    public val docstring: String? = null,
    /**
     * Any additional Pets elements belonging to this class that aren't given for the previous
     * arguments.
     */
    public val extraNodes: Set<PetNode> = emptySet(),
) : HasClassName {
  // TODO: Contract temporary tfm-canon declaration-lowering seams.
  public val authoredEffectsWithActions: List<Effect>
    get() = authoredEffects + actionListToEffects(authoredActions)

  /** Effects authored directly or obtained by lowering the authored actions. */
  public val effects: List<Effect>
    get() = executableEffects ?: authoredEffectsWithActions

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
      val triggerOnly: OneDefault = OneDefault(),
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
      TRIGGER_ONLY,
    }

    internal fun default(kind: DefaultKind) =
        when (kind) {
          ALL_USAGES -> universal
          GAIN_ONLY -> gainOnly
          REMOVE_ONLY -> removeOnly
          TRIGGER_ONLY -> triggerOnly
        }

    internal companion object {
      internal fun merge(defs: Collection<DefaultsDeclaration>): DefaultsDeclaration {
        return DefaultsDeclaration(
            universal = merge(defs.map { it.universal }),
            gainOnly = merge(defs.map { it.gainOnly }),
            removeOnly = merge(defs.map { it.removeOnly }),
            triggerOnly = merge(defs.map { it.triggerOnly }),
            forClass = defs.mapNotNull { it.forClass }.distinct().singleOrNull(),
        )
      }

      private fun merge(ones: Collection<OneDefault>): OneDefault {
        val deps = ones.map { it.specs }.firstOrNull { it.isNotEmpty() }.orEmpty()
        val intensity = ones.firstNotNullOfOrNull { it.intensity }
        return OneDefault(deps, intensity)
      }
    }

    internal val allNodes: Set<PetNode> =
        listOf(universal, gainOnly, removeOnly, triggerOnly).flatMap { it.specs }.toSet()
  }

  public val allNodes: Set<PetNode> by lazy {
    setOf<PetNode>() +
        className +
        supertypes +
        dependencies +
        invariants +
        effects +
        defaultsDeclaration.allNodes +
        properties.keys +
        properties.values +
        extraNodes
  }

  /** Returns this declaration as standalone, parseable Pets source. */
  override fun toString(): String = toString(oneLine = false)

  /** Returns this declaration as parseable Pets source, optionally on one line. */
  public fun toString(oneLine: Boolean): String = buildString {
    docstring?.let { append('"').append(it).append("\"\n") }
    if (abstract) append("ABSTRACT ")
    append("CLASS ").append(className)
    if (dependencies.isNotEmpty()) dependencies.joinTo(this, ", ", "<", ">")
    if (supertypes.isNotEmpty()) {
      supertypes.sortedBy(Expression::toString).joinTo(this, ", ", " : ")
    }

    val body = buildList {
      invariants.sortedBy(Requirement::toString).mapTo(this) { "HAS $it" }
      addAll(defaultsDeclaration.toPets())
      properties.mapTo(this) { (name, value) -> "$name = $value" }
      authoredEffects.mapTo(this, Effect::toString)
      authoredActions.mapTo(this, Action::toString)
    }
    if (body.isNotEmpty()) {
      if (oneLine) body.joinTo(this, separator = "; ", prefix = " { ", postfix = " }")
      else body.joinTo(this, separator = "\n  ", prefix = " {\n  ", postfix = "\n}")
    }
  }

  private fun DefaultsDeclaration.toPets(): List<String> {
    val owner = forClass ?: return emptyList()

    fun OneDefault.expression(): String =
        if (specs.isEmpty()) "$owner" else specs.joinToString(", ", "$owner<", ">")

    return buildList {
      if (universal.specs.isNotEmpty()) add("DEFAULT ${universal.expression()}")
      if (gainOnly.specs.isNotEmpty() || gainOnly.intensity != null) {
        add("DEFAULT +${gainOnly.expression()}${gainOnly.intensity?.symbol.orEmpty()}")
      }
      if (removeOnly.specs.isNotEmpty() || removeOnly.intensity != null) {
        add("DEFAULT -${removeOnly.expression()}${removeOnly.intensity?.symbol.orEmpty()}")
      }
      if (triggerOnly.specs.isNotEmpty()) add("DEFAULT ${triggerOnly.expression()}:")
      if (isEmpty()) add("DEFAULT $owner")
    }
  }
}
