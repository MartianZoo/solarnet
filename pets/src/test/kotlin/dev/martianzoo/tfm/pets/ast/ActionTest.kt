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
import dev.martianzoo.tfm.pets.ast.Requirement.Max
import dev.martianzoo.tfm.pets.ast.Requirement.Min
import dev.martianzoo.tfm.pets.ast.ScalarAndType.Companion.sat
import dev.martianzoo.tfm.pets.checkBothWays
import dev.martianzoo.tfm.pets.testRoundTrip
import dev.martianzoo.tfm.pets.testSampleStrings
import dev.martianzoo.tfm.testlib.PetGenerator
import org.junit.jupiter.api.Disabled
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

  @Disabled
  @Test
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
                        Spend(sat(cn("Megacredit").type)),
                        Spend(sat(cn("Abc").addArgs(cn("Bar").type)))),
                    Spend(sat(cn("Megacredit").type))),
                Cost.Transform(Spend(sat(cn("Megacredit").type)), "PROD")),
            Gain(sat(cn("Megacredit").type), OPTIONAL)))

    checkBothWays(
        "5, 1 OR (1 OR Bar) OR 1 / Megacredit, 1 OR (1 OR Foo OR Qux) -> 5 / Ooh, " +
            "-Xyz<Qux<Foo<Qux<Foo>>>(HAS 5 Bar)>.",
        Action(
            Cost.Multi(
                Spend(sat(5, cn("Megacredit").type)),
                Cost.Or(
                    Spend(sat(cn("Megacredit").type)),
                    Cost.Or(Spend(sat(cn("Megacredit").type)), Spend(sat(cn("Bar").type))),
                    Cost.Per(Spend(sat(cn("Megacredit").type)), sat(cn("Megacredit").type))),
                Cost.Or(
                    Spend(sat(cn("Megacredit").type)),
                    Cost.Or(
                        Spend(sat(cn("Megacredit").type)),
                        Spend(sat(cn("Foo").type)),
                        Spend(sat(cn("Qux").type))))),
            Instruction.Multi(
                Instruction.Per(Gain(sat(5, cn("Megacredit").type)), sat(cn("Ooh").type)),
                Remove(
                    sat(
                        cn("Xyz")
                            .addArgs(
                                cn("Qux")
                                    .addArgs(cn("Foo").addArgs(cn("Qux").addArgs(cn("Foo").type)))
                                    .refine(Min(sat(5, cn("Bar").type))))),
                    AMAP))))

    checkBothWays(
        "PROD[Bar], 5 Foo<Abc<Bar>>, Abc OR Xyz -> MAX 1 Bar: -Xyz",
        Action(
            Cost.Multi(
                Cost.Transform(Spend(sat(cn("Bar").type)), "PROD"),
                Spend(sat(5, cn("Foo").addArgs(cn("Abc").addArgs(cn("Bar").type)))),
                Cost.Or(Spend(sat(cn("Abc").type)), Spend(sat(cn("Xyz").type)))),
            Gated(Max(sat(cn("Bar").type)), Remove(sat(cn("Xyz").type)))))

    checkBothWays(
        "-> Foo<Abc>. / Wau",
        Action(
            null,
            Instruction.Per(
                Gain(sat(cn("Foo").addArgs(cn("Abc").type)), AMAP), sat(cn("Wau").type))))

    checkBothWays(
        "Qux -> Bar?", Action(Spend(sat(cn("Qux").type)), Gain(sat(cn("Bar").type), OPTIONAL)))

    checkBothWays(
        "Ahh -> 11 Foo", Action(Spend(sat(cn("Ahh").type)), Gain(sat(11, cn("Foo").type))))

    checkBothWays(
        "PROD[Foo / 11 Abc] -> -Foo, (MAX 1 Megacredit OR Foo): 1",
        Action(
            Cost.Transform(Cost.Per(Spend(sat(cn("Foo").type)), sat(11, cn("Abc").type)), "PROD"),
            Instruction.Multi(
                Remove(sat(cn("Foo").type)),
                Gated(
                    Requirement.Or(Max(sat(cn("Megacredit").type)), Min(sat(cn("Foo").type))),
                    Gain(sat(cn("Megacredit").type))))))

    checkBothWays(
        "-> PROD[1]", Action(null, Instruction.Transform(Gain(sat(cn("Megacredit").type)), "PROD")))

    checkBothWays(
        "-> @name(Foo<Foo, Ooh, Bar<Ooh>>, Bar<Qux>)",
        Action(
            null,
            Instruction.Custom(
                "name",
                cn("Foo")
                    .addArgs(cn("Foo").type, cn("Ooh").type, cn("Bar").addArgs(cn("Ooh").type)),
                cn("Bar").addArgs(cn("Qux").type))))

    checkBothWays(
        "PROD[1] -> -Ooh / Ooh<Abc, Ahh>",
        Action(
            Cost.Transform(Spend(sat(cn("Megacredit").type)), "PROD"),
            Instruction.Per(
                Remove(sat(cn("Ooh").type)),
                sat(cn("Ooh").addArgs(cn("Abc").type, cn("Ahh").type)))))

    checkBothWays("Xyz -> 1", Action(Spend(sat(cn("Xyz").type)), Gain(sat(cn("Megacredit").type))))

    checkBothWays(
        "Ooh -> 5 / Abc, 11! / Megacredit, -1?, (1!, (1 Bar FROM Foo, 1: 11 Foo))",
        Action(
            Spend(sat(cn("Ooh").type)),
            Instruction.Multi(
                Instruction.Per(Gain(sat(5, cn("Megacredit").type)), sat(cn("Abc").type)),
                Instruction.Per(
                    Gain(sat(11, cn("Megacredit").type), MANDATORY), sat(cn("Megacredit").type)),
                Remove(sat(cn("Megacredit").type), OPTIONAL),
                Instruction.Multi(
                    Gain(sat(cn("Megacredit").type), MANDATORY),
                    Instruction.Multi(
                        Transmute(SimpleFrom(cn("Bar").type, cn("Foo").type), 1),
                        Gated(Min(sat(cn("Megacredit").type)), Gain(sat(11, cn("Foo").type))))))))

    checkBothWays(
        "Abc -> PROD[-1]",
        Action(
            Spend(sat(cn("Abc").type)),
            Instruction.Transform(Remove(sat(cn("Megacredit").type)), "PROD")))

    checkBothWays(
        "1 / 11 Qux<Bar<Foo, Qux, Foo>> -> PROD[Qux: -1]",
        Action(
            Cost.Per(
                Spend(sat(cn("Megacredit").type)),
                sat(
                    11,
                    cn("Qux")
                        .addArgs(
                            cn("Bar").addArgs(cn("Foo").type, cn("Qux").type, cn("Foo").type)))),
            Instruction.Transform(
                Gated(Min(sat(cn("Qux").type)), Remove(sat(cn("Megacredit").type))), "PROD")))

    checkBothWays(
        "PROD[Foo / 11 Megacredit] / Ooh<Bar, Xyz> -> PROD[-Foo!, @name(Xyz<Bar>)]",
        Action(
            Cost.Per(
                Cost.Transform(
                    Cost.Per(Spend(sat(cn("Foo").type)), sat(11, cn("Megacredit").type)), "PROD"),
                sat(cn("Ooh").addArgs(cn("Bar").type, cn("Xyz").type))),
            Instruction.Transform(
                Instruction.Multi(
                    Remove(sat(cn("Foo").type), MANDATORY),
                    Instruction.Custom("name", cn("Xyz").addArgs(cn("Bar").type))),
                "PROD")))

    checkBothWays(
        "11 Abc -> -11 Foo<Qux>",
        Action(Spend(sat(11, cn("Abc").type)), Remove(sat(11, cn("Foo").addArgs(cn("Qux").type)))))

    checkBothWays(
        "PROD[5] -> Xyz, Bar",
        Action(
            Cost.Transform(Spend(sat(5, cn("Megacredit").type)), "PROD"),
            Instruction.Multi(Gain(sat(cn("Xyz").type)), Gain(sat(cn("Bar").type)))))

    checkBothWays(
        "(1 / Megacredit, 11 Bar) OR ((1 / Megacredit OR (1 OR Bar) OR Ooh OR (1, 1))" +
            " OR PROD[1]) -> PROD[-5 Foo]",
        Action(
            Cost.Or(
                Cost.Multi(
                    Cost.Per(Spend(sat(cn("Megacredit").type)), sat(cn("Megacredit").type)),
                    Spend(sat(11, cn("Bar").type))),
                Cost.Or(
                    Cost.Or(
                        Cost.Per(Spend(sat(cn("Megacredit").type)), sat(cn("Megacredit").type)),
                        Cost.Or(Spend(sat(cn("Megacredit").type)), Spend(sat(cn("Bar").type))),
                        Spend(sat(cn("Ooh").type)),
                        Cost.Multi(
                            Spend(sat(cn("Megacredit").type)), Spend(sat(cn("Megacredit").type)))),
                    Cost.Transform(Spend(sat(cn("Megacredit").type)), "PROD"))),
            Instruction.Transform(Remove(sat(5, cn("Foo").type)), "PROD")))

    checkBothWays(
        "Foo<Foo> OR 1, (5 OR 1) OR (5 OR Foo / 5 Foo OR 1 OR 1 / Megacredit) OR 1 " +
            "OR (Abc OR Bar), 11 Qux -> @name(Qux, Foo), Qux<Qux<Bar>>. / Foo",
        Action(
            Cost.Multi(
                Cost.Or(
                    Spend(sat(cn("Foo").addArgs(cn("Foo").type))),
                    Spend(sat(cn("Megacredit").type))),
                Cost.Or(
                    Cost.Or(
                        Spend(sat(5, cn("Megacredit").type)), Spend(sat(cn("Megacredit").type))),
                    Cost.Or(
                        Spend(sat(5, cn("Megacredit").type)),
                        Cost.Per(Spend(sat(cn("Foo").type)), sat(5, cn("Foo").type)),
                        Spend(sat(cn("Megacredit").type)),
                        Cost.Per(Spend(sat(cn("Megacredit").type)), sat(cn("Megacredit").type))),
                    Spend(sat(cn("Megacredit").type)),
                    Cost.Or(Spend(sat(cn("Abc").type)), Spend(sat(cn("Bar").type)))),
                Spend(sat(11, cn("Qux").type))),
            Instruction.Multi(
                Instruction.Custom("name", cn("Qux").type, cn("Foo").type),
                Instruction.Per(
                    Gain(sat(cn("Qux").addArgs(cn("Qux").addArgs(cn("Bar").type))), AMAP),
                    sat(cn("Foo").type)))))
  }
}
