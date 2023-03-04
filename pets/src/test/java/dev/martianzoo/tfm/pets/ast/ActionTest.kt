package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.tfm.pets.ast.Action.Cost
import dev.martianzoo.tfm.pets.ast.Action.Cost.Spend
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.From.SimpleFrom
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.Instruction.Gated
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.AMAP
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.OPTIONAL
import dev.martianzoo.tfm.pets.ast.Instruction.Remove
import dev.martianzoo.tfm.pets.ast.Instruction.Transmute
import dev.martianzoo.tfm.pets.ast.Metric.Count
import dev.martianzoo.tfm.pets.ast.Requirement.Max
import dev.martianzoo.tfm.pets.ast.Requirement.Min
import dev.martianzoo.tfm.pets.ast.ScaledTypeExpr.Companion.scaledType
import dev.martianzoo.tfm.pets.checkBothWays
import dev.martianzoo.tfm.pets.testRoundTrip
import dev.martianzoo.tfm.pets.testSampleStrings
import dev.martianzoo.tfm.testlib.PetGenerator
import org.junit.jupiter.api.Test

// Most testing is done by AutomatedTest
private class ActionTest {
  @Test
  fun stupid() {
    testRoundTrip<Action>("-> Ok")
  }

  val inputs =
      """
    -> 11
    -> Bar
    1 -> -5!
    1 -> -11!
    11 Abc -> 1
    -> PROD[Qux]
    5 -> -11 Xyz?
    PROD[1] -> Qux
    Bar<Bar> -> 11!
    Abc -> Bar / Wau
    PROD[1] -> 11 Xyz
    -> @name(Qux, Xyz)
    PROD[1] -> Foo<Foo>
    PROD[1] -> Bar / Foo
    11 Abc<Qux> -> 11 Bar?
    Qux -> Wau<Ahh>: 11 Abc
    5 Xyz<Foo> -> 5 Qux<Bar>
    11 Abc -> 1. / Megacredit
    Foo -> PROD[-1 / Bar<Wau>]
    Bar -> 5 Ooh<Qux> FROM Abc.
    PROD[Bar] / Foo<Qux> -> -Xyz
    PROD[Foo] -> @name(Qux), Foo.
    5 Abc<Abc<Xyz>, Qux, Qux> -> 1
    1 -> -11 Ooh<Foo>, (Foo, -1, 1)
    PROD[1], 11 Xyz, 5 Foo -> 5 Ahh.
    Xyz<Foo> -> 11 Qux? THEN Ooh / Foo
    Foo<Foo> / Qux -> (1, Qux), -11 Foo
    Xyz<Abc<Qux>> -> PROD[@name(Qux), 1]
    -> (5 Qux FROM Bar, Bar) OR -1 OR Foo
    PROD[Bar<Foo>] / Foo -> -Foo<Foo, Bar>
    Bar, Bar -> Qux, Foo THEN ((1: 1) OR 1)
    5 Ooh<Abc> -> Ooh, 1 OR (1 Abc FROM Foo)
    1 / Eep -> 5 Foo THEN Bar: Abc<Qux> / Xyz
    5 OR 1 -> Foo<Foo<Foo>>. OR -Foo<Foo, Qux>
    Foo<Ahh<Foo<Ahh>>> -> 5, ((-1, 1: -1), Qux)
    1 / 11 Bar -> PROD[Abc], (5 Bar, (-Foo, -1))
    Xyz, Bar, Abc -> PROD[Xyz OR Abc / Megacredit]
    1 -> (1 OR PROD[1] OR (1 OR Foo OR 5 Bar)): -Qux
    Ooh<Qux> / Foo, 1 / Qux -> -Bar(HAS =1 Megacredit)
    Foo / Ooh OR 1, Xyz / 5 Abc, PROD[Foo, Foo] -> Foo!
    11 Foo / 11 Eep -> -Bar, 1, 1 Ahh FROM Ahh?, 1 / Bar
    1 / Megacredit OR PROD[11 / Megacredit] -> 1. OR -Xyz
    PROD[Eep] -> Ooh<Foo<Abc>>., Abc., 11 Bar<Qux, Bar>, 1
    Qux<Qux> -> 1 OR -1. OR (Foo<Qux>: ((1 OR -Foo) OR Foo))
  """
          .trimIndent()

  @Test
  fun testSampleStrings() {
    testSampleStrings<Action>(inputs)
  }

  // @Test
  fun genGrossApiCalls() {
    PetGenerator(0.9).generateTestApiConstructions<Action>(20)
  }

  @Test
  fun gross() {
    checkBothWays(
        "((1, Abc<Bar>) OR 1) OR PROD[1] -> 1?",
        Action(
            Cost.Or(
                Cost.Or(
                    Cost.Multi(
                        Spend(scaledType(cn("Megacredit").type)),
                        Spend(scaledType(cn("Abc").addArgs(cn("Bar"))))),
                    Spend(scaledType(cn("Megacredit").type))),
                Cost.Transform(Spend(scaledType(cn("Megacredit").type)), "PROD")),
            Gain(scaledType(cn("Megacredit").type), OPTIONAL)))

    checkBothWays(
        "5, 1 OR (1 OR Bar) OR 1 / Megacredit, 1 OR (1 OR Foo OR Qux) -> 5 / Ooh, " +
            "-Xyz<Qux<Foo<Qux<Foo>>>(HAS 5 Bar)>.",
        Action(
            Cost.Multi(
                Spend(scaledType(5, cn("Megacredit").type)),
                Cost.Or(
                    Spend(scaledType(cn("Megacredit").type)),
                    Cost.Or(
                        Spend(scaledType(cn("Megacredit").type)),
                        Spend(scaledType(cn("Bar").type))),
                    Cost.Per(
                        Spend(scaledType(cn("Megacredit").type)),
                        Count(scaledType(cn("Megacredit").type)))),
                Cost.Or(
                    Spend(scaledType(cn("Megacredit").type)),
                    Cost.Or(
                        Spend(scaledType(cn("Megacredit").type)),
                        Spend(scaledType(cn("Foo").type)),
                        Spend(scaledType(cn("Qux").type))))),
            Instruction.Multi(
                Instruction.Per(
                    Gain(scaledType(5, cn("Megacredit").type)), Count(scaledType(cn("Ooh").type))),
                Remove(
                    scaledType(
                        cn("Xyz")
                            .addArgs(
                                cn("Qux")
                                    .addArgs(cn("Foo").addArgs(cn("Qux").addArgs(cn("Foo"))))
                                    .refine(Min(scaledType(5, cn("Bar").type))))),
                    AMAP))))

    checkBothWays(
        "PROD[Bar], 5 Foo<Abc<Bar>>, Abc OR Xyz -> MAX 1 Bar: -Xyz",
        Action(
            Cost.Multi(
                Cost.Transform(Spend(scaledType(cn("Bar").type)), "PROD"),
                Spend(scaledType(5, cn("Foo").addArgs(cn("Abc").addArgs(cn("Bar"))))),
                Cost.Or(Spend(scaledType(cn("Abc").type)), Spend(scaledType(cn("Xyz").type)))),
            Gated(Max(scaledType(cn("Bar").type)), Remove(scaledType(cn("Xyz").type)))))

    checkBothWays(
        "-> Foo<Abc>. / Wau",
        Action(
            null,
            Instruction.Per(
                Gain(scaledType(cn("Foo").addArgs(cn("Abc"))), AMAP),
                Count(scaledType(cn("Wau").type)))))

    checkBothWays(
        "Qux -> Bar?",
        Action(Spend(scaledType(cn("Qux").type)), Gain(scaledType(cn("Bar").type), OPTIONAL)))

    checkBothWays(
        "Ahh -> 11 Foo",
        Action(Spend(scaledType(cn("Ahh").type)), Gain(scaledType(11, cn("Foo").type))))

    checkBothWays(
        "PROD[Foo / 11 Abc] -> -Foo, (MAX 1 Megacredit OR Foo): 1",
        Action(
            Cost.Transform(
                Cost.Per(Spend(scaledType(cn("Foo").type)), Count(scaledType(11, cn("Abc").type))),
                "PROD"),
            Instruction.Multi(
                Remove(scaledType(cn("Foo").type)),
                Gated(
                    Requirement.Or(
                        Max(scaledType(cn("Megacredit").type)), Min(scaledType(cn("Foo").type))),
                    Gain(scaledType(cn("Megacredit").type))))))

    checkBothWays(
        "-> PROD[1]",
        Action(null, Instruction.Transform(Gain(scaledType(cn("Megacredit").type)), "PROD")))

    checkBothWays(
        "-> @name(Foo<Foo, Ooh, Bar<Ooh>>, Bar<Qux>)",
        Action(
            null,
            Instruction.Custom(
                "name",
                cn("Foo").addArgs(cn("Foo").type, cn("Ooh").type, cn("Bar").addArgs(cn("Ooh"))),
                cn("Bar").addArgs(cn("Qux")))))

    checkBothWays(
        "PROD[1] -> -Ooh / Ooh<Abc, Ahh>",
        Action(
            Cost.Transform(Spend(scaledType(cn("Megacredit").type)), "PROD"),
            Instruction.Per(
                Remove(scaledType(cn("Ooh").type)),
                Count(scaledType(cn("Ooh").addArgs(cn("Abc"), cn("Ahh")))))))

    checkBothWays(
        "Xyz -> 1",
        Action(Spend(scaledType(cn("Xyz").type)), Gain(scaledType(cn("Megacredit").type))))

    checkBothWays(
        "Ooh -> 5 / Abc, 11! / Megacredit, -1?, (1!, (1 Bar FROM Foo, 1: 11 Foo))",
        Action(
            Spend(scaledType(cn("Ooh").type)),
            Instruction.Multi(
                Instruction.Per(
                    Gain(scaledType(5, cn("Megacredit").type)), Count(scaledType(cn("Abc").type))),
                Instruction.Per(
                    Gain(scaledType(11, cn("Megacredit").type), MANDATORY),
                    Count(scaledType(cn("Megacredit").type))),
                Remove(scaledType(cn("Megacredit").type), OPTIONAL),
                Instruction.Multi(
                    Gain(scaledType(cn("Megacredit").type), MANDATORY),
                    Instruction.Multi(
                        Transmute(SimpleFrom(cn("Bar").type, cn("Foo").type), 1),
                        Gated(
                            Min(scaledType(cn("Megacredit").type)),
                            Gain(scaledType(11, cn("Foo").type))))))))

    checkBothWays(
        "Abc -> PROD[-1]",
        Action(
            Spend(scaledType(cn("Abc").type)),
            Instruction.Transform(Remove(scaledType(cn("Megacredit").type)), "PROD")))
  }
}
