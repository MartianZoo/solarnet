package dev.martianzoo.pets.data

import dev.martianzoo.pets.HasClassName
import dev.martianzoo.pets.Transforming.actionListToEffects
import dev.martianzoo.pets.api.Exceptions.InvalidPetDefinitionException
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.api.SystemClasses.CUSTOM_INSTRUCTION
import dev.martianzoo.pets.api.SystemClasses.CUSTOM_METRIC
import dev.martianzoo.pets.api.SystemClasses.THIS
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction.Quantifier
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.PropertyName
import dev.martianzoo.pets.ast.PropertyValue
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.data.ClassDeclaration.ClassKind.ABSTRACT
import dev.martianzoo.pets.data.ClassDeclaration.DefaultsDeclaration.DefaultKind.ALL_USAGES
import dev.martianzoo.pets.data.ClassDeclaration.DefaultsDeclaration.DefaultKind.GAIN_ONLY
import dev.martianzoo.pets.data.ClassDeclaration.DefaultsDeclaration.DefaultKind.REMOVE_ONLY
import dev.martianzoo.pets.data.ClassDeclaration.DefaultsDeclaration.OneDefault

/**
 * A direct representation of the *declaration* of a component class, such as GreeneryTile, whose
 * source form is
 * [section 11](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#11-class-declarations).
 * A declaration is a signature — a name, an optional dependency list, and an optional supertype
 * list — plus an optional body ([rules L11-3 and
 * L11-5](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#11-class-declarations)).
 * Runtime Catalogs normally load these from `*.pets` source; tools and tests may construct them
 * directly.
 *
 * The information provided here is not very "cooked"; that cooking happens in
 * `dev.martianzoo.pets.types`.
 */
public data class ClassDeclaration(
    /**
     * The stable engine-facing name for the class. No other name is part of the declaration: there
     * is one namespace and no scoping ([rule
     * L10-5](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#10-names)),
     * and how a name is displayed to a person is `NAMING.md`'s subject.
     */
    override val className: ClassName,

    /**
     * Is this class declared to be `ABSTRACT` or regular? `CLASS Foo` declares a concrete class and
     * `ABSTRACT CLASS Foo` an abstract one ([rule
     * L11-3](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#11-class-declarations),
     * [rule T2-1](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#2-classes)).
     */
    public val kind: ClassKind,

    /** Any "new" dependencies being declared by this class (not inherited from a supertype). */
    public val dependencies: List<Expression> = emptyList(),

    /**
     * This class's direct supertypes in authored order, which determines inherited dependency-key
     * order under
     * [rule T3-2](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#3-dependencies).
     */
    public val supertypes: List<Expression> = emptyList(),

    /**
     * Any class invariants declared with `HAS` in the class body ([rule
     * L11-5](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#11-class-declarations)).
     */
    public val invariants: Set<Requirement> = emptySet(),

    /** Effects authored directly in this class body, in declaration order. */
    public val authoredEffects: List<Effect> = emptyList(),

    /** Actions authored directly in this class body. */
    public val authoredActions: List<Action> = emptyList(),

    /** An catalog-specific executable form, when it differs from the authored form. */
    internal val executableEffects: List<Effect>? = null,

    /**
     * The merged contents of any `DEFAULT` clauses in the class body. A clause names the class that
     * declares it — one naming another class is rejected — and clauses are merged into one set per
     * use kind ([rule
     * L11-8](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#11-class-declarations),
     * [rules T10-1 and T10-3](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#10-defaults)).
     */
    public val defaultsDeclaration: DefaultsDeclaration = DefaultsDeclaration(),

    /**
     * Property bounds or values declared directly by this class. A name is assigned at most once
     * per body ([rule
     * L11-9](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#11-class-declarations));
     * what the bounds and values mean is
     * [section 9](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#9-class-properties)
     * of the type system specification.
     */
    public val properties: Map<PropertyName, PropertyValue> = emptyMap(),

    /**
     * The quoted string written on the line before `CLASS`, retained here and re-emitted when this
     * declaration is rendered ([rule
     * L11-7](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#11-class-declarations)).
     */
    public val docstring: String? = null,
    /**
     * Any additional Pets elements belonging to this class that aren't given for the previous
     * arguments.
     */
    public val extraNodes: Set<PetNode> = emptySet(),
) : HasClassName {
  /**
   * This class's authored effects followed by its lowered actions, in that order ([rule
   * L7-6](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#7-actions)).
   * No rule may rely on the ordering to resolve simultaneous gameplay — sequencing owns that — but
   * it makes introspection deterministic once actions have become ordinary effects.
   */
  // TODO: Contract temporary tfm-canon declaration-lowering seams.
  public val authoredEffectsWithActions: List<Effect>
    get() = authoredEffects + actionListToEffects(authoredActions)

  /**
   * The effects this class actually carries: [authoredEffectsWithActions], in the order
   * [rule L7-6](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#7-actions)
   * gives, unless a Catalog supplied its own executable form for this class — in which case that
   * replaces the whole list.
   */
  public val effects: List<Effect>
    get() = executableEffects ?: authoredEffectsWithActions

  public val customMetric: Boolean = CUSTOM_METRIC.expression in supertypes

  public val customInstruction: Boolean = CUSTOM_INSTRUCTION.expression in supertypes

  init {
    if (className == THIS)
        throw PetSyntaxException(
            "`This` refers to the enclosing class and cannot be declared as a class name",
            sourceLocation = className.sourceLocation,
        )
    if (defaultsDeclaration.forClass !in setOf(null, className)) {
      throw PetSyntaxException(
          "`$className` cannot declare defaults for `${defaultsDeclaration.forClass}`; name `$className` instead",
          sourceLocation = defaultsDeclaration.forClass?.sourceLocation,
      )
    }
    fun hasRefinement(expression: Expression): Boolean =
        expression.refinement != null || expression.arguments.any(::hasRefinement)
    // Rule L11-4: a refined type cannot be a bound, so signature expressions carry no refinements
    // at
    // any depth.
    (dependencies + supertypes).firstOrNull(::hasRefinement)?.let {
      throw PetSyntaxException(
          "class signatures cannot contain refined types: `$it`",
          sourceLocation = it.sourceLocation,
      )
    }

    if (customMetric) {
      val behavior =
          when {
            invariants.isNotEmpty() -> "invariants"
            effects.isNotEmpty() -> "effects or actions"
            defaultsDeclaration != DefaultsDeclaration() -> "defaults"
            else -> null
          }
      if (behavior != null)
          throw PetSyntaxException(
              "custom metric `$className` cannot declare Pets $behavior; its behavior comes from its Kotlin implementation",
              sourceLocation =
                  invariants.firstOrNull()?.sourceLocation
                      ?: effects.firstOrNull()?.sourceLocation
                      ?: defaultsDeclaration.forClass?.sourceLocation
                      ?: className.sourceLocation,
          )
    }
  }

  public enum class ClassKind {
    CONCRETE,
    ABSTRACT,
  }

  public companion object {
    /**
     * Indexes a Catalog's declarations, allowing identical contributions but rejecting conflicts.
     */
    public fun indexByName(
        declarations: Iterable<ClassDeclaration>
    ): Map<ClassName, ClassDeclaration> = buildMap {
      declarations.forEach { declaration ->
        val previous = get(declaration.className)
        if (previous != null && previous != declaration) {
          val firstLocation = previous.className.sourceLocation
          val first =
              firstLocation
                  ?.takeIf { it.source == declaration.className.sourceLocation?.source }
                  ?.let { " at ${it.line}:${it.column}" }
                  .orEmpty()
          throw InvalidPetDefinitionException(
              "conflicting declarations of `${declaration.className}`; first declared$first as `${previous.copy(docstring = null).toString(oneLine = true)}`",
              sourceLocation = declaration.className.sourceLocation,
          )
        }
        if (previous == null) put(declaration.className, declaration)
      }
    }
  }

  public val abstract: Boolean = kind == ABSTRACT

  public data class DefaultsDeclaration(
      val universal: OneDefault = OneDefault(),
      val gainOnly: OneDefault = OneDefault(),
      val removeOnly: OneDefault = OneDefault(),
      val forClass: ClassName? = null,
  ) {
    init {
      require(
          forClass != null || listOf(universal, gainOnly, removeOnly).all { it == OneDefault() }
      ) {
        "defaults without a declaring class cannot be rendered or applied"
      }
    }

    public data class OneDefault(
        val specs: List<Expression> = emptyList(),
        val quantifier: Quantifier? = null,
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

    public companion object {
      public fun merge(defs: Collection<DefaultsDeclaration>): DefaultsDeclaration {
        val owners = defs.mapNotNull { it.forClass }.distinct()
        if (owners.size > 1) {
          throw PetSyntaxException(
              "`DEFAULT` clauses name different classes: ${owners.joinToString { "`$it`" }}",
              sourceLocation = owners[1].sourceLocation,
          )
        }
        fun mergeKind(kind: DefaultKind): OneDefault {
          var merged = OneDefault()
          defs.forEach { definition ->
            try {
              merged = merge(listOf(merged, definition.default(kind)))
            } catch (e: IllegalArgumentException) {
              throw PetSyntaxException(
                  "invalid defaults for `${owners.singleOrNull()}`: ${e.message}",
                  e,
                  definition.forClass?.sourceLocation,
              )
            }
          }
          return merged
        }
        return DefaultsDeclaration(
            universal = mergeKind(ALL_USAGES),
            gainOnly = mergeKind(GAIN_ONLY),
            removeOnly = mergeKind(REMOVE_ONLY),
            forClass = owners.singleOrNull(),
        )
      }

      private fun merge(ones: Collection<OneDefault>): OneDefault {
        val dependencyCandidates = ones.map(OneDefault::specs).filter { it.isNotEmpty() }.distinct()
        require(dependencyCandidates.size <= 1) {
          "conflicting dependency defaults: ${dependencyCandidates.joinToString { "`${it.joinToString(", ", "<", ">")}`" }}"
        }
        val quantifierCandidates = ones.mapNotNull(OneDefault::quantifier).distinct()
        require(quantifierCandidates.size <= 1) {
          "conflicting quantifier defaults: ${quantifierCandidates.joinToString { "`${it.symbol}`" }}"
        }
        return OneDefault(
            dependencyCandidates.singleOrNull().orEmpty(),
            quantifierCandidates.singleOrNull(),
        )
      }
    }

    internal val allNodes: Set<PetNode> =
        listOf(universal, gainOnly, removeOnly).flatMap { it.specs }.toSet()
  }

  public val allNodes: Set<PetNode> =
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

  /**
   * Returns this declaration as standalone, parseable Pets source ([rule
   * L11-10](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#11-class-declarations)).
   */
  override fun toString(): String = toString(oneLine = false)

  /**
   * Returns this declaration as parseable Pets source, multi-line or (with [oneLine]) semicolon
   * separated. Parsing either form yields an equal declaration ([rule
   * L11-10](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#11-class-declarations)).
   */
  public fun toString(oneLine: Boolean): String = buildString {
    docstring?.let { append('"').append(it).append("\"\n") }
    if (abstract) append("ABSTRACT ")
    append("CLASS ").append(className)
    if (dependencies.isNotEmpty()) dependencies.joinTo(this, ", ", "<", ">")
    if (supertypes.isNotEmpty()) {
      supertypes.joinTo(this, ", ", " : ")
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
      if (gainOnly.specs.isNotEmpty() || gainOnly.quantifier != null) {
        add("DEFAULT +${gainOnly.expression()}${gainOnly.quantifier?.symbol.orEmpty()}")
      }
      if (removeOnly.specs.isNotEmpty() || removeOnly.quantifier != null) {
        add("DEFAULT -${removeOnly.expression()}${removeOnly.quantifier?.symbol.orEmpty()}")
      }
      if (isEmpty()) add("DEFAULT $owner")
    }
  }
}
