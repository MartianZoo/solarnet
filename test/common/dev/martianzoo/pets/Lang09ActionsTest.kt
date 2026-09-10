package dev.martianzoo.pets

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.Transforming.actionListToEffects
import dev.martianzoo.pets.Transforming.immediateToEffect
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.Action.Cost
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.InstructionTree
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Section 9 of `docs/pets-language-spec.md`: actions a player may invoke. */
internal class Lang09ActionsTest {

  // L9-1 The shape of an action

  @Test
  internal fun `L9-1 an action is an optional cost, an arrow and an instruction`() {
    val action = parse<Action>("Ore -> 5 Widget")

    action.cost shouldBe Cost.Spend(parse("Ore"))
    action.instruction shouldBe parse<InstructionTree>("5 Widget")
    parse<Action>("-> Ok").cost shouldBe null
    parse<Action>("-> Ok").toString() shouldBe "-> Ok"
  }

  // L9-2 What an action means

  @Test
  internal fun `L9-2 an action means spend the cost, then do the result`() {
    parse<Action>("Ore -> 5 Widget").toInstruction() shouldBe
        parse<InstructionTree>("-Ore! THEN 5 Widget")
    parse<Action>("-> 5 Widget").toInstruction() shouldBe parse<InstructionTree>("5 Widget")
    parse<Action>("Ore -> Widget, Gizmo").toInstruction() shouldBe
        parse<InstructionTree>("-Ore! THEN (Widget, Gizmo)")
  }

  @Test
  internal fun `L9-2 the result's own sequence joins the one the arrow makes`() {
    val lowered = parse<Action>("Ore -> Widget THEN Gizmo").toInstruction()

    lowered shouldBe parse<InstructionTree>("-Ore! THEN Widget THEN Gizmo")
    (lowered as dev.martianzoo.pets.ast.Instruction.Then).instructions.size shouldBe 3
  }

  // L9-3 Cost forms

  @Test
  internal fun `L9-3 a cost is a scaled expression, optionally per metric or transformed`() {
    parse<Action>("2 Ore -> Widget").cost shouldBe Cost.Spend(parse("2 Ore"))
    parse<Action>("Ore / Gizmo -> Widget").cost shouldBe
        Cost.Per(Cost.Spend(parse("Ore")), parse("Gizmo"))
    parse<Action>("PROD[Ore] -> Widget").cost shouldBe
        Cost.Transform(Cost.Spend(parse("Ore")), "PROD")
    parse<Action>("Ore / Gizmo -> Widget").toInstruction() shouldBe
        parse<InstructionTree>("-Ore! / Gizmo THEN Widget")
  }

  @Test
  internal fun `L9-3 alternative costs are separate actions, not one composite cost`() {
    listOf("=0 Award: 8 Ore -> Award", "Ore, Gizmo -> Award").forEach {
      shouldThrow<PetSyntaxException> { parse<Action>(it) }
    }
  }

  // L9-4 Lowering to an effect

  @Test
  internal fun `L9-4 an action becomes an effect keyed to its position on its class`() {
    actionListToEffects(listOf(parse("-> Widget"), parse("Ore -> 5 Gizmo"))) shouldContainExactly
        listOf(
            parse<Effect>("UseAction<This, Action1>: Widget"),
            parse<Effect>("UseAction<This, Action2>: -Ore! THEN 5 Gizmo"),
        )
  }

  @Test
  internal fun `L9-4 a class offers at most three actions`() {
    val four = List(4) { parse<Action>("-> Widget$it") }

    actionListToEffects(four.take(3)).size shouldBe 3
    shouldThrow<IllegalArgumentException> { actionListToEffects(four) }
  }

  // L9-5 A class's effects

  @Test
  internal fun `L9-5 a class's effects are its authored effects then its lowered actions`() {
    val declaration = parseClasses("CLASS Bench {\n  This: Widget\n  Ore -> Gizmo\n}").single()

    declaration.authoredEffects shouldContainExactly listOf(parse<Effect>("This: Widget"))
    declaration.effects shouldContainExactly
        listOf(
            parse<Effect>("This: Widget"),
            parse<Effect>("UseAction<This, Action1>: -Ore! THEN Gizmo"),
        )
  }

  // L9-6 Immediate instructions

  @Test
  internal fun `L9-6 an instruction that happens on gain is the effect This colon it`() {
    immediateToEffect(parse("Widget, Gizmo")) shouldBe parse<Effect>("This: Widget, Gizmo")
    immediateToEffect(parse("Ore: Widget")) shouldBe parse<Effect>("This: (Ore: Widget)")
    immediateToEffect(parse("Ok")) shouldBe null
    immediateToEffect(parse("Widget"), effectIsAutomatic = true)!!.automatic shouldBe true
  }

  // Rendering

  @Test
  internal fun `L9-1 actions round-trip`() {
    roundTripAll<Action>(
        """
        -> 2 MC?
        Abc -> Ok
        Foo -> MC
        Ore -> Ok
        2 MC -> MC?
        Bar -> -Abc
        Plant -> MC
        -> Qux<Foo>?
        -> X Bar<Bar>
        Widget -> -Ore
        -> Gizmo<Plant>?
        PROD[Ooh] -> 2 MC
        MC -> Eep FROM Foo
        -> X Widget<Widget>
        5 Xyz -> -Abc / Ooh
        5 Xyz -> -Ore / Ooh
        MC -> Eep FROM Plant
        Xyz<Qux<Bar>> -> X MC
        MC / Ahh -> -Foo<Qux>?
        X Foo(NOT Xyz) -> 2X Bar
        2X Qux -> 2 Bar, MC OR Ok
        2 MC -> -Wau<Foo(NOT Qux)>
        2 Xyz -> -2X MC, Bar / Foo
        MC / Ahh -> -Plant<Gizmo>?
        Qux / Eep -> -Ooh<Foo, Eep>
        X Abc -> -MC, Foo<Bar<Qux>>
        -> (Foo: Foo FROM Foo) OR Ok
        -> -X Ahh<Foo>(HAS MAX 0 MC)!
        X Plant(NOT Xyz) -> 2X Widget
        2 MC -> -Wau<Plant(NOT Gizmo)>
        2 Wau -> -X Foo(NOT Ooh<Xyz>)!
        2 Xyz -> -2X MC, Widget / Plant
        PROD[Qux] -> Qux FROM Bar / Abc
        Plant / Eep -> -Ooh<Plant, Eep>
        PROD[MC / Foo] -> X MC, Foo, -Foo
        -> (Plant: Plant FROM Plant) OR Ok
        Abc<Qux<Eep>> -> Ok OR (MC, 2 Foo)
        PROD[MC] -> (-Foo OR MC / Qux) OR Ooh
        5 Ooh<Ooh, Bar> -> -2X Foo(HAS =1 MC).
        X Ooh<Abc>(HAS MC) -> Foo: -MC, -X Ooh
        MC / EVAL Bar.score - Abc MAX 11 -> -Foo
        -> MAX 0 MC: MC THEN MC., PROD[PROD[Bar]]
        5 Qux -> PROD[MC], PROD[Qux THEN Foo<Qux>]
        Ooh / 3 Ooh<Bar> MAX 5 -> 11 Abc FROM Ooh?
        2 Qux<Xyz(HAS MAX 1 MC, HAS MC)> -> X Ahh<Bar>.
        -> MAX 0 MC: MC THEN MC., PROD[PROD[Widget]]
        MC / EVAL Widget.score - Ore MAX 11 -> -Plant
        2 MC / Qux -> Bar, (Ooh FROM Bar / Foo) BY Foo
        PROD[Xyz(HAS MC)] -> X Bar(NOT Foo) FROM Ooh<Foo>
        5 Gizmo -> PROD[MC], PROD[Gizmo THEN Plant<Gizmo>]
        -> 5 MC, (MAX 0 Foo OR (MC OR Foo)): X Bar<Qux, Qux>
        PROD[PROD[Bar]] -> -Bar<Foo> BY Foo<Xyz<Ooh>> OR Abc.
        2 MC / Gizmo -> Widget, (Ooh FROM Widget / Plant) BY Plant
        Ooh -> -Foo / Ooh - Abc - 2 (Foo MAX 5) THEN Ok THEN X Foo
        2 Wau -> (MC BY Bar, MC) OR (Ooh / 2 Bar) BY Qux<Xyz>, -Abc
        PROD[Bar] -> -2 Bar<Foo(NOT Bar<Eep, Foo<Bar, Foo>, Abc>)>!
        Ooh -> -Plant / Ooh - Ore - 2 (Plant MAX 5) THEN Ok THEN X Plant
        """
    )
  }
}
