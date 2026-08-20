package dev.martianzoo.pets.ast

import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.api.TypeInfo
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.Instruction.Or

/**
 * A complete Pets instruction syntax tree. Most engine code should use [Instruction] when it needs
 * one task or [InstructionGroup] when it needs independent work; this broader type is for Pets
 * composition and the boundaries that deliberately convert between those forms.
 */
public sealed class InstructionTree : PetElement() {
  /** Returns a tree that does this tree [factor] times. */
  public abstract operator fun times(factor: Int): InstructionTree

  /** Whether this tree still requires a gameplay choice or other narrowing. */
  public abstract fun isAbstract(info: TypeInfo): Boolean

  @Suppress("TooGenericExceptionCaught") // TODO
  public fun narrows(abstractTree: InstructionTree, info: TypeInfo): Boolean =
      try {
        ensureNarrows(abstractTree, info)
        true
      } catch (_: Exception) {
        false
      }

  /** Ensures that this tree is a valid narrowing of [abstractTree]. */
  public fun ensureNarrows(abstractTree: InstructionTree, info: TypeInfo) {
    if (abstractTree !is Or && this != NoOp && this::class != abstractTree::class) {
      throw NarrowingException("`$this` can't reify `$abstractTree` (different types)")
    }
    try {
      abstractTree.ensureIsNarrowedBy(this, info)
    } catch (e: NarrowingException) {
      throw NarrowingException("$this does not narrow $abstractTree", e)
    }
  }

  protected abstract fun ensureIsNarrowedBy(proposed: InstructionTree, info: TypeInfo)

  override val kind: kotlin.reflect.KClass<out PetNode> = InstructionTree::class

  internal companion object {
    internal fun parser(): Parser<InstructionTree> = Instruction.treeParser()
  }
}
