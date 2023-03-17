package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.tfm.pets.ast.Action.Cost
import dev.martianzoo.tfm.pets.ast.Action.Cost.Spend
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.FromExpression.SimpleFrom
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.Instruction.Gated
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.AMAP
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.OPTIONAL
import dev.martianzoo.tfm.pets.ast.Instruction.Remove
import dev.martianzoo.tfm.pets.ast.Instruction.Transmute
import dev.martianzoo.tfm.pets.ast.Metric.Count
import dev.martianzoo.tfm.pets.ast.Metric.Scaled
import dev.martianzoo.tfm.pets.ast.Requirement.Max
import dev.martianzoo.tfm.pets.ast.Requirement.Min
import dev.martianzoo.tfm.pets.ast.ScaledExpression.Companion.scaledEx
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
                        Spend(scaledEx(cn("Megacredit").expr)),
                        Spend(scaledEx(cn("Abc").addArgs(cn("Bar"))))),
                    Spend(scaledEx(cn("Megacredit").expr))),
                Cost.Transform(Spend(scaledEx(cn("Megacredit").expr)), "PROD")),
            Gain(scaledEx(cn("Megacredit").expr), OPTIONAL)))

    checkBothWays(
        "5, 1 OR (1 OR Bar) OR 1 / Megacredit, 1 OR (1 OR Foo OR Qux) -> 5 / Ooh, " +
            "-Xyz<Qux<Foo<Qux<Foo>>>(HAS 5 Bar)>.",
        Action(
            Cost.Multi(
                Spend(scaledEx(5, cn("Megacredit").expr)),
                Cost.Or(
                    Spend(scaledEx(cn("Megacredit").expr)),
                    Cost.Or(
                        Spend(scaledEx(cn("Megacredit").expr)), Spend(scaledEx(cn("Bar").expr))),
                    Cost.Per(
                        Spend(scaledEx(cn("Megacredit").expr)),
                        Count(cn("Megacredit").expr))),
                Cost.Or(
                    Spend(scaledEx(cn("Megacredit").expr)),
                    Cost.Or(
                        Spend(scaledEx(cn("Megacredit").expr)),
                        Spend(scaledEx(cn("Foo").expr)),
                        Spend(scaledEx(cn("Qux").expr))))),
            Instruction.Multi(
                Instruction.Per(
                    Gain(scaledEx(5, cn("Megacredit").expr)), Count(cn("Ooh").expr)),
                Remove(
                    scaledEx(
                        cn("Xyz")
                            .addArgs(
                                cn("Qux")
                                    .addArgs(cn("Foo").addArgs(cn("Qux").addArgs(cn("Foo"))))
                                    .refine(Min(scaledEx(5, cn("Bar").expr))))),
                    AMAP))))

    checkBothWays(
        "PROD[Bar], 5 Foo<Abc<Bar>>, Abc OR Xyz -> MAX 1 Bar: -Xyz",
        Action(
            Cost.Multi(
                Cost.Transform(Spend(scaledEx(cn("Bar").expr)), "PROD"),
                Spend(scaledEx(5, cn("Foo").addArgs(cn("Abc").addArgs(cn("Bar"))))),
                Cost.Or(Spend(scaledEx(cn("Abc").expr)), Spend(scaledEx(cn("Xyz").expr)))),
            Gated(Max(scaledEx(cn("Bar").expr)), true, Remove(scaledEx(cn("Xyz").expr)))))

    checkBothWays(
        "-> Foo<Abc>. / Wau",
        Action(
            null,
            Instruction.Per(
                Gain(scaledEx(cn("Foo").addArgs(cn("Abc"))), AMAP),
                Count((cn("Wau").expr)))))

    checkBothWays(
        "Qux -> Bar?",
        Action(Spend(scaledEx(cn("Qux").expr)), Gain(scaledEx(cn("Bar").expr), OPTIONAL)))

    checkBothWays(
        "Ahh -> 11 Foo",
        Action(Spend(scaledEx(cn("Ahh").expr)), Gain(scaledEx(11, cn("Foo").expr))))

    checkBothWays(
        "PROD[Foo / 11 Abc] -> -Foo, (MAX 1 Megacredit OR Foo): 1",
        Action(
            Cost.Transform(
                Cost.Per(Spend(scaledEx(cn("Foo").expr)), Scaled(11, Count(cn("Abc").expr))),
                "PROD"),
            Instruction.Multi(
                Remove(scaledEx(cn("Foo").expr)),
                Gated(
                    Requirement.Or(
                        Max(scaledEx(cn("Megacredit").expr)), Min(scaledEx(cn("Foo").expr))),
                    true,
                    Gain(scaledEx(cn("Megacredit").expr))))))

    checkBothWays(
        "-> PROD[1]",
        Action(null, Instruction.Transform(Gain(scaledEx(cn("Megacredit").expr)), "PROD")))

    checkBothWays(
        "-> @name(Foo<Foo, Ooh, Bar<Ooh>>, Bar<Qux>)",
        Action(
            null,
            Instruction.Custom(
                "name",
                cn("Foo").addArgs(cn("Foo").expr, cn("Ooh").expr, cn("Bar").addArgs(cn("Ooh"))),
                cn("Bar").addArgs(cn("Qux")))))

    checkBothWays(
        "PROD[1] -> -Ooh / Ooh<Abc, Ahh>",
        Action(
            Cost.Transform(Spend(scaledEx(cn("Megacredit").expr)), "PROD"),
            Instruction.Per(
                Remove(scaledEx(cn("Ooh").expr)),
                Count(cn("Ooh").addArgs(cn("Abc"), cn("Ahh"))))))

    checkBothWays(
        "Xyz -> 1", Action(Spend(scaledEx(cn("Xyz").expr)), Gain(scaledEx(cn("Megacredit").expr))))

    checkBothWays(
        "Ooh -> 5 / Abc, 11! / Megacredit, -1?, (1!, (1 Bar FROM Foo, 1 ?: 11 Foo))",
        Action(
            Spend(scaledEx(cn("Ooh").expr)),
            Instruction.Multi(
                Instruction.Per(
                    Gain(scaledEx(5, cn("Megacredit").expr)), Count(cn("Abc").expr)),
                Instruction.Per(
                    Gain(scaledEx(11, cn("Megacredit").expr), MANDATORY),
                    Count(cn("Megacredit").expr)),
                Remove(scaledEx(cn("Megacredit").expr), OPTIONAL),
                Instruction.Multi(
                    Gain(scaledEx(cn("Megacredit").expr), MANDATORY),
                    Instruction.Multi(
                        Transmute(SimpleFrom(cn("Bar").expr, cn("Foo").expr), 1),
                        Gated(
                            Min(scaledEx(cn("Megacredit").expr)),
                            false,
                            Gain(scaledEx(11, cn("Foo").expr))))))))

    checkBothWays(
        "Abc -> PROD[-1]",
        Action(
            Spend(scaledEx(cn("Abc").expr)),
            Instruction.Transform(Remove(scaledEx(cn("Megacredit").expr)), "PROD")))
  }
}
