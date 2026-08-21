package dev.martianzoo.tfm.data

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class StandardActionDefinitionTest {
  @Test
  fun jsonNameIsTheCanonicalClassName() {
    val definition =
        JsonReader.readActions(
                """
                {
                  actions: [{ name: "ExampleSA", action: "-> Plant" }],
                }
                """
            )
            .single()

    definition.className shouldBe cn("ExampleSA")
    definition.asClassDeclaration.className shouldBe cn("ExampleSA")
  }
}
