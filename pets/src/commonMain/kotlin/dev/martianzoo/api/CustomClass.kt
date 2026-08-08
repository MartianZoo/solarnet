package dev.martianzoo.api

import dev.martianzoo.pets.HasClassName
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.types.Type

/**
 * Implementation for a "custom class" (of the form `CLASS Foo : Custom`). By default its Pets class
 * name is the implementation's Kotlin simple name.
 */
public abstract class CustomClass(name: String? = null) : HasClassName {
  public constructor(className: ClassName) : this(className.toString())

  final override val className: ClassName by lazy {
    cn(name ?: requireNotNull(this::class.simpleName))
  }

  /**
   * Pets classes this implementation may resolve or produce at runtime. Class loading follows these
   * names when this custom class loads; other references may still load them independently.
   */
  public open val requiredClassNames: Set<ClassName> = emptySet()

  /**
   * For a type with 0 dependencies: translates an instruction to gain this type into another
   * instruction that wil be prepared and executed instead.
   */
  public open fun translate(game: GameReader): Instruction = throw NotImplementedError()

  /**
   * For a type with 1 dependency: translates an instruction to gain this type into another
   * instruction that wil be prepared and executed instead.
   */
  public open fun translate(game: GameReader, type0: Type): Instruction =
      throw NotImplementedError()

  /**
   * For a type with 2 dependencies: translates an instruction to gain this type into another
   * instruction that wil be prepared and executed instead.
   */
  public open fun translate(game: GameReader, type0: Type, type1: Type): Instruction =
      throw NotImplementedError()

  /**
   * For a type with 3 dependencies: translates an instruction to gain this type into another
   * instruction that wil be prepared and executed instead.
   */
  public open fun translate(game: GameReader, type0: Type, type1: Type, type2: Type): Instruction =
      throw NotImplementedError()

  /**
   * For a type with 4 dependencies: translates an instruction to gain this type into another
   * instruction that wil be prepared and executed instead.
   */
  public open fun translate(
      game: GameReader,
      type0: Type,
      type1: Type,
      type2: Type,
      type3: Type,
  ): Instruction = throw NotImplementedError()
}
