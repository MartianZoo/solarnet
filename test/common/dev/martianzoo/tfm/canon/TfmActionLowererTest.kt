package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.Effect
import io.kotest.matchers.collections.shouldContainExactly
import kotlin.test.Test

internal class TfmActionLowererTest {
  @Test
  internal fun `standard-resource actions use Terraforming Mars billing`() {
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

    TfmActionLowerer.lower(declaration)
        .effects
        .shouldContainExactly(
            parse<Effect>(
                "UseAction<This, Action1>: 2 Owed<Class<MC>> THEN Billing<Class<This>, Action1>"
            ),
            parse<Effect>("-Billing<Class<This>, Action1>: Foo"),
            parse<Effect>(
                "UseAction<This, Action2>: Owed<Class<Steel>> THEN " +
                    "Billing<Class<This>, Action2, Class<Steel>>"
            ),
            parse<Effect>("-Billing<Class<This>, Action2>: Bar"),
            parse<Effect>("UseAction<This, Action3>: -X Plant! THEN X Heat"),
        )
  }
}
