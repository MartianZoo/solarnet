package dev.martianzoo.pets.ast

/**
 * Authored effect syntax, including whole-effect marks under
 * [section 8](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#8-transform-blocks).
 * Execution consumes the concrete [Effect] after expansion.
 */
public sealed class EffectTree : PetElement() {
  /** The written trigger and instruction, without applying enclosing transforms; for inspection. */
  public val untransformed: Effect
    get() =
        when (this) {
          is Effect -> this
          is Transform -> inner.untransformed
        }

  /**
   * Extracts the instruction while retaining enclosing transform marks. The caller must supply any
   * bindings that the removed trigger originally supplied before executing the copy.
   */
  public fun instructionWithTransforms(): InstructionTree =
      when (this) {
        is Effect -> instruction
        is Transform -> Instruction.Transform(inner.instructionWithTransforms(), transformKind)
      }

  override val kind: kotlin.reflect.KClass<out PetNode> = EffectTree::class

  /** A whole effect marked for the named transform. */
  public data class Transform(val inner: EffectTree, override val transformKind: String) :
      EffectTree(), TransformNode<EffectTree> {
    override fun extract(): EffectTree = inner

    override fun visitChildren(visitor: Visitor): Unit = visitor.visit(inner)

    override fun toString(): String = "$transformKind[$inner]"
  }
}
