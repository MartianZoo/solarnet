package dev.martianzoo.tfm.pets.ast

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Effect.Companion.effect
import dev.martianzoo.tfm.pets.ast.Effect.Trigger
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.OnGainOf
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.OnRemoveOf
import dev.martianzoo.tfm.pets.ast.FromExpression.ComplexFrom
import dev.martianzoo.tfm.pets.ast.FromExpression.SimpleFrom
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.Instruction.Gated
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.AMAP
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.OPTIONAL
import dev.martianzoo.tfm.pets.ast.Instruction.Remove
import dev.martianzoo.tfm.pets.ast.Instruction.Then
import dev.martianzoo.tfm.pets.ast.Instruction.Transmute
import dev.martianzoo.tfm.pets.ast.Metric.Count
import dev.martianzoo.tfm.pets.ast.Requirement.Exact
import dev.martianzoo.tfm.pets.ast.Requirement.Max
import dev.martianzoo.tfm.pets.ast.Requirement.Min
import dev.martianzoo.tfm.pets.ast.ScaledExpression.Companion.scaledEx
import dev.martianzoo.tfm.pets.checkBothWays
import dev.martianzoo.tfm.pets.testSampleStrings
import dev.martianzoo.tfm.testlib.PetGenerator
import org.junit.jupiter.api.Test

// Most testing is done by AutomatedTest
private class EffectTest {

  val inputs =
      """
    Ooh: 1
    Xyz: -1
    -Foo: 5!
    Abc: -Abc
    -Xyz: -Bar
    -Xyz:: -Abc
    Eep: PROD[1]
    Qux: -11 Eep!
    PROD[Ahh]: Foo
    Ahh: @name(Wau)
    PROD[Ahh]: -Qux?
    Qux:: PROD[5 Foo]
    PROD[Xyz]: PROD[1]
    -Ooh<Xyz, Bar>:: -1
    Ooh: PROD[PROD[Foo]]
    PROD[Bar]: PROD[-Bar]
    PROD[Xyz]:: PROD[-Bar]
    PROD[Ooh]: -Foo, 11 Bar
    -Foo<Bar, Bar, Abc>: Bar
    PROD[Abc]: 1 Ooh FROM Xyz
    -Ooh: @name(Qux<Qux<Bar>>)
    -Ooh: (1 Qux FROM Bar) OR 5
    Xyz<Ooh, Bar, Eep>: PROD[-5]
    Xyz: PROD[Qux<Foo, Abc>: Foo]
    Wau: 1 Bar(HAS 5 Qux) FROM Ahh
    Ooh:: 1 / Abc, 11 Ooh<Foo<Foo>>
    -Qux: Xyz / 5 Foo(HAS MAX 1 Qux)
    Abc<Foo>:: 5 Foo FROM Abc<Foo>, 5
    Ooh<Bar>:: 11 Foo, 5 Foo<Bar<Foo>>
    -Bar(HAS Bar): -Eep<Qux, Bar<Bar>>.
    -Qux<Ooh<Foo>>(HAS 5 OR Bar): -5 Ooh
    PROD[Qux]: (1: (1 / 5 Foo, Foo<Xyz>))
    PROD[-Ahh]: PROD[-Qux / 11 Megacredit]
    Eep: (1 THEN 1) OR (-Qux, 1, -5 Foo, 1)
    Qux:: 1 Bar FROM Bar / Bar<Qux>, -5 Qux?
    -Ooh: Ooh, (5 Abc<Foo>, 1: -1), 5 Foo!, 1
    Eep<Abc>:: 11 Ahh<Foo>, @name(Qux) OR -Abc
    Foo: PROD[5. OR (=1 Megacredit: (-1 OR 1))]
    -Foo<Ooh<Abc>>: 11 Xyz, 1, -Foo!, @name(Ooh)
    Xyz<Xyz>: 1 Xyz FROM Abc / Xyz<Xyz<Bar>, Bar>
    PROD[Abc]: Ooh OR (1 THEN Foo.), -11, Foo, Ooh
    PROD[-Foo]: Qux: Qux / 5 Bar, Qux OR @name(Abc)
    -Ooh<Foo<Ahh>>(HAS 1 OR (1 OR Foo)): Bar, -5 Ooh
    -Eep<Foo, Ooh<Foo>>: ((-1, -Abc), 1!) OR Abc<Qux>
    PROD[-Abc]: (Xyz OR MAX 0 Qux): -1 / Ooh<Foo>, Foo
    PROD[Ooh<Ooh>]: PROD[Abc / Qux, -Ooh OR @name(Foo)]
    Eep: (1, 1 OR (Foo: @name(Foo))) OR (@name(Bar), -1)
    -Wau<Bar<Foo>>: -5, 1. / 11 Abc, 5 Abc FROM Foo / Ooh
    -Foo: PROD[5 Abc], (-1 THEN 1) OR (Bar OR (1: 1)), Ahh
    Qux<Abc<Qux>, Qux>:: (1, 1 OR Foo) OR (1 Abc FROM Abc.)
    Foo(HAS 11 Foo): -1 / 5 Abc<Bar>, @name(Abc), -Abc<Qux>.
    -Ahh<Xyz(HAS =0 Megacredit), Foo<Abc>>: 5 / 11 Megacredit
    -Xyz: (1 / Bar, 1 / Wau), (1, 1 / Foo OR Qux OR (1: -Bar))
    -Bar<Foo<Ahh, Foo<Foo>>, Foo, Qux<Qux<Qux>, Foo>>:: PROD[1]
    -Abc<Qux>: ((1 OR (MAX 1 Megacredit OR 1)): (Bar, 1 OR Qux))
  """
          .trimIndent()

  @Test
  fun testSampleStrings() {
    testSampleStrings<Effect>(inputs)
  }

  @Test
  fun nodeCount() {
    val eff = effect("Xyz<Xyz>: PROD[(1 Abc FROM Qux) OR 1]")
    // ef, og, te, cn, te, cn, pr, or, tr, fr, te, cn, te, cn, ga, ste, te, cn
    assertThat(eff.descendantCount()).isEqualTo(18)
  }

  // @Test
  fun genGrossApiCalls() {
    PetGenerator(0.95).generateTestApiConstructions<Effect>(20)
  }

  @Test
  fun testGross() {
    checkBothWays(
        "-Abc:: 1 Foo<Bar FROM Bar> / Abc",
        Effect(
            OnRemoveOf.create(cn("Abc").expr),
            Instruction.Per(
                Transmute(
                    ComplexFrom(cn("Foo"), listOf(SimpleFrom(cn("Bar").expr, cn("Bar").expr))), 1),
                Count(scaledEx(cn("Abc").expr))),
            true))

    checkBothWays(
        "PROD[-Bar]: PROD[1 Xyz<Bar> FROM Foo]",
        Effect(
            Trigger.Transform(OnRemoveOf.create(cn("Bar").expr), "PROD"),
            Instruction.Transform(
                Transmute(SimpleFrom(cn("Xyz").addArgs(cn("Bar")), cn("Foo").expr), 1), "PROD")))

    checkBothWays(
        "Ahh: PROD[1 Bar FROM Bar.]",
        Effect(
            OnGainOf.create(cn("Ahh").expr),
            Instruction.Transform(
                Transmute(SimpleFrom(cn("Bar").expr, cn("Bar").expr), 1, AMAP), "PROD")))

    checkBothWays(
        "Ooh: @name(Qux<Ahh>)",
        Effect(
            OnGainOf.create(cn("Ooh").expr),
            Instruction.Custom("name", cn("Qux").addArgs(cn("Ahh")))))

    checkBothWays(
        "Wau: PROD[Ooh / Qux]",
        Effect(
            OnGainOf.create(cn("Wau").expr),
            Instruction.Transform(
                Instruction.Per(Gain(scaledEx(cn("Ooh").expr)),
                    Count(scaledEx(cn("Qux").expr))),
                "PROD")))

    checkBothWays(
        "-Bar<Bar>:: 1!",
        Effect(
            OnRemoveOf.create(cn("Bar").addArgs(cn("Bar"))),
            Gain(scaledEx(cn("Megacredit").expr), MANDATORY),
            true))

    checkBothWays(
        "-Xyz<Xyz>(HAS Bar): 1 Foo<Foo> FROM Xyz?",
        Effect(
            OnRemoveOf.create(
                cn("Xyz").addArgs(cn("Xyz")).refine(Min(scaledEx(1, cn("Bar").expr)))),
            Transmute(SimpleFrom(cn("Foo").addArgs(cn("Foo")), cn("Xyz").expr), 1, OPTIONAL)))

    checkBothWays(
        "PROD[-Qux]: 5 Foo<Bar<Xyz> FROM Qux> OR Bar<Foo, Foo<Foo>>, Qux, Xyz OR 1," +
            " 1 Bar<Bar<Ahh>> FROM Ooh",
        Effect(
            Trigger.Transform(OnRemoveOf.create(cn("Qux").expr), "PROD"),
            Instruction.Multi(
                Instruction.Or(
                    Transmute(
                        ComplexFrom(
                            cn("Foo"),
                            listOf(SimpleFrom(cn("Bar").addArgs(cn("Xyz")), cn("Qux").expr))),
                        5),
                    Gain(
                        scaledEx(
                            cn("Bar").addArgs(cn("Foo").expr, cn("Foo").addArgs(cn("Foo").expr))))),
                Gain(scaledEx(cn("Qux").expr)),
                Instruction.Or(
                    Gain(scaledEx(cn("Xyz").expr)), Gain(scaledEx(cn("Megacredit").expr))),
                Transmute(
                    SimpleFrom(
                        cn("Bar").addArgs(cn("Bar").addArgs(cn("Ahh").expr)), cn("Ooh").expr),
                    1))))

    checkBothWays(
        "-Bar: (Bar / Qux, 1 / Megacredit), 1 / Megacredit",
        Effect(
            OnRemoveOf.create(cn("Bar").expr),
            Instruction.Multi(
                Instruction.Multi(
                    Instruction.Per(Gain(scaledEx(cn("Bar").expr)), Count(scaledEx(cn("Qux")
                        .expr))),
                    Instruction.Per(
                        Gain(scaledEx(1, cn("Megacredit").expr)),
                        Count(scaledEx(cn("Megacredit").expr)))),
                Instruction.Per(
                    Gain(scaledEx(1, cn("Megacredit").expr)),
                    Count(scaledEx(cn("Megacredit").expr))))))

    checkBothWays(
        "-Bar<Bar, Foo<Foo>>: 1 THEN (1! OR Abc / 11 Megacredit) THEN =1 Megacredit: -Abc",
        Effect(
            OnRemoveOf.create(cn("Bar").addArgs(cn("Bar").expr, cn("Foo").addArgs(cn("Foo").expr))),
            Then(
                Gain(scaledEx(cn("Megacredit").expr)),
                Instruction.Or(
                    Gain(scaledEx(1, cn("Megacredit").expr), MANDATORY),
                    Instruction.Per(
                        Gain(scaledEx(cn("Abc").expr)),
                        Count(scaledEx(11, cn("Megacredit").expr)))),
                Gated(
                    Exact(scaledEx(cn("Megacredit").expr)),
                    Remove(scaledEx(1, cn("Abc").expr))))))

    checkBothWays(
        "Bar: PROD[-5, (Abc / Megacredit, 1 Foo FROM Foo), (Foo OR 1): " +
            "5 Foo<Qux FROM Foo>, (1 Foo FROM Qux<Qux>, Bar / Megacredit)]",
        Effect(
            OnGainOf.create(cn("Bar").expr),
            Instruction.Transform(
                Instruction.Multi(
                    Remove(scaledEx(5, cn("Megacredit").expr)),
                    Instruction.Multi(
                        Instruction.Per(
                            Gain(scaledEx(cn("Abc").expr)),
                            Count(scaledEx(cn("Megacredit").expr))),
                        Transmute(SimpleFrom(cn("Foo").expr, cn("Foo").expr), 1)),
                    Gated(
                        Requirement.Or(
                            Min(scaledEx(1, cn("Foo").expr)),
                            Min(scaledEx(cn("Megacredit").expr))),
                        Transmute(
                            ComplexFrom(
                                cn("Foo"), listOf(SimpleFrom(cn("Qux").expr, cn("Foo").expr))),
                            5)),
                    Instruction.Multi(
                        Transmute(SimpleFrom(cn("Foo").expr, cn("Qux").addArgs(cn("Qux").expr)), 1),
                        Instruction.Per(
                            Gain(scaledEx(1, cn("Bar").expr)),
                            Count(scaledEx(cn("Megacredit").expr))))),
                "PROD")))

    checkBothWays(
        "-Abc<Eep<Foo>, Foo>: (1 Bar FROM Foo, Bar), PROD[1 Qux FROM Qux]",
        Effect(
            OnRemoveOf.create(cn("Abc").addArgs(cn("Eep").addArgs(cn("Foo").expr), cn("Foo").expr)),
            Instruction.Multi(
                Instruction.Multi(
                    Transmute(SimpleFrom(cn("Bar").expr, cn("Foo").expr), 1),
                    Gain(scaledEx(1, cn("Bar").expr))),
                Instruction.Transform(
                    Transmute(SimpleFrom(cn("Qux").expr, cn("Qux").expr), 1), "PROD"))))

    checkBothWays(
        "PROD[-Xyz]: PROD[1]",
        Effect(
            Trigger.Transform(OnRemoveOf.create(cn("Xyz").expr), "PROD"),
            Instruction.Transform(Gain(scaledEx(cn("Megacredit").expr)), "PROD")))

    checkBothWays(
        "-Bar: Qux, (Bar OR Foo OR 1, 1): -Foo, MAX 5 Qux<Qux>: Bar",
        Effect(
            OnRemoveOf.create(cn("Bar").expr),
            Instruction.Multi(
                Gain(scaledEx(cn("Qux").expr)),
                Gated(
                    Requirement.And(
                        Requirement.Or(
                            Min(scaledEx(cn("Bar").expr)),
                            Min(scaledEx(cn("Foo").expr)),
                            Min(scaledEx(cn("Megacredit").expr))),
                        Min(scaledEx(cn("Megacredit").expr))),
                    Remove(scaledEx(cn("Foo").expr))),
                Gated(
                    Max(scaledEx(5, cn("Qux").addArgs(cn("Qux").expr))),
                    Gain(scaledEx(cn("Bar").expr))))))

    checkBothWays(
        "PROD[Foo]: Xyz OR Bar, 1 Abc FROM Ahh, MAX 1 Ooh: ((Foo, 1) OR " +
            "((1 OR -1) OR (1, 1)) OR 1), (-1!, Bar) OR -5 Foo!",
        Effect(
            Trigger.Transform(OnGainOf.create(cn("Foo").expr), "PROD"),
            Instruction.Multi(
                Instruction.Or(
                    Gain(scaledEx(1, cn("Xyz").expr)), Gain(scaledEx(1, cn("Bar").expr))),
                Transmute(SimpleFrom(cn("Abc").expr, cn("Ahh").expr), 1),
                Gated(
                    Max(scaledEx(cn("Ooh").expr)),
                    Instruction.Or(
                        Instruction.Multi(
                            Gain(scaledEx(1, cn("Foo").expr)),
                            Gain(scaledEx(cn("Megacredit").expr))),
                        Instruction.Or(
                            Instruction.Or(
                                Gain(scaledEx(1, cn("Megacredit").expr)),
                                Remove(scaledEx(1, cn("Megacredit").expr))),
                            Instruction.Multi(
                                Gain(scaledEx(cn("Megacredit").expr)),
                                Gain(scaledEx(1, cn("Megacredit").expr)))),
                        Gain(scaledEx(cn("Megacredit").expr)))),
                Instruction.Or(
                    Instruction.Multi(
                        Remove(scaledEx(cn("Megacredit").expr), MANDATORY),
                        Gain(scaledEx(cn("Bar").expr))),
                    Remove(scaledEx(5, cn("Foo").expr), MANDATORY)))))

    checkBothWays(
        "Bar<Ahh<Abc, Foo>>: 1 Bar FROM Bar! THEN (-1, 1 THEN 1), Ahh<Foo>, Bar, Ahh?",
        Effect(
            OnGainOf.create(cn("Bar").addArgs(cn("Ahh").addArgs(cn("Abc").expr, cn("Foo").expr))),
            Instruction.Multi(
                Then(
                    Transmute(SimpleFrom(cn("Bar").expr, cn("Bar").expr), 1, MANDATORY),
                    Instruction.Multi(
                        Remove(scaledEx(1, cn("Megacredit").expr)),
                        Then(
                            Gain(scaledEx(cn("Megacredit").expr)),
                            Gain(scaledEx(cn("Megacredit").expr))))),
                Gain(scaledEx(cn("Ahh").addArgs(cn("Foo").expr))),
                Gain(scaledEx(1, cn("Bar").expr)),
                Gain(scaledEx(cn("Ahh").expr), OPTIONAL))))

    checkBothWays(
        "-Wau<Abc<Abc>>: (1 Abc FROM Ahh<Qux>) OR -Bar",
        Effect(
            OnRemoveOf.create(cn("Wau").addArgs(cn("Abc").addArgs(cn("Abc").expr))),
            Instruction.Or(
                Transmute(SimpleFrom(cn("Abc").expr, cn("Ahh").addArgs(cn("Qux").expr)), 1),
                Remove(scaledEx(1, cn("Bar").expr)))))
  }
}
