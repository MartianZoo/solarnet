package dev.martianzoo.tfm.api

import dev.martianzoo.tfm.pets.ast.Instruction

/**
 * For instructions that can't be expressed in Pets, write `@functionName(Arg1, Arg2...)` and
 * implement this interface. Any [Authority] providing a class declaration that uses that syntax
 * will need to also return this instance from [Authority.customInstruction].
 */
public abstract class CustomInstruction(
    /**
     * The name, in lowerCamelCase, that this function will be addressable by (preceded by `@`) in
     * Pets code. [Authority.customInstruction] must return this instance when passed
     * [functionName].
     */
    val functionName: String,
) {

  /** Returns the instructions to execute in place of this custom instructions. */
  abstract fun translate(game: GameReader, arguments: List<Type>): Instruction
}
