package dev.martianzoo.pets.ast

/**
 * A subtree marked for rewriting by a named handler, the common example being `PROD[...]`, as
 * defined by
 * [section 8](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#8-transform-blocks).
 * [Rule
 * L8-1](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#8-transform-blocks)
 * admits a block on an instruction, an action cost, a metric, a requirement, a trigger, a whole
 * effect, and a whole action; each of those (e.g. [Instruction.Transform]) implements this
 * interface.
 *
 * A block whose kind has no handler is preserved verbatim, so a source may carry marks a later
 * stage will interpret ([rule
 * L8-2](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#8-transform-blocks)).
 */
public interface TransformNode<P : PetNode> {
  /**
   * The all-caps word identifying this kind of transform, e.g. `"PROD"` ([rule
   * L10-4](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#10-names)).
   */
  public val transformKind: String

  /** The node this transform node is wrapping. */
  public fun extract(): P
}
