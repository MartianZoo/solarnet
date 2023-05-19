package dev.martianzoo.tfm.api

import dev.martianzoo.tfm.pets.HasClassName
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Instruction

/**
 * TODO
 */
public abstract class CustomClass(
  override val className: ClassName,
) : HasClassName {
  constructor(name: String) : this(cn(name))

  /** Returns the instructions to execute in place of this custom instructions. */
  // TODO pass just the type itself, or split args
  abstract fun translate(game: GameReader, arguments: List<Type>): Instruction
}
