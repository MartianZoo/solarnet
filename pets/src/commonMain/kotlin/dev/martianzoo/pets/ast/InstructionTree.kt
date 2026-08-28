package dev.martianzoo.pets.ast

import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.pets.Specification
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.TypeInfo
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.Instruction.Or

/**
 * A complete Pets instruction syntax tree. Most engine code should use [Instruction] when it needs
 * one task or [InstructionGroup] when it needs independent work; this broader type is for Pets
 * composition and the transitions that deliberately convert between those forms.
 */
public sealed class InstructionTree : PetElement(), Specification<InstructionTree> {
  /** Returns a tree that does this tree [factor] times. */
  public abstract operator fun times(factor: Int): InstructionTree

  /** Whether this tree still requires a gameplay choice or other narrowing. */
  public abstract override fun isAbstract(info: TypeInfo): Boolean

  /** Ensures that this tree is a valid narrowing of [that]. */
  public override fun ensureNarrows(that: InstructionTree, info: TypeInfo) {
    if (that !is Or && this != NoOp && this::class != that::class) {
      throw NarrowingException("`$this` can't narrow `$that` (different types)")
    }
    try {
      that.ensureIsNarrowedBy(this, info)
    } catch (e: NarrowingException) {
      throw NarrowingException("$this does not narrow $that", e)
    }
  }

  protected abstract fun ensureIsNarrowedBy(proposed: InstructionTree, info: TypeInfo)

  override val kind: kotlin.reflect.KClass<out PetNode> = InstructionTree::class

  internal companion object {
    internal fun parser(): Parser<InstructionTree> = Instruction.treeParser()
  }
}
