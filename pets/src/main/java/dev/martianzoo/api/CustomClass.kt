package dev.martianzoo.api

import dev.martianzoo.api.Exceptions.AbstractException
import dev.martianzoo.api.Exceptions.DependencyException
import dev.martianzoo.pets.HasClassName
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Instruction

/** Implementation for a "custom class" (of the form `CLASS Foo : Custom`). */
public abstract class CustomClass(override val className: ClassName) : HasClassName {
  constructor(name: String) : this(cn(name))

  /**
   * For a type with 0 dependencies: translates an instruction to gain this type into another
   * instruction that wil be prepared and executed instead.
   */
  open fun translate(game: GameReader): Instruction = throw NotImplementedError()

  /**
   * For a type with 1 dependency: translates an instruction to gain this type into another
   * instruction that wil be prepared and executed instead.
   */
  open fun translate(game: GameReader, type0: Type): Instruction = throw NotImplementedError()

  /**
   * For a type with 2 dependencies: translates an instruction to gain this type into another
   * instruction that wil be prepared and executed instead.
   */
  open fun translate(game: GameReader, type0: Type, type1: Type): Instruction =
      throw NotImplementedError()

  /**
   * For a type with 3 dependencies: translates an instruction to gain this type into another
   * instruction that wil be prepared and executed instead.
   */
  open fun translate(game: GameReader, type0: Type, type1: Type, type2: Type): Instruction =
      throw NotImplementedError()

  /**
   * For a type with 4 dependencies: translates an instruction to gain this type into another
   * instruction that wil be prepared and executed instead.
   */
  open fun translate(
      game: GameReader,
      type0: Type,
      type1: Type,
      type2: Type,
      type3: Type,
  ): Instruction {
    error("")
  }

  fun prepare(game: GameReader, type: Type): Instruction {
    if (type.abstract) throw AbstractException("")
    val args = type.expressionFull.arguments.map { game.resolve(it) }
    val missing = args.filter { game.countComponent(it) == 0 }
    if (missing.any()) throw DependencyException(missing)

    return try {
      when (args.size) {
        0 -> translate(game)
        1 -> translate(game, args[0])
        2 -> translate(game, args[0], args[1])
        3 -> translate(game, args[0], args[1], args[2])
        4 -> translate(game, args[0], args[1], args[2], args[3])
        else -> error("what kind of type has 5 deps?")
      }
    } catch (e: NotImplementedError) {
      error(
          "type ${type.expressionFull} has ${args.size} dependencies, " +
              "but the appropriate translate() overload was not overridden")
    }
  }
}
