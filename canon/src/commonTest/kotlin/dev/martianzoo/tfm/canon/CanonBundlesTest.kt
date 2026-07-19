package dev.martianzoo.tfm.canon

import dev.martianzoo.api.SystemClasses.COMPONENT
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.bundles.System.System
import dev.martianzoo.tfm.canon.bundles.TerraformingMars.TerraformingMars
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class CanonBundlesTest {
  @Test
  fun systemRulesetOwnsTheSystemDeclarations() {
    Canon.rulesets.shouldContain(System)
    Canon.classDeclaration(COMPONENT) shouldBe System.classDeclaration(COMPONENT)
  }

  @Test
  fun terraformingMarsRulesetOwnsTheCoreGameDeclarations() {
    Canon.rulesets.shouldContain(TerraformingMars)
    Canon.classDeclaration(cn("TerraformingMars")) shouldBe
        TerraformingMars.classDeclaration(cn("TerraformingMars"))
  }
}
