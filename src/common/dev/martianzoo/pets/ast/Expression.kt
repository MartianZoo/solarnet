package dev.martianzoo.pets.ast

import dev.martianzoo.pets.HasClassName
import dev.martianzoo.pets.HasExpression
import dev.martianzoo.pets.Specification
import dev.martianzoo.pets.api.TypeInfo
import dev.martianzoo.pets.types.ClassLoader
import dev.martianzoo.pets.types.Type
import kotlin.reflect.KClass

/**
 * The noun of the Pets language: a particular *representation* of a type, as defined by
 * [section 1](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#1-expressions).
 * It appears in every other element, and everywhere it appears it identifies a type.
 *
 * An expression is a class name, an optional argument list and an optional refinement ([rule
 * L1-1](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#1-expressions)).
 * Each argument (like `Ants` in `Microbe<Player1, Ants>`) is itself an expression; a refinement is
 * a conjunction of state-aware requirements (as in `Card(HAS VenusTag)`) and structural differences
 * (as in `Anyone(NOT Player1)`) ([rule
 * L1-3](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#1-expressions)).
 *
 * Two expressions are equal only when their spellings agree ([rule
 * L1-8](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#1-expressions)),
 * so a single type has many unequal representations: `Microbe<This, Player1>` and `Microbe<Player1,
 * This>` are different expressions, as are `Tile` and `Tile<Area>`. The type system, not the
 * syntax, is the authority on identity — [ClassLoader] resolves all four into their [Type]s,
 * collapsing the distinctions the syntax deliberately keeps ([rule
 * L1-9](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#1-expressions)).
 */
// A parser may attach source-only information through a subtype and extract it before returning
// an AST. Structural operations and copy always describe the ordinary expression.
public open class Expression(
    final override val className: ClassName,

    /** The written argument list, each argument an expression in its own right. */
    public val arguments: List<Expression> = emptyList(),

    /** The written refinement, or null if none was written. */
    public val refinement: Refinement? = null,

    /**
     * Whether the source wrote angle brackets, including an explicit empty `<>`. Writing an empty
     * argument list is not the same as writing none: the two denote one type but stay
     * distinguishable, because `<>` says "I accept this use's defaults on purpose" ([rule
     * L1-2](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#1-expressions)).
     */
    public val argumentsSpecified: Boolean = arguments.isNotEmpty(),

    /**
     * An explicit Type-variable marker declared by this expression or referenced here. A reference
     * keeps the declaration's structural expression in [className], [arguments], and [refinement],
     * so ordinary Type operations remain unaware of the shorter authored spelling.
     */
    public val typeVariableName: TypeVariableName? = null,
) : PetElement(), HasClassName, HasExpression, Specification<Expression> {
  // Expressions are immutable after parsing; zero is the uncached sentinel.
  private var cachedHashCode: Int = 0

  /** Returns this expression with the specified structural members changed. */
  public fun copy(
      className: ClassName = this.className,
      arguments: List<Expression> = this.arguments,
      refinement: Refinement? = this.refinement,
      argumentsSpecified: Boolean = this.argumentsSpecified,
      typeVariableName: TypeVariableName? = this.typeVariableName,
  ): Expression = Expression(className, arguments, refinement, argumentsSpecified, typeVariableName)

  override fun equals(other: Any?): Boolean =
      this === other ||
          (other is Expression &&
              this::class == other::class &&
              className == other.className &&
              arguments == other.arguments &&
              refinement == other.refinement &&
              argumentsSpecified == other.argumentsSpecified &&
              equalityTypeVariableName == other.equalityTypeVariableName)

  private val equalityTypeVariableName: TypeVariableName?
    get() = typeVariableName.takeUnless { it is TypeVariableName.ExpandedReference }

  override fun hashCode(): Int {
    if (cachedHashCode != 0) return cachedHashCode
    var result = className.hashCode()
    result = 31 * result + arguments.hashCode()
    result = 31 * result + (refinement?.hashCode() ?: 0)
    result = 31 * result + argumentsSpecified.hashCode()
    result = 31 * result + (equalityTypeVariableName?.hashCode() ?: 0)
    cachedHashCode = result
    return result
  }

  final override val expression: Expression
    get() = this

  final override fun isAbstract(info: TypeInfo): Boolean = info.isAbstract(this)

  final override fun ensureNarrows(that: Expression, info: TypeInfo): Unit =
      info.ensureNarrows(that, this)

  final override fun visitChildren(visitor: Visitor) {
    visitor.visit(className)
    visitor.visit(arguments)
    visitor.visit(refinement)
  }

  final override fun toString(): String = buildString {
    if (typeVariableName is TypeVariableName.UnqualifiedReference) {
      append(typeVariableName.name).append('@')
      return@buildString
    }
    val authoredMarker = typeVariableName?.takeIf {
      it is TypeVariableName.Declaration || it is TypeVariableName.Reference
    }
    authoredMarker?.let { append(it.name.orEmpty()).append('@') }
    append(className)
    val reference = authoredMarker as? TypeVariableName.Reference
    if (reference?.argumentsSpecified ?: argumentsSpecified) {
      append(arguments.joinToString(", ", "<", ">"))
    }
    if (reference == null) refinement?.let { append("($it)") }
  }

  /** Does this expression consist only of a class name, with no arguments and no refinement? */
  public val simple: Boolean = arguments.isEmpty() && refinement == null && !argumentsSpecified

  /** The internal roles assigned to occurrences of one explicit Type-variable marker. */
  public sealed class TypeVariableName private constructor() {
    /** One resolved lexical variable, distinct from an equal marker in a nested scope. */
    internal class Resolution

    /** The authored local name, or null when the explicit `@` marker is anonymous. */
    public abstract val name: String?

    /** The exact root Class qualified by this marker. */
    public abstract val boundClassName: ClassName

    internal val key: Pair<ClassName, String?>
      get() = boundClassName to name

    /** The marker as it appears before arguments or refinements in authored Pets. */
    public val authoredSpelling: String
      get() = "${name.orEmpty()}@" + if (this is UnqualifiedReference) "" else boundClassName

    internal abstract val resolution: Resolution?

    /** Resolved occurrences compare by lexical scope; unresolved parser markers compare by key. */
    internal val identity: Any
      get() = resolution ?: key

    /** Source shorthand awaiting a uniquely named binding in an enclosing scope. */
    internal data class UnqualifiedReference(override val name: String) : TypeVariableName() {
      // This structural placeholder is replaced before Type interpretation.
      override val boundClassName: ClassName = dev.martianzoo.pets.api.SystemClasses.COMPONENT
      override val resolution: Resolution? = null
    }

    /** The occurrence selected internally to supply the value shared by matching markers. */
    public class Declaration
    private constructor(
        override val name: String?,
        override val boundClassName: ClassName,
        override val resolution: Resolution?,
    ) : TypeVariableName() {
      public constructor(
          name: String?,
          boundClassName: ClassName,
      ) : this(name, boundClassName, null)

      /** Whether this occurrence has been assigned to a lexical scope. */
      public val resolved: Boolean
        get() = resolution != null

      override fun equals(other: Any?): Boolean = other is Declaration && key == other.key

      override fun hashCode(): Int = key.hashCode()

      internal fun resolved(resolution: Resolution): Declaration =
          Declaration(name, boundClassName, resolution)
    }

    /** Another occurrence with the same marker in the same scope. */
    public class Reference
    private constructor(
        override val name: String?,
        override val boundClassName: ClassName,
        internal val argumentsSpecified: Boolean,
        override val resolution: Resolution?,
    ) : TypeVariableName() {
      internal constructor(
          name: String?,
          boundClassName: ClassName,
          argumentsSpecified: Boolean = false,
          resolved: Boolean = false,
      ) : this(
          name,
          boundClassName,
          argumentsSpecified,
          if (resolved) Resolution() else null,
      )

      /** Whether this occurrence has been assigned to a lexical scope. */
      public val resolved: Boolean
        get() = resolution != null

      override fun equals(other: Any?): Boolean =
          other is Reference && key == other.key && argumentsSpecified == other.argumentsSpecified

      override fun hashCode(): Int = 31 * key.hashCode() + argumentsSpecified.hashCode()

      override fun toString(): String = "Reference(boundClassName=$boundClassName, name=$name)"

      internal fun resolved(resolution: Resolution): Reference =
          Reference(name, boundClassName, argumentsSpecified, resolution)
    }

    /** A resolved reference to the class named by the surrounding refined `Class<T>`. */
    internal data class RepresentedClassReference(
        override val boundClassName: ClassName,
    ) : TypeVariableName() {
      override val name: String = boundClassName.asString
      override val resolution: Resolution? = null
    }

    /** Identity retained after an explicit reference is expanded to its structural expression. */
    internal class ExpandedReference(
        override val name: String?,
        override val boundClassName: ClassName,
        override val resolution: Resolution?,
    ) : TypeVariableName()
  }

  /**
   * Is this just the name [name], with no arguments and no refinement, however the empty argument
   * list was written? `This` and `This<>` are both the bare `This` placeholder; they are not equal
   * as expressions, because they render differently, but neither one carries an argument.
   */
  internal fun isBare(name: ClassName): Boolean =
      className == name && arguments.isEmpty() && refinement == null

  /**
   * Returns this expression with [moreArgs] added after its existing [arguments]. Any resulting
   * non-empty list counts as written, so the result has [argumentsSpecified] set.
   */
  public fun appendArguments(moreArgs: List<Expression>): Expression =
      replaceArguments(arguments + moreArgs)

  private fun replaceArguments(newArgs: List<Expression>): Expression =
      copy(
          arguments = newArgs,
          argumentsSpecified = argumentsSpecified || newArgs.isNotEmpty(),
      )

  /**
   * Returns this expression with the given refinement. This expression must not already have a
   * refinement.
   */
  internal fun has(refinement: Refinement?): Expression {
    require(this.refinement == null)
    return if (refinement == null) this else copy(refinement = refinement)
  }

  internal fun has(refinement: Requirement?): Expression {
    require(this.refinement == null)
    return if (refinement != null) copy(refinement = Refinement.has(refinement)) else this
  }

  final override val kind: KClass<out PetNode> = Expression::class

  /**
   * One clause of an expression's refinement, or a conjunction of them. Each clause repeats its own
   * keyword, so a top-level comma separates clauses rather than continuing one ([rule
   * L1-3](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#1-expressions));
   * what a clause *means* is
   * [section 8](https://github.com/MartianZoo/solarnet/blob/main/docs/type-system-spec.md#8-refinements)
   * of the type system specification.
   */
  public sealed class Refinement private constructor() : PetNode() {
    /** A predicate's bound meaning does not depend on a local choice's syntax marker. */
    internal fun withoutChoiceNames(): Refinement =
        object : dev.martianzoo.pets.PetTransformer() {
              override fun transformNode(node: PetNode): PetNode =
                  if (
                      node is Expression &&
                          node.typeVariableName !is TypeVariableName.RepresentedClassReference
                  )
                      transformChildren(node.copy(typeVariableName = null))
                  else transformChildren(node)
            }
            .transformRefinement(this)

    override val kind: KClass<out PetNode> = Refinement::class

    /**
     * Admits only the components of the outer domain meeting [requirement]. A conjunction inside
     * one `HAS` must be grouped, since a bare comma would start the next clause instead ([rule
     * L1-3](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#1-expressions)).
     */
    public data class Has(val requirement: Requirement) : Refinement() {
      init {
        require(requirement !is Requirement.And) {
          "a `HAS` clause cannot contain a top-level requirement conjunction"
        }
      }

      override fun visitChildren(visitor: Visitor): Unit = visitor.visit(requirement)

      override fun toString(): String = "HAS $requirement"
    }

    /** Excludes every Type overlapping [excluded] from the explicitly written outer domain. */
    public data class Not(val excluded: Expression) : Refinement() {
      override fun visitChildren(visitor: Visitor): Unit = visitor.visit(excluded)

      override fun toString(): String = "NOT $excluded"
    }

    /**
     * Admits only what every one of [refinements] admits — the comma-separated conjunction of
     * [rule L1-3](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#1-expressions).
     * Conjunctions do not nest; build one through [create].
     */
    @ConsistentCopyVisibility
    public data class And internal constructor(val refinements: Set<Refinement>) : Refinement() {
      init {
        require(refinements.size >= 2)
        require(refinements.none { it is And })
      }

      override fun visitChildren(visitor: Visitor): Unit = visitor.visit(refinements)

      override fun toString(): String = refinements.joinToString()
    }

    public companion object {
      /** The one refinement meaning "both". */
      internal fun join(ref1: Refinement, ref2: Refinement): Refinement {
        if (ref1 == ref2) return ref1
        return create(ref1.conjuncts() + ref2.conjuncts())
      }

      /**
       * Returns the refinement meeting all of [refinements], flattening any nested conjunctions. A
       * single clause is returned as itself rather than as an [And]. At least one is required.
       */
      public fun create(refinements: Collection<Refinement>): Refinement {
        val flattened = refinements.flatMapTo(linkedSetOf()) { it.conjuncts() }
        require(flattened.isNotEmpty())
        return if (flattened.size == 1) flattened.single() else And(flattened)
      }

      /** Builds and normalizes a HAS refinement from a requirement. */
      public fun has(requirement: Requirement): Refinement =
          create(Requirement.split(requirement).map(::Has))
    }

    internal fun conjuncts(): Set<Refinement> = if (this is And) refinements else setOf(this)

    internal fun retaining(predicate: (Refinement) -> Boolean): Refinement? =
        conjuncts().filter(predicate).takeIf { it.isNotEmpty() }?.let(Companion::create)
  }
}
