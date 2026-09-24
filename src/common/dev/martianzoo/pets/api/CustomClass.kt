package dev.martianzoo.pets.api

import dev.martianzoo.pets.HasClassName
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.types.Type

/**
 * Implementation for a "custom class" (of the form `CLASS Foo : Custom`). By default its Pets class
 * name is the implementation's Kotlin simple name. Instruction translations may return one
 * instruction, a group of independent instructions, or a no-op.
 */
public abstract class CustomClass(name: String? = null) : HasClassName {
  public constructor(className: ClassName) : this(className.toString())

  final override val className: ClassName = cn(name ?: requireNotNull(this::class.simpleName))

  /**
   * Pets classes this implementation may resolve or produce at runtime. Class loading follows these
   * names when this custom class loads; other references may still load them independently.
   */
  public open val requiredClassNames: Set<ClassName> = emptySet()

  /**
   * Optionally translates a gain before ordinary Type narrowing. This lets a custom instruction
   * interpret an authored refinement as an instruction argument rather than a live-component
   * predicate. Return null to use the ordinary concrete custom-instruction path.
   */
  public open fun translateGain(game: GameReader, gain: Gain): InstructionTree? = null

  /**
   * For a type with 0 dependencies: translates an instruction to gain this type into another
   * instruction tree that will be resolved and executed instead.
   */
  public open fun translate(game: GameReader): InstructionTree =
      throw NotImplementedError("`$className` does not implement translation with 0 dependencies")

  /**
   * For a type with 1 dependency: translates an instruction to gain this type into another
   * instruction tree that will be resolved and executed instead.
   */
  public open fun translate(game: GameReader, type0: Type): InstructionTree =
      throw NotImplementedError("`$className` does not implement translation with 1 dependency")

  /**
   * For a type with 2 dependencies: translates an instruction to gain this type into another
   * instruction tree that will be resolved and executed instead.
   */
  public open fun translate(game: GameReader, type0: Type, type1: Type): InstructionTree =
      throw NotImplementedError("`$className` does not implement translation with 2 dependencies")

  /**
   * For a type with 3 dependencies: translates an instruction to gain this type into another
   * instruction tree that will be resolved and executed instead.
   */
  public open fun translate(
      game: GameReader,
      type0: Type,
      type1: Type,
      type2: Type,
  ): InstructionTree =
      throw NotImplementedError("`$className` does not implement translation with 3 dependencies")

  /**
   * For a type with 4 dependencies: translates an instruction to gain this type into another
   * instruction tree that will be resolved and executed instead.
   */
  public open fun translate(
      game: GameReader,
      type0: Type,
      type1: Type,
      type2: Type,
      type3: Type,
  ): InstructionTree =
      throw NotImplementedError("`$className` does not implement translation with 4 dependencies")
}
