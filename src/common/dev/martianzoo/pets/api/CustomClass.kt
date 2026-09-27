package dev.martianzoo.pets.api

import dev.martianzoo.pets.HasClassName
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.types.Type

/**
 * Implementation for a "custom class" (of the form `CLASS Foo : Custom`). By default its Pets class
 * name is the implementation's Kotlin simple name. Instruction translations may return one
 * instruction, a group of independent instructions, or a no-op. An unimplemented translation arity
 * rejects the request with [ExpressionException]; a metric-only implementation need not translate.
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
   * For a type with 0 dependencies: translates an instruction to gain this type into another
   * instruction tree that will be resolved and executed instead.
   */
  public open fun translate(game: GameReader): InstructionTree = noInstructionBehavior(0)

  /**
   * For a type with 1 dependency: translates an instruction to gain this type into another
   * instruction tree that will be resolved and executed instead.
   */
  public open fun translate(game: GameReader, type0: Type): InstructionTree =
      noInstructionBehavior(1)

  /**
   * For a type with 2 dependencies: translates an instruction to gain this type into another
   * instruction tree that will be resolved and executed instead.
   */
  public open fun translate(game: GameReader, type0: Type, type1: Type): InstructionTree =
      noInstructionBehavior(2)

  /**
   * For a type with 3 dependencies: translates an instruction to gain this type into another
   * instruction tree that will be resolved and executed instead.
   */
  public open fun translate(
      game: GameReader,
      type0: Type,
      type1: Type,
      type2: Type,
  ): InstructionTree = noInstructionBehavior(3)

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
  ): InstructionTree = noInstructionBehavior(4)

  private fun noInstructionBehavior(dependencies: Int): Nothing =
      throw ExpressionException(
          "custom class `$className` has no instruction implementation for $dependencies " +
              if (dependencies == 1) "dependency" else "dependencies"
      )
}
