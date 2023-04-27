package dev.martianzoo.tfm.pets.ast

/**
 * A "major" kind of Pets node, like an [Instruction], but not an ancillary type like
 * [FromExpression], [ScaledExpression], or [ClassName].
 */
sealed class PetElement : PetNode()
