package dev.martianzoo.tfm.pets.ast

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Effect.Companion.effect
import dev.martianzoo.tfm.pets.ast.Effect.Trigger
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.OnGain
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.OnRemove
import dev.martianzoo.tfm.pets.ast.From.ComplexFrom
import dev.martianzoo.tfm.pets.ast.From.SimpleFrom
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.Instruction.Gated
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.AMAP
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.OPTIONAL
import dev.martianzoo.tfm.pets.ast.Instruction.Remove
import dev.martianzoo.tfm.pets.ast.Instruction.Then
import dev.martianzoo.tfm.pets.ast.Instruction.Transmute
import dev.martianzoo.tfm.pets.ast.Requirement.Exact
import dev.martianzoo.tfm.pets.ast.Requirement.Max
import dev.martianzoo.tfm.pets.ast.Requirement.Min
import dev.martianzoo.tfm.pets.ast.ScalarAndType.Companion.sat
import dev.martianzoo.tfm.pets.checkBothWays
import dev.martianzoo.tfm.pets.countNodesInTree
import dev.martianzoo.tfm.pets.testSampleStrings
import dev.martianzoo.tfm.testlib.PetGenerator
import org.junit.jupiter.api.Disabled
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
    // ef, og, te, cn, te, cn, pr, or, tr, fr, te, cn, te, cn, ga, sat, te, cn
    assertThat(countNodesInTree(eff)).isEqualTo(18)
  }

  @Disabled
  @Test
  fun genGrossApiCalls() {
    PetGenerator(0.95).generateTestApiConstructions<Effect>(20)
  }

  @Test
  fun testGross() {
    checkBothWays(
        "-Abc:: 1 Foo<Bar FROM Bar> / Abc",
        Effect(
            OnRemove(cn("Abc").type),
            Instruction.Per(
                Transmute(
                    ComplexFrom(cn("Foo"), listOf(SimpleFrom(cn("Bar").type, cn("Bar").type))), 1),
                sat(cn("Abc").type)),
            true))

    checkBothWays(
        "PROD[-Bar]: PROD[1 Xyz<Bar> FROM Foo]",
        Effect(
            Trigger.Transform(OnRemove(cn("Bar").type), "PROD"),
            Instruction.Transform(
                Transmute(SimpleFrom(cn("Xyz").addArgs(cn("Bar")), cn("Foo").type), 1),
                "PROD")))

    checkBothWays(
        "Ahh: PROD[1 Bar FROM Bar.]",
        Effect(
            OnGain(cn("Ahh").type),
            Instruction.Transform(
                Transmute(SimpleFrom(cn("Bar").type, cn("Bar").type), 1, AMAP), "PROD")))

    checkBothWays(
        "Ooh: @name(Qux<Ahh>)",
        Effect(
            OnGain(cn("Ooh").type), Instruction.Custom("name", cn("Qux").addArgs(cn("Ahh")))))

    checkBothWays(
        "Wau: PROD[Ooh / Qux]",
        Effect(
            OnGain(cn("Wau").type),
            Instruction.Transform(
                Instruction.Per(Gain(sat(cn("Ooh").type)), sat(cn("Qux").type)), "PROD")))

    checkBothWays(
        "-Bar<Bar>:: 1!",
        Effect(
            OnRemove(cn("Bar").addArgs(cn("Bar"))),
            Gain(sat(cn("Megacredit").type), MANDATORY),
            true))

    checkBothWays(
        "-Xyz<Xyz>(HAS Bar): 1 Foo<Foo> FROM Xyz?",
        Effect(
            OnRemove(cn("Xyz").addArgs(cn("Xyz")).refine(Min(sat(1, cn("Bar").type)))),
            Transmute(SimpleFrom(cn("Foo").addArgs(cn("Foo")), cn("Xyz").type), 1, OPTIONAL)))

    checkBothWays(
        "PROD[-Qux]: 5 Foo<Bar<Xyz> FROM Qux> OR Bar<Foo, Foo<Foo>>, Qux, Xyz OR 1," +
            " 1 Bar<Bar<Ahh>> FROM Ooh",
        Effect(
            Trigger.Transform(OnRemove(cn("Qux").type), "PROD"),
            Instruction.Multi(
                Instruction.Or(
                    Transmute(
                        ComplexFrom(
                            cn("Foo"),
                            listOf(SimpleFrom(cn("Bar").addArgs(cn("Xyz")), cn("Qux").type))),
                        5),
                    Gain(
                        sat(cn("Bar").addArgs(cn("Foo").type, cn("Foo").addArgs(cn("Foo").type))))),
                Gain(sat(cn("Qux").type)),
                Instruction.Or(Gain(sat(cn("Xyz").type)), Gain(sat(cn("Megacredit").type))),
                Transmute(
                    SimpleFrom(
                        cn("Bar").addArgs(cn("Bar").addArgs(cn("Ahh").type)), cn("Ooh").type),
                    1))))

    checkBothWays(
        "-Bar: (Bar / Qux, 1 / Megacredit), 1 / Megacredit",
        Effect(
            OnRemove(cn("Bar").type),
            Instruction.Multi(
                Instruction.Multi(
                    Instruction.Per(Gain(sat(cn("Bar").type)), sat(cn("Qux").type)),
                    Instruction.Per(
                        Gain(sat(1, cn("Megacredit").type)), sat(cn("Megacredit").type))),
                Instruction.Per(Gain(sat(1, cn("Megacredit").type)), sat(cn("Megacredit").type)))))

    checkBothWays(
        "-Bar<Bar, Foo<Foo>>: 1 THEN (1! OR Abc / 11 Megacredit) THEN =1 Megacredit: -Abc",
        Effect(
            OnRemove(cn("Bar").addArgs(cn("Bar").type, cn("Foo").addArgs(cn("Foo").type))),
            Then(
                Gain(sat(cn("Megacredit").type)),
                Instruction.Or(
                    Gain(sat(1, cn("Megacredit").type), MANDATORY),
                    Instruction.Per(Gain(sat(cn("Abc").type)), sat(11, cn("Megacredit").type))),
                Gated(Exact(sat(cn("Megacredit").type)), Remove(sat(1, cn("Abc").type))))))

    checkBothWays(
        "Bar: PROD[-5, (Abc / Megacredit, 1 Foo FROM Foo), (Foo OR 1): " +
            "5 Foo<Qux FROM Foo>, (1 Foo FROM Qux<Qux>, Bar / Megacredit)]",
        Effect(
            OnGain(cn("Bar").type),
            Instruction.Transform(
                Instruction.Multi(
                    Remove(sat(5, cn("Megacredit").type)),
                    Instruction.Multi(
                        Instruction.Per(Gain(sat(cn("Abc").type)), sat(cn("Megacredit").type)),
                        Transmute(SimpleFrom(cn("Foo").type, cn("Foo").type), 1)),
                    Gated(
                        Requirement.Or(
                            Min(sat(1, cn("Foo").type)), Min(sat(cn("Megacredit").type))),
                        Transmute(
                            ComplexFrom(
                                cn("Foo"), listOf(SimpleFrom(cn("Qux").type, cn("Foo").type))),
                            5)),
                    Instruction.Multi(
                        Transmute(SimpleFrom(cn("Foo").type, cn("Qux").addArgs(cn("Qux").type)), 1),
                        Instruction.Per(Gain(sat(1, cn("Bar").type)), sat(cn("Megacredit").type)))),
                "PROD")))

    checkBothWays(
        "-Abc<Eep<Foo>, Foo>: (1 Bar FROM Foo, Bar), PROD[1 Qux FROM Qux]",
        Effect(
            OnRemove(cn("Abc").addArgs(cn("Eep").addArgs(cn("Foo").type), cn("Foo").type)),
            Instruction.Multi(
                Instruction.Multi(
                    Transmute(SimpleFrom(cn("Bar").type, cn("Foo").type), 1),
                    Gain(sat(1, cn("Bar").type))),
                Instruction.Transform(
                    Transmute(SimpleFrom(cn("Qux").type, cn("Qux").type), 1), "PROD"))))

    checkBothWays(
        "PROD[-Xyz]: PROD[1]",
        Effect(
            Trigger.Transform(OnRemove(cn("Xyz").type), "PROD"),
            Instruction.Transform(Gain(sat(cn("Megacredit").type)), "PROD")))

    checkBothWays(
        "-Bar: Qux, (Bar OR Foo OR 1, 1): -Foo, MAX 5 Qux<Qux>: Bar",
        Effect(
            OnRemove(cn("Bar").type),
            Instruction.Multi(
                Gain(sat(cn("Qux").type)),
                Gated(
                    Requirement.And(
                        Requirement.Or(
                            Min(sat(cn("Bar").type)),
                            Min(sat(cn("Foo").type)),
                            Min(sat(cn("Megacredit").type))),
                        Min(sat(cn("Megacredit").type))),
                    Remove(sat(cn("Foo").type))),
                Gated(Max(sat(5, cn("Qux").addArgs(cn("Qux").type))), Gain(sat(cn("Bar").type))))))

    checkBothWays(
        "PROD[Foo]: Xyz OR Bar, 1 Abc FROM Ahh, MAX 1 Ooh: ((Foo, 1) OR " +
            "((1 OR -1) OR (1, 1)) OR 1), (-1!, Bar) OR -5 Foo!",
        Effect(
            Trigger.Transform(OnGain(cn("Foo").type), "PROD"),
            Instruction.Multi(
                Instruction.Or(Gain(sat(1, cn("Xyz").type)), Gain(sat(1, cn("Bar").type))),
                Transmute(SimpleFrom(cn("Abc").type, cn("Ahh").type), 1),
                Gated(
                    Max(sat(cn("Ooh").type)),
                    Instruction.Or(
                        Instruction.Multi(
                            Gain(sat(1, cn("Foo").type)), Gain(sat(cn("Megacredit").type))),
                        Instruction.Or(
                            Instruction.Or(
                                Gain(sat(1, cn("Megacredit").type)),
                                Remove(sat(1, cn("Megacredit").type))),
                            Instruction.Multi(
                                Gain(sat(cn("Megacredit").type)),
                                Gain(sat(1, cn("Megacredit").type)))),
                        Gain(sat(cn("Megacredit").type)))),
                Instruction.Or(
                    Instruction.Multi(
                        Remove(sat(cn("Megacredit").type), MANDATORY), Gain(sat(cn("Bar").type))),
                    Remove(sat(5, cn("Foo").type), MANDATORY)))))

    checkBothWays(
        "Bar<Ahh<Abc, Foo>>: 1 Bar FROM Bar! THEN (-1, 1 THEN 1), Ahh<Foo>, Bar, Ahh?",
        Effect(
            OnGain(cn("Bar").addArgs(cn("Ahh").addArgs(cn("Abc").type, cn("Foo").type))),
            Instruction.Multi(
                Then(
                    Transmute(SimpleFrom(cn("Bar").type, cn("Bar").type), 1, MANDATORY),
                    Instruction.Multi(
                        Remove(sat(1, cn("Megacredit").type)),
                        Then(Gain(sat(cn("Megacredit").type)), Gain(sat(cn("Megacredit").type))))),
                Gain(sat(cn("Ahh").addArgs(cn("Foo").type))),
                Gain(sat(1, cn("Bar").type)),
                Gain(sat(cn("Ahh").type), OPTIONAL))))

    checkBothWays(
        "-Wau<Abc<Abc>>: (1 Abc FROM Ahh<Qux>) OR -Bar",
        Effect(
            OnRemove(cn("Wau").addArgs(cn("Abc").addArgs(cn("Abc").type))),
            Instruction.Or(
                Transmute(SimpleFrom(cn("Abc").type, cn("Ahh").addArgs(cn("Qux").type)), 1),
                Remove(sat(1, cn("Bar").type)))))

    checkBothWays(
        "PROD[Bar]: Xyz / 5 Qux, 1 OR (@name(Abc), 1, -Bar / Foo)",
        Effect(
            Trigger.Transform(OnGain(cn("Bar").type), "PROD"),
            Instruction.Multi(
                Instruction.Per(Gain(sat(1, cn("Xyz").type)), sat(5, cn("Qux").type)),
                Instruction.Or(
                    Gain(sat(cn("Megacredit").type)),
                    Instruction.Multi(
                        Instruction.Custom("name", cn("Abc").type),
                        Gain(sat(1, cn("Megacredit").type)),
                        Instruction.Per(Remove(sat(cn("Bar").type)), sat(cn("Foo").type)))))))

    checkBothWays(
        "PROD[Foo]: Abc<Foo<Abc, Bar>>",
        Effect(
            Trigger.Transform(OnGain(cn("Foo").type), "PROD"),
            Gain(sat(cn("Abc").addArgs(cn("Foo").addArgs(cn("Abc").type, cn("Bar").type))))))

    checkBothWays(
        "Bar<Ahh<Qux>, Xyz>:: -Abc?",
        Effect(
            OnGain(cn("Bar").addArgs(cn("Ahh").addArgs(cn("Qux").type), cn("Xyz").type)),
            Remove(sat(1, cn("Abc").type), OPTIONAL),
            true))
  }
}
