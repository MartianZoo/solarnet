package dev.martianzoo.pets.ast

/**
 * Authored action syntax, including whole-action marks under
 * [section 8](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#8-transform-blocks).
 * Lowering an action to effects preserves its enclosing marks for elaboration.
 */
public sealed class ActionTree : PetElement() {
  /** The written cost and result without applying enclosing transforms; for inspection. */
  public val untransformed: Action
    get() =
        when (this) {
          is Action -> this
          is Transform -> inner.untransformed
        }

  override val kind: kotlin.reflect.KClass<out PetNode> = ActionTree::class

  /** A whole action marked for the named transform. */
  public data class Transform(val inner: ActionTree, override val transformKind: String) :
      ActionTree(), TransformNode<ActionTree> {
    override fun extract(): ActionTree = inner

    override fun visitChildren(visitor: Visitor): Unit = visitor.visit(inner)

    override fun toString(): String = "$transformKind[$inner]"
  }
}
