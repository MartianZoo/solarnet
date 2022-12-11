package dev.martianzoo.tfm.petaform

import org.junit.jupiter.api.Test

class AutoTest {
  @Test
  fun expressions() {
    val gen = PetaformGenerator()
    gen.printTestStrings<QuantifiedExpression>(10)
    gen.goNuts<QuantifiedExpression>(2000)
  }

  @Test
  fun triggers() {
    val gen = PetaformGenerator(0.6)
    gen.printTestStrings<Effect.Trigger>(10)
    gen.goNuts<Effect.Trigger>(1000)
  }

  @Test
  fun predicates() {
    val gen = PetaformGenerator()
    gen.printTestStrings<Predicate>(10)
    gen.goNuts<Predicate>(5000)
  }

  @Test
  fun instructions() {
    val gen = PetaformGenerator()
    gen.printTestStrings<Instruction>(10)
    gen.goNuts<Instruction>(5000)
  }

  @Test
  fun effects() {
    val gen = PetaformGenerator(0.9)
    gen.printTestStrings<Effect>(10)
    gen.goNuts<Effect>(2000)
  }

  @Test
  fun costs() {
    val gen = PetaformGenerator()
    gen.printTestStrings<Action.Cost>(10)
    gen.goNuts<Action.Cost>(1000)
  }

  @Test
  fun actions() {
    val gen = PetaformGenerator()
    gen.printTestStrings<Action>(10)
    gen.goNuts<Action>(2000)
  }
}
