package dev.martianzoo.tfm.petaform

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.petaform.Effect.Trigger
import dev.martianzoo.tfm.petaform.Effect.Trigger.Conditional
import dev.martianzoo.tfm.petaform.Effect.Trigger.Now
import dev.martianzoo.tfm.petaform.Effect.Trigger.OnGain
import dev.martianzoo.tfm.petaform.Effect.Trigger.OnRemove
import dev.martianzoo.tfm.petaform.Instruction.Companion.then
import dev.martianzoo.tfm.petaform.Instruction.FromIsRightHere
import dev.martianzoo.tfm.petaform.Instruction.Gain
import dev.martianzoo.tfm.petaform.Instruction.Intensity.MANDATORY
import dev.martianzoo.tfm.petaform.Instruction.Intensity.OPTIONAL
import dev.martianzoo.tfm.petaform.Instruction.Remove
import dev.martianzoo.tfm.petaform.Instruction.Transmute
import dev.martianzoo.tfm.petaform.PetaformParser.parse
import dev.martianzoo.tfm.petaform.Predicate.Max
import dev.martianzoo.tfm.petaform.Predicate.Min
import org.junit.jupiter.api.Test

// Most testing is done by AutoTest
class EffectTest {
  // TODO
  val inputs = """
    Foo: Bar
  """.trimIndent().split('\n')

  @Test fun testSampleStrings() {
    inputs.forEach { testRoundTrip<Effect>(it) }
  }

  @Test fun apiCreation() {
    val effects = listOf(
        Effect(
            Conditional(Now(Min(null, 1)), Max(TypeExpression("Qux", listOf()), 1)),
            Instruction.multi(
                Gain(TypeExpression("Eep", listOf())),
                then(
                    Instruction.multi(
                        then(Remove(TypeExpression("Qux", listOf())), Gain(null, 5)),
                        Gain(TypeExpression("Ooh", listOf()), 1)),
                    Gain(TypeExpression("Qux", listOf())),
                ),
            ),
        ),
        Effect(
            OnGain(TypeExpression("Eep", listOf(TypeExpression("Abc", listOf())))),
            Instruction.or(
                Gain(null, 42),
                Instruction.multi(
                    Transmute(
                        FromIsRightHere(
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
            Conditional(
                OnGain(TypeExpression("Qux", listOf())),
                Min(TypeExpression("Abc", listOf()), 5),
            ),
            Gain(
                TypeExpression("Abc",
                    listOf(),
                    predicate = Min(TypeExpression("Xyz", listOf()), 1)
                )
            ),
        ),
        Effect(
            OnGain(
                TypeExpression("Ahh",
                    listOf(
                        TypeExpression("Ooh", listOf()),
                        TypeExpression("Foo",
                            listOf(),
                            predicate = Predicate.or(
                                Min(TypeExpression("Abc", listOf()), 42),
                                Min(TypeExpression("Xyz", listOf()), 1),
                            ),
                        ),
                        TypeExpression("Qux", listOf()),
                    ),
                ),
            ),
            Remove(
                TypeExpression("Ooh", listOf(), predicate = Min(TypeExpression("Abc", listOf()))),
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
                    Predicate.and(Max(TypeExpression("Abc", listOf()), 0), Max(TypeExpression("Ooh", listOf()), 42)),
                ),
            ),
            Remove(TypeExpression("Wau", listOf()), intensity = MANDATORY),
        ),
        Effect(
            Conditional(
                OnGain(TypeExpression("Ahh", listOf())),
                Predicate.or(
                    Predicate.or(
                        Min(TypeExpression("Abc", listOf()), 42),
                        Min(TypeExpression("Abc", listOf(TypeExpression("Foo", listOf())))),
                        Min(TypeExpression("Foo", listOf()), 1), Min(TypeExpression("Bar", listOf()), 5),
                    ),
                    Min(TypeExpression("Qux", listOf()), 1),
                ),
            ),
            Remove(TypeExpression("Bar", listOf()), 1),
        ),
    )

    // Yes, I'd rather restructure this
    val effectsText = effects.map { it.toString() }
    assertThat(effectsText).containsExactly(
        "NOW 1 IF MAX 1 Qux: Eep, (-Qux THEN 5, 1 Ooh) THEN Qux",
        "Eep<Abc>: 42 OR (1 Xyz FROM Qux, -1) OR 5 Xyz?",
        "PROD[-Foo]: -42",
        "Qux IF 5 Abc: Abc(HAS 1 Xyz)",
        "Ahh<Ooh, Foo(HAS 42 Abc OR 1 Xyz), Qux>: -42 Ooh(HAS Abc)",
        "Qux: 42 Qux / Eep<Qux> OR -Foo<Ooh>!",
        "-Foo<Eep>(HAS MAX 0 Abc, MAX 42 Ooh): -Wau!",
        "Ahh IF (42 Abc OR Abc<Foo> OR 1 Foo OR 5 Bar) OR 1 Qux: -1 Bar",
        ).inOrder()

    assertThat(effectsText.map {parse<Effect>(it)}).containsExactlyElementsIn(effects).inOrder()
  }
}
