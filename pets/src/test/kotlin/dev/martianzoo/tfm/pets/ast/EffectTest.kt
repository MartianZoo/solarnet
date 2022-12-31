package dev.martianzoo.tfm.pets.ast

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.pets.PetsParser.parse
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.OnGain
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.OnRemove
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.Prod
import dev.martianzoo.tfm.pets.ast.FromExpression.SimpleFrom
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.OPTIONAL
import dev.martianzoo.tfm.pets.ast.Instruction.Remove
import dev.martianzoo.tfm.pets.ast.Instruction.Transmute
import dev.martianzoo.tfm.pets.ast.Requirement.Max
import dev.martianzoo.tfm.pets.ast.Requirement.Min
import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion.te
import org.junit.jupiter.api.Test

// Most testing is done by AutomatedTest
class EffectTest {

  // TODO
  val inputs = """
    Bar: 5
    -Eep: Qux
    Abc: 11, 5
    Ooh: 5, Abc
    -Foo: 5 Xyz.
    -Abc: 1, -Xyz
    PROD[Xyz]: 5 Foo
    Foo: -5 Ooh? OR 1
    Xyz: Xyz THEN -Bar
    PROD[-Abc]: -11 Bar
    PROD[-Ooh]: (Wau: 5)
    Abc<Abc>: Bar / 5 Abc
    Wau: -1 Foo(HAS MAX 0)
    -Xyz: Foo<Foo>! / 1 Wau
    PROD[Wau]: Qux / Qux, Qux
    PROD[-Xyz]: 1 Ahh FROM Qux
    Foo<Ooh>: -Bar / 11 Bar<Foo>
    Foo: Bar(HAS MAX 1 OR MAX 0)!
    Ooh<Wau>: (MAX 1 Ahh<Xyz>: Xyz)
    Xyz: (MAX 5 Qux: 5 Bar FROM Qux!)
    Xyz<Xyz>: -Abc<Foo<Bar>, Qux<Foo>>?
    Xyz(HAS (1 OR Bar) OR =0 Foo): 1., Foo
    PROD[-Qux]: 5 Bar FROM Ooh, -1 Xyz, -11
    PROD[Bar<Abc, Wau>]: (5 Xyz: 5 Bar / Abc)
    PROD[-Eep]: (Abc: (1 OR (1 OR -Abc, Foo)))
    Ahh: Qux FROM Foo, Qux: 11 Foo, -5 Abc<Abc>?
    Wau: Wau(HAS 1 Bar OR Bar<Bar>, MAX 0) FROM Eep
    PROD[-Foo]: ((MAX 0 OR Bar<Foo, Bar<Abc>>): Bar)
    PROD[-Bar]: Ooh<Qux<Wau FROM Bar(HAS 1 Foo<Foo>)>>!
    PROD[-Qux]: 11 Bar FROM Eep(HAS =0 Foo<Foo> OR Foo)?
    PROD[Abc]: 5: Bar THEN 1 Qux!, Foo<Bar> / Eep<Bar<Qux>>
    Foo<Wau<Xyz>>: 1 Xyz: 1 Bar, 5: (Qux<Foo> FROM Ooh, Bar)
    -Eep<Abc>(HAS Xyz): Ooh, -11 Ahh<Ahh<Foo>>, Abc, Abc<Bar>
    Wau<Ahh, Xyz>: 11 Eep<Xyz<Abc, Foo FROM Ooh>>!, 11 Abc<Qux>
    -Xyz: Ahh, -Xyz, MAX 0 Bar: Ooh, Foo, Abc, -Xyz, 5 Foo, Foo?
    -Foo: Ahh(HAS Foo OR (Foo OR Abc)), Bar / Abc OR (MAX 0: Foo)
    Foo<Ahh>: 11 Bar OR (Abc<Ahh, Qux>, Bar FROM Foo, 1 Qux / Ahh)
    -Abc<Ooh, Abc(HAS 11 Bar)>: Foo, -Foo, Bar<Bar, Qux<Xyz>, Ahh>!
    PROD[Abc]: 1 Eep<Bar>, Foo / 1 Bar, Foo THEN Abc / Abc, 5 Abc, 5
    Eep<Eep, Qux<Wau, Foo<Bar>>, Eep>: 5 Foo<Ahh<Abc<Foo, Abc>, Ahh>>?
    -Xyz<Ooh<Ahh, Foo<Xyz, Ooh>>>: Bar<Bar>, -1 Bar<Ahh<Eep<Foo, Qux>>>
    -Abc<Xyz>: Foo!, -Bar / Foo, Bar<Qux>, 5 Foo / Bar(HAS Bar) OR 1 Foo
    PROD[Ahh]: Ooh?, Abc. / Ahh, Xyz FROM Foo, -Qux!, 5 Ahh<Qux> FROM Qux
    -Ahh<Eep<Foo, Foo>, Xyz<Qux>(HAS 11 Bar<Ahh, Bar>)>: 5 Abc?, Eep?, 11 Bar
    PROD[Foo]: 1 Ooh<Qux<Foo>, Bar<Qux> FROM Foo> / 11 Bar<Ooh, Abc<Foo<Foo>>>
    Xyz<Ahh, Ooh<Foo, Bar<Ooh<Foo, Bar>>>, Eep>: 5 Xyz<Ahh<Ahh>> FROM Foo<Foo>?
    Wau<Xyz<Qux(HAS Bar)>, Foo<Foo<Qux, Qux>>>: 5 Ahh OR -Bar / Bar, 5 Ahh / Ooh
    Eep<Xyz<Foo, Ooh<Ooh, Bar>>>: 11 Abc(HAS Bar) FROM Wau, Foo, 5 Foo, 1 Qux<Ooh>
    -Ooh<Foo<Xyz, Foo, Qux>>: 5 Qux<Ooh, Xyz, Bar> OR 5?, =0 Qux: -Bar, 5: Foo<Abc>
  """.trimIndent()

  @Test fun testSampleStrings() {
    val pass = dev.martianzoo.tfm.pets.testSampleStrings<Effect>(inputs)
    assertThat(pass).isTrue()
  }

  @Test fun apiCreation() {
    Effect(
        OnGain(
            te("Abc",
                te("Foo"),
                te("Bar", te("Qux", te("Foo"))),
                te("Foo")
            ),
        ),
        Instruction.Multi(
            Instruction.Multi(
                Remove(te("Foo")),
                Gain(te("Ahh")),
            ),
            Instruction.Multi(
                Instruction.Or(
                    Instruction.Per(
                        Gain(te("Foo")),
                        QuantifiedExpression(null, 5),
                    ),
                    Gain(te("Bar")),
                ),
                Instruction.Or(
                    Gain(null, 1),
                    Gain(te("Bar")),
                ),
            ),
            Gain(te("Bar"), 1),
        ),
    )


    val effects = listOf(
        Effect(
            OnGain(TypeExpression("Eep", listOf(TypeExpression("Abc", listOf())))),
            Instruction.Or(
                Gain(null, 42),
                Instruction.Multi(
                    Transmute(
                        SimpleFrom(
                            TypeExpression("Xyz", listOf()),
                            TypeExpression("Qux", listOf())
                        ),
                        1),
                    Remove(null, 1),
                ),
                Gain(TypeExpression("Xyz", listOf()), 5, OPTIONAL),
            ),
        ),
        Effect(Prod(OnRemove(TypeExpression("Foo", listOf()))), Remove(null, 42)),
        Effect(
            OnGain(
                TypeExpression("Ahh",
                    listOf(
                        TypeExpression("Ooh", listOf()),
                        TypeExpression("Foo",
                            listOf(),
                            requirement = Requirement.Or(setOf(
                                Min(TypeExpression("Abc", listOf()), 42),
                                Min(TypeExpression("Xyz", listOf()), 1),
                            )),
                        ),
                        TypeExpression("Qux", listOf()),
                    ),
                ),
            ),
            Remove(
                TypeExpression("Ooh", listOf(), requirement = Min(TypeExpression("Abc", listOf()))),
                42),
        ),
        Effect(
            OnGain(TypeExpression("Qux", listOf())),
            Instruction.Or(
                Instruction.Per(
                    Gain(TypeExpression("Qux", listOf()), 42),
                    QuantifiedExpression(TypeExpression("Eep", listOf(TypeExpression("Qux", listOf())))),
                ),
                Remove(
                    TypeExpression("Foo", listOf(TypeExpression("Ooh", listOf()))),
                    intensity = MANDATORY,
                ),
            ),
        ),
        Effect(
            OnRemove(
                TypeExpression("Foo",
                    listOf(TypeExpression("Eep", listOf())),
                    Requirement.And(listOf(Max(TypeExpression("Abc", listOf()), 0), Max(TypeExpression("Ooh", listOf()), 42))),
                ),
            ),
            Remove(TypeExpression("Wau", listOf()), intensity = MANDATORY),
        ),
    )

    // Yes, I'd rather restructure this
    val effectsText = effects.map { it.toString() }
    assertThat(effectsText).containsExactly(
        "Eep<Abc>: 42 OR (1 Xyz FROM Qux, -1) OR 5 Xyz?",
        "PROD[-Foo]: -42",
        "Ahh<Ooh, Foo(HAS 42 Abc OR 1 Xyz), Qux>: -42 Ooh(HAS Abc)",
        "Qux: 42 Qux / Eep<Qux> OR -Foo<Ooh>!",
        "-Foo<Eep>(HAS MAX 0 Abc, MAX 42 Ooh): -Wau!",
        ).inOrder()

    assertThat(effectsText.map {parse<Effect>(it)}).containsExactlyElementsIn(effects).inOrder()
  }
}
