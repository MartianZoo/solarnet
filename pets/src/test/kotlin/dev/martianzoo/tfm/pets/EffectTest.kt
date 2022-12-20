package dev.martianzoo.tfm.pets

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.pets.Effect.Trigger
import dev.martianzoo.tfm.pets.Effect.Trigger.OnGain
import dev.martianzoo.tfm.pets.Effect.Trigger.OnRemove
import dev.martianzoo.tfm.pets.Instruction.Gain
import dev.martianzoo.tfm.pets.Instruction.Intensity.MANDATORY
import dev.martianzoo.tfm.pets.Instruction.Intensity.OPTIONAL
import dev.martianzoo.tfm.pets.Instruction.Remove
import dev.martianzoo.tfm.pets.Instruction.SimpleFrom
import dev.martianzoo.tfm.pets.Instruction.Transmute
import dev.martianzoo.tfm.pets.Parser.parse
import dev.martianzoo.tfm.pets.Requirement.Max
import dev.martianzoo.tfm.pets.Requirement.Min
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
    NOW Abc: 5 Ooh
    NOW Qux OR 5: 5
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
    NOW =0 Eep: 1 Abc FROM Ooh?
    Foo<Ooh>: -Bar / 11 Bar<Foo>
    Foo: Bar(HAS MAX 1 OR MAX 0)!
    NOW 11 Foo OR (1 Bar, 1): Eep?
    Ooh<Wau>: (MAX 1 Ahh<Xyz>: Xyz)
    NOW 5 Eep: -5 Bar, 5 Ooh / 1 Qux
    Xyz: (MAX 5 Qux: 5 Bar FROM Qux!)
    NOW 1 Ooh OR MAX 5 OR Ooh OR 1: 11
    Xyz<Xyz>: -Abc<Foo<Bar>, Qux<Foo>>?
    NOW (MAX 1, 1) OR Abc: -11 Eep<Bar>?
    Xyz(HAS (1 OR Bar) OR =0 Foo): 1., Foo
    PROD[-Qux]: 5 Bar FROM Ooh, -1 Xyz, -11
    NOW Bar OR (Qux OR Foo), MAX 11: -11 Qux
    PROD[Bar<Abc, Wau>]: (5 Xyz: 5 Bar / Abc)
    PROD[-Eep]: (Abc: (1 OR (1 OR -Abc, Foo)))
    NOW Abc: -5 Qux, Foo / Abc, Qux OR Abc, Qux
    Ahh: Qux FROM Foo, Qux: 11 Foo, -5 Abc<Abc>?
    NOW MAX 1: (-5, -Eep) OR Abc<Ooh> OR Bar<Qux>
    NOW 1 Xyz, 11: 11 Bar<Qux<Ooh>, Bar FROM Eep>.
    Wau: Wau(HAS 1 Bar OR Bar<Bar>, MAX 0) FROM Eep
    PROD[-Foo]: ((MAX 0 OR Bar<Foo, Bar<Abc>>): Bar)
    NOW MAX 0 OR =5 Foo: Qux<Ooh> OR (Bar, Foo) OR -5
    PROD[-Bar]: Ooh<Qux<Wau FROM Bar(HAS 1 Foo<Foo>)>>!
    PROD[-Qux]: 11 Bar FROM Eep(HAS =0 Foo<Foo> OR Foo)?
    NOW Qux OR Foo, MAX 1 OR 5 Eep: 5 Foo, 5 Ooh FROM Foo
    NOW Abc OR 1: Abc, (Ahh: 1) OR Bar / Foo, MAX 1: 1 Xyz
    PROD[Abc]: 5: Bar THEN 1 Qux!, Foo<Bar> / Eep<Bar<Qux>>
    Foo<Wau<Xyz>>: 1 Xyz: 1 Bar, 5: (Qux<Foo> FROM Ooh, Bar)
    -Eep<Abc>(HAS Xyz): Ooh, -11 Ahh<Ahh<Foo>>, Abc, Abc<Bar>
    Wau<Ahh, Xyz>: 11 Eep<Xyz<Abc, Foo FROM Ooh>>!, 11 Abc<Qux>
    -Xyz: Ahh, -Xyz, MAX 0 Bar: Ooh, Foo, Abc, -Xyz, 5 Foo, Foo?
    -Foo: Ahh(HAS Foo OR (Foo OR Abc)), Bar / Abc OR (MAX 0: Foo)
    Foo<Ahh>: 11 Bar OR (Abc<Ahh, Qux>, Bar FROM Foo, 1 Qux / Ahh)
    -Abc<Ooh, Abc(HAS 11 Bar)>: Foo, -Foo, Bar<Bar, Qux<Xyz>, Ahh>!
    PROD[Abc]: 1 Eep<Bar>, Foo / 1 Bar, Foo THEN Abc / Abc, 5 Abc, 5
    NOW Eep<Ooh, Bar>: 5 Bar<Xyz<Abc>, Qux FROM Bar, Xyz>, 5 Qux<Bar>
    Eep<Eep, Qux<Wau, Foo<Bar>>, Eep>: 5 Foo<Ahh<Abc<Foo, Abc>, Ahh>>?
    -Xyz<Ooh<Ahh, Foo<Xyz, Ooh>>>: Bar<Bar>, -1 Bar<Ahh<Eep<Foo, Qux>>>
    -Abc<Xyz>: Foo!, -Bar / Foo, Bar<Qux>, 5 Foo / Bar(HAS Bar) OR 1 Foo
    PROD[Ahh]: Ooh?, Abc. / Ahh, Xyz FROM Foo, -Qux!, 5 Ahh<Qux> FROM Qux
    NOW MAX 0 Ooh: Ooh, Bar<Xyz>, -Bar<Foo> / Bar OR Abc / Foo<Bar>, 11 Abc?
    -Ahh<Eep<Foo, Foo>, Xyz<Qux>(HAS 11 Bar<Ahh, Bar>)>: 5 Abc?, Eep?, 11 Bar
    PROD[Foo]: 1 Ooh<Qux<Foo>, Bar<Qux> FROM Foo> / 11 Bar<Ooh, Abc<Foo<Foo>>>
    Xyz<Ahh, Ooh<Foo, Bar<Ooh<Foo, Bar>>>, Eep>: 5 Xyz<Ahh<Ahh>> FROM Foo<Foo>?
    Wau<Xyz<Qux(HAS Bar)>, Foo<Foo<Qux, Qux>>>: 5 Ahh OR -Bar / Bar, 5 Ahh / Ooh
    NOW (Ahh OR Abc) OR (((Bar, =0) OR Foo) OR (MAX 0, 5)): Foo<Bar>, Ooh, Foo, 5
    Eep<Xyz<Foo, Ooh<Ooh, Bar>>>: 11 Abc(HAS Bar) FROM Wau, Foo, 5 Foo, 1 Qux<Ooh>
    -Ooh<Foo<Xyz, Foo, Qux>>: 5 Qux<Ooh, Xyz, Bar> OR 5?, =0 Qux: -Bar, 5: Foo<Abc>
  """.trimIndent()

  @Test fun testSampleStrings() {
    val pass = testSampleStrings<Effect>(inputs)
    assertThat(pass).isTrue()
  }

  @Test fun apiCreation() {
    val effects = listOf(
        Effect(
            OnGain(TypeExpression("Eep", listOf(TypeExpression("Abc", listOf())))),
            Instruction.or(
                Gain(null, 42),
                Instruction.multi(
                    Transmute(
                        SimpleFrom(
                            TypeExpression("Xyz", listOf()),
                            TypeExpression("Qux", listOf())),
                        1),
                    Remove(null, 1),
                ),
                Gain(TypeExpression("Xyz", listOf()), 5, OPTIONAL),
            ),
        ),
        Effect(Trigger.Prod(OnRemove(TypeExpression("Foo", listOf()))), Remove(null, 42)),
        Effect(
            OnGain(
                TypeExpression("Ahh",
                    listOf(
                        TypeExpression("Ooh", listOf()),
                        TypeExpression("Foo",
                            listOf(),
                            requirement = Requirement.or(
                                Min(TypeExpression("Abc", listOf()), 42),
                                Min(TypeExpression("Xyz", listOf()), 1),
                            ),
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
            Instruction.or(
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
                    Requirement.and(Max(TypeExpression("Abc", listOf()), 0), Max(TypeExpression("Ooh", listOf()), 42)),
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
