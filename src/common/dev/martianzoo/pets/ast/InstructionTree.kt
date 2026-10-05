package dev.martianzoo.pets.ast

import dev.martianzoo.pets.Specification
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.TypeInfo
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.Instruction.Or
import dev.martianzoo.pets.types.ClassTable

/**
 * A complete Pets instruction syntax tree. Most engine code should use [Instruction] when it needs
 * one task or [InstructionGroup] when it needs independent work; this broader type is for Pets
 * composition and the transitions that deliberately convert between those forms.
 */
public sealed class InstructionTree : PetElement(), Specification<InstructionTree> {
  /** Returns a tree that does this tree [factor] times. */
  public abstract operator fun times(factor: Int): InstructionTree

  /**
   * Whether this tree still requires a gameplay choice or other narrowing. Per
   * [rule L3-1](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#3-narrowing)
   * that means an unfixed `X`, an absent or optional quantifier, an abstract expression, or an
   * `OR`. This judgment is about *elaborated* instructions: an authored change carries no
   * quantifier until elaboration supplies one.
   */
  public abstract override fun isAbstract(info: TypeInfo): Boolean

  /**
   * Combines the choices constrained by both trees, using [table] for Type intersections. Returns
   * null when no common narrowing can be represented. Fixed structure (including gates, metrics,
   * performers and stage counts) must agree; this does not resolve instructions or choose among the
   * remaining alternatives. Surviving alternatives are checked against both operands' narrowing
   * rules, including shared choices within sequences and transmutations. An overlapping Type domain
   * without a single expressible bound, or a combined tree unable to retain shared choices, throws
   * [NarrowingException] rather than guessing or treating the inputs as disjoint. The caller must
   * supply a more specific choice.
   */
  public fun intersect(
      that: InstructionTree,
      table: ClassTable,
      info: TypeInfo,
  ): InstructionTree? = InstructionIntersection(table, info).intersect(this, that)

  /**
   * Ensures that this tree is a valid narrowing of [that] — an acceptable way of carrying out the
   * more general [that], as defined by
   * [section 3](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#3-narrowing).
   *
   * Narrowing preserves the kind of node, the number of `THEN` stages and the size of a group, with
   * exactly two exceptions: any instruction may narrow an [Or] by narrowing one arm, and [NoOp] may
   * narrow an optional change ([rule
   * L3-2](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#3-narrowing)).
   *
   * @throws NarrowingException if this tree does not narrow [that], saying why. A failure caused by
   *   something else — an unknown class, a malformed proposal — propagates instead of becoming a
   *   narrowing refusal ([rule
   *   L3-10](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#3-narrowing)).
   */
  public override fun ensureNarrows(that: InstructionTree, info: TypeInfo) {
    if (that !is Or && this != NoOp && this::class != that::class) {
      throw NarrowingException("`$this` cannot narrow `$that`: instruction types differ")
    }
    try {
      that.ensureIsNarrowedBy(this, info)
    } catch (e: NarrowingException) {
      throw NarrowingException("`$this` does not narrow `$that`", e)
    }
  }

  protected abstract fun ensureIsNarrowedBy(proposed: InstructionTree, info: TypeInfo)

  override val kind: kotlin.reflect.KClass<out PetNode> = InstructionTree::class
}
