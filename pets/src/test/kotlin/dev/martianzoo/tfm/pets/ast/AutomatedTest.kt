package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.tfm.pets.ast.Effect.Trigger
import dev.martianzoo.tfm.testlib.PetGenerator
import org.junit.jupiter.api.Test

const val FACTOR = 10

private class AutomatedTest {

  @Test
  fun scalarAndTypes() {
    val gen = PetGenerator()
    gen.goNuts<ScalarAndType>(200 * FACTOR)
  }

  @Test
  fun triggers() {
    val gen = PetGenerator(0.6)
    gen.goNuts<Trigger>(100 * FACTOR)
  }

  @Test
  fun requirements() {
    val gen = PetGenerator()
    gen.goNuts<Requirement>(500 * FACTOR)
    // gen.printTestStringOfEachLength<Requirement>(60)
  }

  @Test
  fun instructions() {
    val gen = PetGenerator()
    gen.goNuts<Instruction>(200 * FACTOR)
    // gen.printTestStringOfEachLength<Instruction>(60)
  }

  @Test
  fun effects() {
    val gen = PetGenerator(0.9)
    gen.goNuts<Effect>(200 * FACTOR)
    // gen.printTestStringOfEachLength<Effect>(60)
  }

  @Test
  fun costs() {
    val gen = PetGenerator()
    gen.goNuts<Action.Cost>(100 * FACTOR)
  }

  @Test
  fun actions() {
    val gen = PetGenerator()
    gen.goNuts<Action>(200 * FACTOR)
    // gen.printTestStringOfEachLength<Action>(60)
  }
}
