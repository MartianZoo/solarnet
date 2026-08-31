package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger.ByTrigger
import dev.martianzoo.pets.ast.Effect.Trigger.IfTrigger
import dev.martianzoo.pets.ast.Effect.Trigger.Or
import dev.martianzoo.tfm.pets.testSampleStrings
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.test.assertFailsWith

// Most testing is done by AutomatedTest
internal class EffectTest {

  private val inputs =
      """
      Bar: 11 MC
      This: Ok
      -Foo: -5X MC
      X This: Ok
      -X This: 5 MC!
      This: 5 Abc?
      PROD[Qux]: MC?
      This: MC, -!Foo
      X Eep<Bar>: 5X MC?
      PROD[X This]: Ok
      PROD[X Xyz]:: -2 MC.
      -Xyz: 2 MC / Xyz OR MC
      PROD[Bar]: Ahh, Ahh
      -This: MC / PROD[Qux]
      This: (MC: Abc) BY Bar
      PROD[Qux]: -Xyz, 2 Bar
      -X Foo(HAS Foo): -X Eep
      This BY Ooh: Foo!, 2 Xyz
      X This: PROD[2 Bar], -Abc
      !Ooh<Ahh>: 2X Xyz FROM Qux
      -Eep: Ok THEN Ok, PROD[Foo]
      X This:: 2X Ahh, -Qux(HAS MC)
      PROD[Wau<Bar, Ooh>]: PROD[MC?]
      X Abc: Foo, (Ok BY Qux) BY Bar
      X Qux<Bar>: Bar / 2 Bar<Bar>, MC
      Foo BY Player2 IF =1 Xyz: -X Ahh
      PROD[X Ahh]: Bar<Ooh> / Xyz, -Qux
      -X This: -Foo! OR X Qux, Ahh, Bar?
      This OR PROD[X This]: Bar FROM !Ooh
      -Bar: 11 Ahh<Foo, Foo> FROM Eep<Qux>
      Bar<Foo<Bar<Bar<Bar>>, Eep>, Bar>: -MC
      X This: Abc / 2 Bar<Bar, Ooh> - Qux
      PROD[Abc]: -X Qux, 11 Abc<Xyz> FROM Xyz
      This OR This: 2 Xyz<Ooh<Foo>(HAS 5 Xyz)>
      PROD[-Ooh<Qux<Bar>, Bar>]: -MC / PROD[Foo]
      (This BY Abc) BY Xyz: !Foo<Foo>, MC!, MC: X MC.
      PROD[Foo<Qux>]: 5X MC, Ahh<Qux> / Xyz, PROD[MC]
      !Foo: -MC / Foo, Ooh FROM Bar, 5 MC!, Abc OR Qux
      PROD[-Xyz] BY Foo:: MC / Foo MAX 5, Foo(HAS MC)
      -This: X MC!, Xyz(HAS Foo) FROM Foo, -5 Foo / Qux
      Xyz IF MAX 0 MC: Xyz<Abc, Foo> FROM Ahh
      PROD[X Bar]: PROD[MC / Foo, Qux], Xyz, Qux, 2 Abc
      PROD[X This]: 5X Eep<Bar<Abc>>, 2 MC, Abc<Ooh, Qux>!
      Ahh<Abc, Xyz>: -Eep(HAS Bar) OR (Bar: -Xyz BY Bar)
      X Qux:: ((MC OR Bar): Abc) OR Ok OR -5 Qux, PROD[Ok]
      Qux(HAS MAX 1 MC): (-5 MC, Qux FROM Qux) OR Xyz
      Ahh IF MAX 1 Foo: PROD[2 Bar, MC / Bar, Bar<Foo>: Xyz]
      PROD[Foo] OR PROD[Bar]:: -5X MC, 2 MC THEN Foo<Qux> FROM Foo
      PROD[Eep]:: -5 MC, -2 Ooh<Abc>, (Foo: 2 MC) OR (Qux FROM Foo)
      -Foo<!Qux>: -X Foo<!Qux<Bar<Foo>, Abc>> / 2 (2 Foo<Abc>)
      Bar IF MAX 2 Bar: X Abc / Qux<Eep> OR PROD[MC] BY Bar<Xyz>
      PROD[Foo]:: (2 Qux FROM Foo) OR (-Foo, MC), 2 Qux FROM Ahh.
      PROD[X This]: MC: MC, Abc / Bar<Bar<Bar>> OR 2 Foo., -2X Qux.
      Ahh<Foo>: (Qux<Qux, Foo>, MC / 2 Bar OR 2 MC) OR Abc / PROD[Abc]
      """
          .trimIndent()

  @Test
  internal fun testSampleStrings() {
    testSampleStrings<Effect>(inputs)
  }

  @Test
  internal fun nodeCount() {
    val effect: Effect = parse("Xyz<Xyz>: PROD[(1 Abc FROM Qux) OR MC]")
    effect.descendantCount() shouldBe 20
  }

  @Test
  internal fun classTypesCannotBeTriggers() {
    assertFailsWith<PetSyntaxException> { parse<Effect>("Class<Foo>: Bar") }
    assertFailsWith<PetSyntaxException> { parse<Effect>("-Class<Foo>: Bar") }
    assertFailsWith<PetSyntaxException> { parse<Effect>("PROD[Class<Foo>]: Bar") }

    parse<Effect>("PlayCard<Class<Foo>>: Bar").toString() shouldBe "PlayCard<Class<Foo>>: Bar"
  }

  @Test
  internal fun componentTriggersRequireAQualifier() {
    listOf("Component: Bar", "-Component: Bar", "Foo OR Component: Bar").forEach {
      assertFailsWith<PetSyntaxException>(it) { parse<Effect>(it) }
    }

    parse<Effect>("Component IF Foo: Bar").toString() shouldBe "Component IF Foo: Bar"
    parse<Effect>("Component BY Anyone: Bar").toString() shouldBe "Component BY Anyone: Bar"
    parse<Effect>("Owned<Player>: Bar").toString() shouldBe "Owned<Player>: Bar"
  }

  @Test
  internal fun bySelectorsAreExpressions() {
    parse<Effect>("Foo BY !Owner: Bar").toString() shouldBe "Foo BY !Owner: Bar"
    parse<Effect>("Foo BY !Player2: Bar").toString() shouldBe "Foo BY !Player2: Bar"
  }

  @Test
  internal fun triggerAlternativesNeedNoParentheses() {
    parse<Effect>("Foo OR -Bar BY Anyone:: Qux").toString() shouldBe "Foo OR -Bar BY Anyone:: Qux"
  }

  @Test
  internal fun orBindsMoreTightlyThanByAndIf() {
    val trigger = parse<Effect>("Foo OR -Bar BY Anyone IF Qux: Eep").trigger as IfTrigger

    ((trigger.inner as ByTrigger).inner is Or) shouldBe true
    trigger.toString() shouldBe "Foo OR -Bar BY Anyone IF Qux"
  }

  @Test
  internal fun groupingAllowsBranchSpecificQualifiers() {
    parse<Effect>("(Foo BY Player IF Qux) OR (-Bar BY Anyone IF Abc): Eep").toString() shouldBe
        "(Foo BY Player IF Qux) OR (-Bar BY Anyone IF Abc): Eep"
  }

  @Test
  internal fun groupingAllowsQualifiersAtDifferentLevels() {
    parse<Effect>("(Foo IF Qux) OR Bar BY Anyone IF Abc: Eep").toString() shouldBe
        "(Foo IF Qux) OR Bar BY Anyone IF Abc: Eep"
  }

  @Test
  internal fun triggerAlternativesCannotMixSelfAndSubscriptions() {
    assertFailsWith<PetSyntaxException> { parse<Effect>("This OR Foo: Qux") }
  }

  @Test
  internal fun conditionalTriggerRequirementsNeedNoParentheses() {
    parse<Effect>("This IF =3 This OR =5 This: PROD[Heat]").toString() shouldBe
        "This IF =3 This OR =5 This: PROD[Heat]"
  }
}
