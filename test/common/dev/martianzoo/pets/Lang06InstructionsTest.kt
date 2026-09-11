package dev.martianzoo.pets

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.FromExpression.Compact
import dev.martianzoo.pets.ast.FromExpression.Full
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Each
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.Instruction.Gain.Companion.gain
import dev.martianzoo.pets.ast.Instruction.Gated
import dev.martianzoo.pets.ast.Instruction.NoOp
import dev.martianzoo.pets.ast.Instruction.Or
import dev.martianzoo.pets.ast.Instruction.Quantifier.AMAP
import dev.martianzoo.pets.ast.Instruction.Quantifier.MANDATORY
import dev.martianzoo.pets.ast.Instruction.Quantifier.OPTIONAL
import dev.martianzoo.pets.ast.Instruction.Remove
import dev.martianzoo.pets.ast.Instruction.Remove.Companion.remove
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.ast.Instruction.Transmute
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.XScalar
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Section 6 of `docs/pets-language-spec.md`: instructions as relations between two states. */
internal class Lang06InstructionsTest {

  // L6-1 The three elementary instructions

  @Test
  internal fun `L6-1 gain, removal and transmutation`() {
    val gained = parse<Instruction>("4 Plant<Player2>") as Gain
    gained.count shouldBe ActualScalar(4)
    gained.gaining shouldBe parse<Expression>("Plant<Player2>")
    gained.removing shouldBe null

    val removed = parse<Instruction>("-8 Heat<Player1>") as Remove
    removed.count shouldBe ActualScalar(8)
    removed.removing shouldBe parse<Expression>("Heat<Player1>")
    removed.gaining shouldBe null

    val moved = parse<Instruction>("3 Plant<Player4> FROM Plant<Player2>") as Transmute
    moved.count shouldBe ActualScalar(3)
    moved.gaining shouldBe parse<Expression>("Plant<Player4>")
    moved.removing shouldBe parse<Expression>("Plant<Player2>")

    gain(cn("Plant")) shouldBe parse<Instruction>("Plant!")
    remove(cn("Plant"), count = 3, quantifier = AMAP) shouldBe parse<Instruction>("-3 Plant.")
  }

  // L6-2 Counts

  @Test
  internal fun `L6-2 a count that no integer can hold is a syntax error`() {
    shouldThrow<PetSyntaxException> { parse<Instruction>("999999999999999999999999999999 Plant") }
  }

  @Test
  internal fun `L6-2 a count is a positive integer or X, with an optional coefficient`() {
    (parse<Instruction>("Plant") as Gain).count shouldBe ActualScalar(1)
    (parse<Instruction>("11 Plant") as Gain).count shouldBe ActualScalar(11)
    (parse<Instruction>("X Plant") as Gain).count shouldBe XScalar(1)
    (parse<Instruction>("2X Plant") as Gain).count shouldBe XScalar(2)
    shouldThrow<PetSyntaxException> { parse<Instruction>("0 Plant") }
    shouldThrow<PetSyntaxException> { parse<Instruction>("-0 Plant") }
  }

  // L6-3 Quantifiers

  @Test
  internal fun `L6-3 a quantifier says how much of the count must happen`() {
    (parse<Instruction>("2 Plant!") as Gain).quantifier shouldBe MANDATORY
    (parse<Instruction>("2 Plant.") as Gain).quantifier shouldBe AMAP
    (parse<Instruction>("2 Plant?") as Gain).quantifier shouldBe OPTIONAL
    (parse<Instruction>("2 Plant") as Gain).quantifier shouldBe null
    shouldThrow<PetSyntaxException> { parse<Instruction>("2 Plant!?") }
  }

  // L6-4 Ok

  @Test
  internal fun `L6-4 Ok is the instruction that relates a state to itself`() {
    parse<InstructionTree>("Ok") shouldBe NoOp
    NoOp.toString() shouldBe "Ok"
    parse<InstructionTree>("Ok, Plant") shouldBe parse<InstructionTree>("Plant")
    InstructionGroup.of(listOf<InstructionTree>(NoOp)).isEmpty() shouldBe true
    InstructionGroup.createTree(listOf<InstructionTree>(NoOp)) shouldBe NoOp
    shouldThrow<PetSyntaxException> { InstructionGroup(listOf(NoOp)) }
  }

  // L6-5 PER

  @Test
  internal fun `L6-5 a slash scales an elementary change by a metric`() {
    val per = parse<Instruction>("Titanium / 3 EarthTag") as Instruction.Per

    per.inner shouldBe parse<Instruction>("Titanium")
    per.metric shouldBe parse("3 EarthTag")
    shouldThrow<PetSyntaxException> { parse<Instruction>("(Plant, Heat) / Steel") }
    shouldThrow<PetSyntaxException> { parse<Instruction>("(Plant OR Heat) / Steel") }
  }

  // L6-6 Gates

  @Test
  internal fun `L6-6 a gate binds less tightly than OR and does not nest directly`() {
    val gated = parse<Instruction>("3 PlantTag: Plant OR 4 Plant") as Gated

    (gated.inner is Or) shouldBe true
    gated.toString() shouldBe "3 PlantTag: Plant OR 4 Plant"
    parse<Instruction>("3 PlantTag: (Plant OR 4 Plant)").toString() shouldBe
        "3 PlantTag: Plant OR 4 Plant"

    val alternatives = parse<Instruction>("(3 PlantTag: 4 Plant) OR Plant") as Or
    (alternatives.instructions.first() is Gated) shouldBe true
    shouldThrow<PetSyntaxException> { parse<Instruction>("Plant OR 3 PlantTag: 4 Plant") }
    shouldThrow<PetSyntaxException> { parse<Instruction>("Plant: Heat: Steel") }
  }

  // L6-7 OR

  @Test
  internal fun `L6-7 duplicate alternatives are rejected rather than collapsed`() {
    parse<Instruction>("Plant OR Heat").toString() shouldBe "Plant OR Heat"
    shouldThrow<PetSyntaxException> { parse<Instruction>("Plant OR Plant") }
  }

  // L6-8 Groups

  @Test
  internal fun `L6-8 a comma makes a group, which is not one instruction`() {
    parse<InstructionTree>("Plant, Heat") shouldBe
        InstructionGroup(listOf(parse("Plant"), parse("Heat")))
    shouldThrow<PetSyntaxException> { parse<Instruction>("Plant, Heat") }
  }

  @Test
  internal fun `L6-8 groups flatten and a group of one is that one`() {
    parse<InstructionTree>("(Plant)") shouldBe parse<InstructionTree>("Plant")
    InstructionGroup.of(listOf(parse<InstructionTree>("Plant, Heat"), parse("Steel")))
        .instructions shouldBe listOf(parse("Plant"), parse("Heat"), parse("Steel"))
    InstructionGroup.createTree(listOf(parse<InstructionTree>("Plant"))) shouldBe
        parse<InstructionTree>("Plant")
  }

  // L6-9 THEN

  @Test
  internal fun `L6-9 THEN is one right-associative sequence of stages`() {
    val then = parse<Instruction>("Plant THEN Heat THEN Steel") as Then

    then.stages shouldBe listOf(parse<Instruction>("Plant"), parse<Instruction>("Heat"))
    then.continuation shouldBe parse<Instruction>("Steel")
    then.instructions.size shouldBe 3
    parse<Instruction>("Plant THEN (Heat THEN Steel)") shouldBe then
  }

  @Test
  internal fun `L6-9 a stage before the last is one instruction`() {
    shouldThrow<PetSyntaxException> { parse<Instruction>("(Plant THEN Heat) THEN Steel") }
    shouldThrow<PetSyntaxException> { parse<InstructionTree>("(Plant, Heat) THEN Steel") }
    shouldThrow<PetSyntaxException> { parse<Instruction>("((Plant THEN Heat) OR Steel) THEN Ore") }
    parse<InstructionTree>("Plant THEN (Heat, Steel)").toString() shouldBe
        "Plant THEN (Heat, Steel)"
  }

  // L6-10 EACH

  /**
   * This module can pin the fanout's syntax and scope. Enumerating a live world, and rejecting a
   * concrete or unused selector, happen where the fanout is resolved:
   * `test/common/dev/martianzoo/engine/EachSelectorOwnerTest.kt` and
   * `InstructionResolutionTest.kt`.
   */
  @Test
  internal fun `L6-10 EACH names a selector and a body`() {
    val each = parse<Instruction>("EACH Player { 2 Plant, Heat }") as Each

    each.selector shouldBe parse<Expression>("Player")
    each.body shouldBe parse<InstructionTree>("2 Plant, Heat")
    "$each" shouldBe "EACH Player { 2 Plant, Heat }"
  }

  @Test
  internal fun `L6-10 a selector refinement filters without joining the name the body uses`() {
    (parse<Instruction>("EACH ResourceCard(HAS CardResource) { CardResource }") as Each)
        .selectorName shouldBe parse<Expression>("ResourceCard")
    (parse<Instruction>("EACH Player(NOT Player1) { Plant<Player> }") as Each).selectorName shouldBe
        parse<Expression>("Player")
    (parse<Instruction>("EACH Class<Area> { Area }") as Each).representedSelectorName shouldBe
        parse<Expression>("Area")
  }

  @Test
  internal fun `L6-10 a fanout body may evaluate a class property per branch`() {
    roundTrip<Instruction>("EACH Player { Score<Player> / EVAL Goal.metric }")
  }

  @Test
  internal fun `L6-10 a fanout needs a body and does not nest`() {
    shouldThrow<PetSyntaxException> { parse<Instruction>("EACH Player { Ok }") }
    shouldThrow<PetSyntaxException> { parse<Instruction>("EACH Player { EACH Area { Plant } }") }
  }

  // L6-11 BY

  @Test
  internal fun `L6-11 BY names the performer and distributes over a group`() {
    (parse<Instruction>("Plant BY Player1") as Instruction.By).actor shouldBe
        parse<Expression>("Player1")
    parse<InstructionTree>("(Plant, Heat) BY Player1").toString() shouldBe
        "Plant BY Player1, Heat BY Player1"
  }

  // L6-12 Compact transmutation

  @Test
  internal fun `L6-12 a compact transmutation changes exactly one argument`() {
    val compact = parse<Instruction>("Marker<Mars1, Player1 FROM Player2>") as Transmute

    compact.gaining shouldBe parse<Expression>("Marker<Mars1, Player1>")
    compact.removing shouldBe parse<Expression>("Marker<Mars1, Player2>")
    (compact.fromEx is Compact) shouldBe true
    shouldThrow<PetSyntaxException> { parse<Instruction>("Marker<Mars1 FROM Mars2, P1 FROM P2>") }

    parse<Instruction>("Marker<Player1> FROM Marker<Player2>").let {
      ((it as Transmute).fromEx is Full) shouldBe true
    }
    roundTrip<InstructionTree>("Marker<Mars1 FROM Mars2>(HAS Plant)")
  }

  // L6-13 Precedence and rendering

  @Test
  internal fun `L6-13 instructions round-trip`() {
    roundTripAll<InstructionTree>(
        """
        2 MC
        Qux?
        -5 MC
        X Foo
        -X MC?
        Plant?
        X Foo?
        2 Plant
        2X Abc.
        X Plant
        -11X Bar
        -5 Plant
        -X Plant?
        2X Plant.
        -11X Plant
        -Foo<Qux>?
        -MC, X Foo
        X Wau<Qux>?
        5 Foo BY Wau
        Foo<Eep<Qux>>
        -Plant, X Heat
        -Plant<Steel>?
        5 Plant BY Steel
        5 MC, 2 Qux / Bar
        2 Bar THEN MC: Xyz
        Bar / PROD[Foo], MC
        Foo FROM This / This
        Foo / Bar MAX 5 - Qux
        -MC / 2 Foo MAX 5, Bar
        Plant FROM This / This
        5 Foo(NOT Bar) FROM Ahh
        5 Plant, 2 Heat / Steel
        Ahh<Bar, Abc<Bar<Eep>>>
        (Foo: Xyz) OR -Bar / Foo
        2 Heat THEN Plant: Steel
        Abc(HAS 5 Foo(NOT Bar))?
        -11X MC?, PROD[Bar] OR Ok
        Heat / PROD[Plant], Steel
        (MAX 0 Foo, MAX 1 Foo): Ok
        Qux THEN X Qux, MC, 5 Foo.
        Foo(NOT Abc<Foo<Bar<Foo>>>)
        (5 Xyz FROM Bar) BY Ooh<Wau>
        -Steel / 2 Plant MAX 5, Heat
        5 Plant(NOT Steel) FROM Heat
        Ok BY Eep<Foo(NOT Qux<Qux>)>
        Ahh<Abc> THEN Qux, PROD[-Qux]
        EACH Player { 2 Plant, Heat }
        Steel(HAS 5 Plant(NOT Heat))?
        X Bar(NOT Qux<Foo, Ooh<Bar>>)
        Foo, Foo(HAS 2 Bar) / Bar<Foo>
        (MC: MC, -MC!, Xyz, -MC) OR Foo
        (Plant: Steel) OR -Heat / Plant
        X Wau FROM Bar, Foo / PROD[3 Qux]
        Ok BY Steel<Plant(NOT Heat<Heat>)>
        2 Abc(HAS MC) FROM Foo, 5 MC, 5 Bar
        Foo(HAS Abc)!, Xyz OR 2 Abc<Ooh>, MC
        11X Wau<Ahh>(HAS MC OR Abc) FROM Abc.
        2X Qux / 2 Abc OR (-2 Ahh<Foo>., Qux)
        -2 Abc<Xyz<Qux>, Bar<Foo>>., Ok OR Abc
        PROD[Ok OR (MC: Foo)], PROD[2 Ahh / Qux]
        Bar(HAS Abc) / Eep<Abc>, 5 Bar., MC / Bar
        2 Ahh, MAX 0 MC: Qux?, Abc?, Qux<Bar, Qux>
        EACH Player(NOT Player1) { Plant<Player> }
        X Ooh<Qux<Abc<Qux>>>?, -Qux<Xyz<Qux<Abc>>>
        MC, X Foo, Foo? / Ooh, Ok THEN Ok THEN 2 Bar
        (-Foo, Foo, MC) OR Foo BY Qux, PROD[Xyz<Abc>]
        2 Heat(HAS Plant) FROM Steel, 5 Plant, 5 Heat
        Foo(NOT Bar), 5 MC BY Abc<Xyz<Bar<Bar<Qux>>>>
        (MC OR Abc): 2 MC, 11 Qux: MC / Foo<Bar>, -Abc
        -2 Ooh OR Foo / Foo, Xyz, -X Abc, Ooh FROM Bar
        X Ahh FROM Bar<Qux<Xyz(HAS MC OR (MC OR Qux))>>
        Bar<Ooh<Bar>> FROM Qux, Abc FROM Abc / 2 (3 Foo)
        MC, Abc<Foo<Ahh>, Foo<Abc<Bar>, Foo, Foo>, Xyz>.
        MC, 5X Ahh., Bar / PROD[Foo], PROD[Ok BY Foo<Bar>]
        ((Foo OR =1 MC) OR MAX 2 Ooh): (MC, Bar.) OR X Bar.
        MC?, -Bar<Qux> / PROD[Bar], Foo<Ooh<Foo, Bar, Bar>>!
        -5 Foo OR (Eep OR Foo), Bar BY Foo, MC, -Qux<Qux> / Bar
        2 Qux!, Foo FROM Foo, X Bar<Qux<Foo>>. BY Wau<Xyz, Ahh>
        X MC? OR Foo<Qux>. / Xyz<Bar> OR -X Foo<Abc<Foo, Foo>>.
        MC / PROD[PROD[Bar]], 5X Ahh FROM Bar<Foo, Foo<Bar, Foo>>!
        (Foo OR Ahh<Qux>): (Ok BY Foo(NOT Bar)) BY Eep<Foo<Abc<Qux>>>
        Qux / 2 Abc<Bar>, MC OR (Foo, Qux, MC), -Foo, 2 Bar FROM Wau?
        Wau<Foo> FROM Foo!, (MC: 2 MC) OR (MC, 2 MC / Foo) OR -Qux / Foo
        (Plant OR Heat): (Ok BY Plant(NOT Steel)) BY Heat<Plant<Steel<Heat>>>
        -X Wau<Ahh<Ahh>, Bar(NOT Foo<Abc>)>, PROD[-MC OR (Foo FROM Qux<Abc>)]
        Steel<Heat> FROM Plant!, (Heat: 2 Heat) OR (Heat, 2 Heat / Plant) OR -Steel / Plant
        """
    )
  }

  @Test
  internal fun `L6-13 a backslash before a line ending continues an instruction`() {
    roundTrip<InstructionTree>("Plant\\\r\n OR Heat", "Plant OR Heat")
  }

  @Test
  internal fun `L6-13 grouping is re-inserted wherever re-parsing needs it`() {
    roundTrip<InstructionTree>("(Plant FROM This) / This", "Plant FROM This / This")
    roundTrip<InstructionTree>("Plant: (Heat, -5 Steel)")
    roundTrip<InstructionTree>("(Plant, Heat) OR Steel")
    roundTrip<InstructionTree>("(Plant FROM Heat) OR Steel")
    roundTrip<InstructionTree>("Plant OR Heat THEN Steel", "(Plant OR Heat) THEN Steel")
    roundTrip<InstructionTree>(
        "PROD[MC, -MC., PROD[MC: -MC], (MC, (Bar, 5 Foo))]",
        "PROD[MC, -MC., PROD[MC: -MC], MC, Bar, 5 Foo]",
    )
    roundTrip<InstructionTree>(
        "PROD[(Ooh / MC, Foo, MC), Bar / Bar THEN MC, MC]",
        "PROD[Ooh / MC, Foo, MC, Bar / Bar THEN MC, MC]",
    )
  }

  // L6-14 X across one instruction

  @Test
  internal fun `L6-14 X may span a sequence but not independent instructions`() {
    parse<InstructionTree>("X Plant THEN 2X Heat").toString() shouldBe "X Plant THEN 2X Heat"
    shouldThrow<PetSyntaxException> { parse<InstructionTree>("X Plant, X Heat") }
  }
}
