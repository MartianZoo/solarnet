package dev.martianzoo.pets

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger
import dev.martianzoo.pets.ast.Effect.Trigger.ByTrigger
import dev.martianzoo.pets.ast.Effect.Trigger.IfTrigger
import dev.martianzoo.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.pets.ast.Effect.Trigger.OnRemoveOf
import dev.martianzoo.pets.ast.Effect.Trigger.WhenGain
import dev.martianzoo.pets.ast.Effect.Trigger.WhenRemove
import dev.martianzoo.pets.ast.Effect.Trigger.XTrigger
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.InstructionTree
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Section 8 of `docs/pets-language-spec.md`: effects as rules attached to a class. */
internal class Lang08EffectsTest {

  // L8-1 The shape of an effect

  @Test
  internal fun `L8-1 an effect is a trigger, a colon and an instruction`() {
    val effect = parse<Effect>("CityTile: 2 MC")

    effect.trigger shouldBe OnGainOf.create(parse<Expression>("CityTile"))
    effect.instruction shouldBe parse<InstructionTree>("2 MC")
  }

  // L8-2 Automatic effects

  @Test
  internal fun `L8-2 a double colon marks an automatic effect`() {
    parse<Effect>("CityTile:: 2 MC").automatic shouldBe true
    parse<Effect>("CityTile: 2 MC").automatic shouldBe false
    parse<Effect>("CityTile:: 2 MC").toString() shouldBe "CityTile:: 2 MC"
  }

  // L8-3 The two kinds of trigger

  @Test
  internal fun `L8-3 self triggers and subscriptions`() {
    parse<Effect>("This: Plant").trigger shouldBe WhenGain
    parse<Effect>("-This: Plant").trigger shouldBe WhenRemove
    parse<Effect>("CityTile: Plant").trigger shouldBe OnGainOf.create(parse("CityTile"))
    parse<Effect>("-CityTile: Plant").trigger shouldBe OnRemoveOf.create(parse("CityTile"))

    (OnGainOf.create(parse<Expression>("CityTile")) is Trigger.SubscribedTrigger) shouldBe true
  }

  // L8-4 A self trigger is not a subscription

  @Test
  internal fun `L8-4 This as an expression is still the self trigger`() {
    // Writing the context expression as a subscription target collapses to the self trigger, so
    // there is no way to spell "subscribe to my own type" by accident.
    OnGainOf.create(parse<Expression>("This")) shouldBe WhenGain
    OnRemoveOf.create(parse<Expression>("This")) shouldBe WhenRemove
  }

  @Test
  internal fun `L8-4 an empty argument list does not make This a subscription`() {
    // `This<>` is the same placeholder as `This` (L3-5), even though the two are different
    // expressions, so it is classified structurally rather than by expression equality.
    parse<Effect>("This<>: Plant").trigger shouldBe WhenGain
    parse<Effect>("-This<>: Plant").trigger shouldBe WhenRemove
    parse<Effect>("This<Player1>: Plant").trigger shouldBe
        OnGainOf.create(parse<Expression>("This<Player1>"))
  }

  // L8-5 X triggers

  @Test
  internal fun `L8-5 X before a trigger's expression binds the size of the change`() {
    val gained = parse<Effect>("X Plant: X Heat").trigger as XTrigger
    gained.inner shouldBe OnGainOf.create(parse<Expression>("Plant"))
    gained.toString() shouldBe "X Plant"

    parse<Effect>("-X Plant: X Heat").trigger.toString() shouldBe "-X Plant"
    parse<Effect>("X This: Ok").trigger.toString() shouldBe "X This"
    parse<Effect>("-X This: 5 MC!").trigger.toString() shouldBe "-X This"
  }

  // L8-6 OR, and no mixing

  @Test
  internal fun `L8-6 OR joins triggers, and self and subscribed triggers may not mix`() {
    parse<Effect>("Plant OR -Heat: Steel").trigger.toString() shouldBe "Plant OR -Heat"
    parse<Effect>("This OR -This: Steel").trigger.toString() shouldBe "This OR -This"
    shouldThrow<PetSyntaxException> { parse<Effect>("This OR Plant: Steel") }
  }

  // L8-7 BY and IF

  @Test
  internal fun `L8-7 OR binds tighter than BY, which binds tighter than IF`() {
    val trigger = parse<Effect>("Plant OR -Heat BY Anyone IF Steel: Ore").trigger as IfTrigger

    ((trigger.inner as ByTrigger).inner is Trigger.Or) shouldBe true
    trigger.toString() shouldBe "Plant OR -Heat BY Anyone IF Steel"
    parse<Effect>("Plant OR -Heat BY Anyone:: Steel").toString() shouldBe
        "Plant OR -Heat BY Anyone:: Steel"
  }

  @Test
  internal fun `L8-7 parentheses give one alternative its own qualifier`() {
    roundTrip<Effect>("(Plant BY Player1 IF Steel) OR (-Heat BY Anyone IF Ore): Eep")
    roundTrip<Effect>("(Plant IF Steel) OR Heat BY Anyone IF Ore: Eep")
  }

  @Test
  internal fun `L8-7 a BY selector is an expression`() {
    roundTrip<Effect>("Plant BY Player(NOT Owner): Heat")
    roundTrip<Effect>("Plant BY Actor(NOT Player2): Heat")
    roundTrip<Effect>("Plant IF =3 This OR =5 This: PROD[Heat]")
  }

  // L8-8 Class literals are not triggers

  @Test
  internal fun `L8-8 a class literal may not be a trigger`() {
    shouldThrow<PetSyntaxException> { parse<Effect>("Class<Plant>: Heat") }
    shouldThrow<PetSyntaxException> { parse<Effect>("-Class<Plant>: Heat") }
    shouldThrow<PetSyntaxException> { parse<Effect>("PROD[Class<Plant>]: Heat") }
    roundTrip<Effect>("PlayCard<Class<Plant>>: Heat")
  }

  // L8-9 A bare Component subscription

  @Test
  internal fun `L8-9 a bare Component subscription must be qualified`() {
    listOf("Component: Heat", "-Component: Heat", "Plant OR Component: Heat").forEach {
      shouldThrow<PetSyntaxException> { parse<Effect>(it) }
    }

    roundTrip<Effect>("Component IF Plant: Heat")
    roundTrip<Effect>("Component BY Anyone: Heat")
    roundTrip<Effect>("Owned<Player>: Heat")
  }

  // L8-10 Rendering

  @Test
  internal fun `L8-10 a gated instruction is parenthesized after the colon`() {
    roundTrip<Effect>("Plant: (Heat: Steel)")
    parse<Effect>("Plant: (Heat: Steel)").instruction shouldBe parse<InstructionTree>("Heat: Steel")
    roundTrip<Effect>("Plant IF Heat, Steel: Ore", "Plant IF (Heat, Steel): Ore")
  }

  @Test
  internal fun `L8-10 effects round-trip`() {
    roundTripAll<Effect>(
        """
        This: Ok
        Bar: 11 MC
        X This: Ok
        -Foo: -5X MC
        Plant: 11 MC
        This: 5 Abc?
        -Plant: -5X MC
        -X This: 5 MC!
        PROD[Qux]: MC?
        This: 5 Steel?
        PROD[Plant]: MC?
        PROD[X This]: Ok
        X Eep<Bar>: 5X MC?
        PROD[Bar]: Ahh, Ahh
        PROD[X Xyz]:: -2 MC.
        X Ore<Plant>: 5X MC?
        -This: MC / PROD[Qux]
        -Xyz: 2 MC / Xyz OR MC
        PROD[Qux]: -Xyz, 2 Bar
        PROD[X Steel]:: -2 MC.
        This: (MC: Abc) BY Bar
        -This: MC / PROD[Plant]
        -X Foo(HAS Foo): -X Eep
        This: MC, -Bar(NOT Foo)
        This BY Ooh: Foo!, 2 Xyz
        This: (MC: Heat) BY Plant
        X This: PROD[2 Bar], -Abc
        -Steel: 2 MC / Steel OR MC
        This: MC, -Heat(NOT Plant)
        -Eep: Ok THEN Ok, PROD[Foo]
        This BY Ore: Plant!, 2 Steel
        X This:: 2X Ahh, -Qux(HAS MC)
        PROD[Wau<Bar, Ooh>]: PROD[MC?]
        X Abc: Foo, (Ok BY Qux) BY Bar
        X This:: 2X Eep, -Heat(HAS MC)
        Foo BY Player2 IF =1 Xyz: -X Ahh
        X Qux<Bar>: Bar / 2 Bar<Bar>, MC
        PROD[X Ahh]: Bar<Ooh> / Xyz, -Qux
        -X This: -Foo! OR X Qux, Ahh, Bar?
        Foo(NOT Ooh<Ahh>): 2X Xyz FROM Qux
        Plant: EACH Player { Heat<Player> }
        X This: Abc / 2 Bar<Bar, Ooh> - Qux
        -Bar: 11 Ahh<Foo, Foo> FROM Eep<Qux>
        Plant BY Player2 IF =1 Steel: -X Eep
        Bar<Foo<Bar<Bar<Bar>>, Eep>, Bar>: -MC
        PROD[Abc]: -X Qux, 11 Abc<Xyz> FROM Xyz
        Plant(NOT Ore<Eep>): 2X Steel FROM Heat
        Xyz IF MAX 0 MC: Xyz<Abc, Foo> FROM Ahh
        This OR This: 2 Xyz<Ooh<Foo>(HAS 5 Xyz)>
        PROD[-Ooh<Qux<Bar>, Bar>]: -MC / PROD[Foo]
        This OR PROD[X This]: Bar FROM Foo(NOT Ooh)
        This OR PROD[X This]: Heat FROM Plant(NOT Ore)
        PROD[-Xyz] BY Foo:: MC / Foo MAX 5, Foo(HAS MC)
        PROD[Foo<Qux>]: 5X MC, Ahh<Qux> / Xyz, PROD[MC]
        Qux(HAS MAX 1 MC): (-5 MC, Qux FROM Qux) OR Xyz
        -This: X MC!, Xyz(HAS Foo) FROM Foo, -5 Foo / Qux
        PROD[X Bar]: PROD[MC / Foo, Qux], Xyz, Qux, 2 Abc
        Ahh<Abc, Xyz>: -Eep(HAS Bar) OR (Bar: -Xyz BY Bar)
        PROD[X This]: 5X Eep<Bar<Abc>>, 2 MC, Abc<Ooh, Qux>!
        X Qux:: ((MC OR Bar): Abc) OR Ok OR -5 Qux, PROD[Ok]
        Ahh IF MAX 1 Foo: PROD[2 Bar, MC / Bar, Bar<Foo>: Xyz]
        (This BY Abc) BY Xyz: Bar(NOT Foo<Foo>), MC!, MC: X MC.
        PROD[-Steel] BY Plant:: MC / Plant MAX 5, Plant(HAS MC)
        Bar(NOT Foo): -MC / Foo, Ooh FROM Bar, 5 MC!, Abc OR Qux
        Bar IF MAX 2 Bar: X Abc / Qux<Eep> OR PROD[MC] BY Bar<Xyz>
        PROD[Foo]:: (2 Qux FROM Foo) OR (-Foo, MC), 2 Qux FROM Ahh.
        PROD[Foo] OR PROD[Bar]:: -5X MC, 2 MC THEN Foo<Qux> FROM Foo
        PROD[Eep]:: -5 MC, -2 Ooh<Abc>, (Foo: 2 MC) OR (Qux FROM Foo)
        PROD[X This]: MC: MC, Abc / Bar<Bar<Bar>> OR 2 Foo., -2X Qux.
        (This BY Heat) BY Steel: Plant(NOT Plant<Plant>), MC!, MC: X MC.
        Ahh<Foo>: (Qux<Qux, Foo>, MC / 2 Bar OR 2 MC) OR Abc / PROD[Abc]
        -Foo<Bar(NOT Qux)>: -X Foo<Bar(NOT Qux<Bar<Foo>, Abc>)> / 2 (2 Foo<Abc>)
        """
    )
  }

  @Test
  internal fun `L8-10 an effect's descendant count is its whole subtree`() {
    parse<Effect>("Steel<Steel>: PROD[(1 Heat FROM Plant) OR MC]").descendantCount() shouldBe 20
  }
}
