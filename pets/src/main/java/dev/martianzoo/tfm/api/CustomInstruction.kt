package dev.martianzoo.tfm.api

import dev.martianzoo.tfm.pets.ast.Instruction

/**
 * For instructions that can't be expressed in Pets, write `@functionName(Arg1, Arg2...)` and
 * implement this interface. Any [Authority] providing a class declaration that uses that syntax
 * will need to also return this instance from [Authority.customInstruction].
 *
 * Only one of [translate] or [execute] need be overridden.
 */
public abstract class CustomInstruction(
    /**
     * The name, in lowerCamelCase, that this function will be addressable by (preceded by `@`) in
     * Pets code. [Authority.customInstruction] must return this instance when passed
     * [functionName].
     */
    val functionName: String
) {

  /**
   * When possible override this method, and compute an [Instruction] that can be executed in place
   * of this one. When this isn't possible, override [execute] instead.
   */
  public open fun translate(game: ReadOnlyGameState, arguments: List<Type>): Instruction {
    throw ExecuteInsteadException()
  }

  public open fun execute(game: GameState, arguments: List<Type>) {
    throw NotImplementedError()
  }

  /** For use by the engine, not custom implementations. */
  public class ExecuteInsteadException : RuntimeException("")
}
