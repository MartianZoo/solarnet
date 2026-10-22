package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.Effect
import io.kotest.matchers.collections.shouldContainExactly
import kotlin.test.Test

internal class TfmActionLowererTest {
  @Test
  internal fun `payment classification follows declared inheritance rather than resource names`() {
    val declarations =
        parseClasses(
            """
            CLASS Provider { Alloy -> Foo; Metal -> Bar; MC -> Baz }
            CLASS Alloy : Metal
            ABSTRACT CLASS StandardResource { ABSTRACT CLASS Metal }
            CLASS MC
            """
                .trimIndent()
        )
    TfmActionLowerer.lower(declarations)
        .first()
        .effects
        .shouldContainExactly(
            parse<Effect>(
                "UseAction<This, Action1>: Owed<Class<Alloy>> THEN PaymentScope<Class<This>, Class<Alloy>, Class<Action1>>"
            ),
            parse<Effect>("-PaymentScope<Class<This>, Class<Alloy>, Class<Action1>>: Foo"),
            parse<Effect>(
                "UseAction<This, Action2>: Owed<Class<Metal>> THEN PaymentScope<Class<This>, Class<Metal>, Class<Action2>>"
            ),
            parse<Effect>("-PaymentScope<Class<This>, Class<Metal>, Class<Action2>>: Bar"),
            parse<Effect>("UseAction<This, Action3>: -MC! THEN Baz"),
        )
  }

  @Test
  internal fun `payment lowering preserves absent action failures`() {
    val declaration = parseClasses("CLASS Provider { 2 MC -> Foo }").single()
    TfmActionLowerer.lower(Canon.explicitClassDeclarations + declaration)
        .single { it.className == declaration.className }
        .effects
        .shouldContainExactly(
            parse<Effect>(
                "UseAction<This, Action1>: 2 Owed<Class<MC>> THEN PaymentScope<Class<This>, Class<MC>, Class<Action1>>"
            ),
            parse<Effect>("-PaymentScope<Class<This>, Class<MC>, Class<Action1>>: Foo"),
            parse<Effect>("UseAction<This, Action2>: Die"),
            parse<Effect>("UseAction<This, Action3>: Die"),
        )
  }

  @Test
  internal fun `two authored actions leave only the third slot absent`() {
    val declaration = parseClasses("CLASS Provider { -> Foo; -> Bar }").single()
    TfmActionLowerer.lower(Canon.explicitClassDeclarations + declaration)
        .single { it.className == declaration.className }
        .effects
        .shouldContainExactly(
            parse<Effect>("UseAction<This, Action1>: Foo"),
            parse<Effect>("UseAction<This, Action2>: Bar"),
            parse<Effect>("UseAction<This, Action3>: Die"),
        )
  }

  @Test
  internal fun `standard-resource actions use Terraforming Mars payment scopes`() {
    val declaration =
        parseClasses(
                """
                CLASS Provider {
                  2 MC -> Foo
                  Steel -> Bar
                  X Plant -> X Heat
                }
                """
                    .trimIndent()
            )
            .single()

    TfmActionLowerer.lower(Canon.explicitClassDeclarations + declaration)
        .single { it.className == declaration.className }
        .effects
        .shouldContainExactly(
            parse<Effect>(
                "UseAction<This, Action1>: 2 Owed<Class<MC>> THEN PaymentScope<Class<This>, Class<MC>, Class<Action1>>"
            ),
            parse<Effect>("-PaymentScope<Class<This>, Class<MC>, Class<Action1>>: Foo"),
            parse<Effect>(
                "UseAction<This, Action2>: Owed<Class<Steel>> THEN " +
                    "PaymentScope<Class<This>, Class<Steel>, Class<Action2>>"
            ),
            parse<Effect>("-PaymentScope<Class<This>, Class<Steel>, Class<Action2>>: Bar"),
            parse<Effect>(
                "UseAction<This, Action3>: X Owed<Class<Plant>> THEN " +
                    "PaymentScope<Class<This>, Class<Plant>, Class<Action3>> THEN " +
                    "MAX 0 PaymentScope: (X Heat)"
            ),
        )
  }
}
