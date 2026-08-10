package dev.martianzoo.tfm.data

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class StandardActionDefinitionTest {
  @Test
  fun jsonIdIsTheCanonicalClassName() {
    val definition =
        JsonReader.readActions(
                """
                {
                  actions: [{ id: "ExampleSA", action: "-> Plant" }],
                  projects: [],
                }
                """
            )
            .single()

    definition.className shouldBe cn("ExampleSA")
    definition.asClassDeclaration.className shouldBe cn("ExampleSA")
  }
}
