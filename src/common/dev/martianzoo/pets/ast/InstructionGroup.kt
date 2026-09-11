package dev.martianzoo.pets.ast

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.api.TypeInfo
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.XScalar

/**
 * A normalized group of independent instructions, used for both Pets commas and task batches. The
 * comma has the lowest precedence of all instruction operators, and its result is a group rather
 * than one instruction: nothing in this language relates the members to each other, which is
 * exactly what makes them independent ([rule
 * L6-8](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#6-instructions)).
 *
 * Groups flatten, `Ok` vanishes from one rather than appearing as an empty member, and empty and
 * singleton batches render as their canonical Pets trees ([NoOp] and the sole instruction); their
 * wrapper identity is not part of the source representation. Groups narrow elementwise: members are
 * matched by position and the sizes must agree ([rule
 * L7-10](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#7-narrowing-what-remains-open)).
 * Because nothing connects the members, two of them may not share an `X` ([rule
 * L6-14](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#6-instructions)).
 */
public data class InstructionGroup(val instructions: List<Instruction>) : InstructionTree() {
  /** How many independent instructions this group holds. */
  public val size: Int
    get() = instructions.size

  /** Whether this group holds no instructions at all, making it equivalent to `Ok`. */
  public fun isEmpty(): Boolean = instructions.isEmpty()

  override fun times(factor: Int): InstructionGroup = of(instructions.map { it * factor })

  override fun isAbstract(info: TypeInfo): Boolean = instructions.any { it.isAbstract(info) }

  override fun visitChildren(visitor: Visitor): Unit = visitor.visit(instructions)

  override fun ensureIsNarrowedBy(proposed: InstructionTree, info: TypeInfo) {
    proposed as? InstructionGroup
        ?: throw NarrowingException("$proposed does not narrow grouped instruction $this")
    if (proposed.instructions.size != instructions.size) {
      throw NarrowingException("$proposed does not narrow grouped instruction $this")
    }
    for ((wide, narrow) in instructions.zip(proposed.instructions)) {
      narrow.ensureNarrows(wide, info)
    }
  }

  override fun precedence(): Int = 0

  override fun toString(): String =
      when (instructions.size) {
        0 -> NoOp.toString()
        1 -> instructions.single().toString()
        else -> instructions.joinToString(", ") { groupPartIfNeeded(it) }
      }

  init {
    if (instructions.any { it == NoOp }) {
      throw PetSyntaxException("Instruction groups cannot contain Ok")
    }
    if (instructions.count { it.descendantsOfType<XScalar>().any() } > 1) {
      throw PetSyntaxException("X cannot link independent instructions")
    }
  }

  public companion object {
    /** Flattens groups and removes no-ops, preserving the remaining instruction order. */
    public fun of(trees: Iterable<InstructionTree>): InstructionGroup =
        InstructionGroup(
            trees.flatMap {
              when (it) {
                is InstructionGroup -> it.instructions
                is NoOp -> emptyList()
                is Instruction -> listOf(it)
              }
            }
        )

    /** Wraps one tree as independent instructions, flattening a group and removing a no-op. */
    public fun of(tree: InstructionTree): InstructionGroup = of(listOf(tree))

    /** Returns canonical Pets syntax, collapsing empty and singleton groups. */
    public fun createTree(trees: Iterable<InstructionTree>): InstructionTree =
        of(trees).let {
          when (it.size) {
            0 -> NoOp
            1 -> it.instructions.single()
            else -> it
          }
        }
  }
}
