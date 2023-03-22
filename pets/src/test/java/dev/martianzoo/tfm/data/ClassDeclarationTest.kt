package dev.martianzoo.tfm.data

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.SpecialClassNames.THIS
import dev.martianzoo.tfm.pets.Parsing
import dev.martianzoo.tfm.pets.PureTransformers.actionToEffect
import dev.martianzoo.tfm.pets.ast.Action.Companion.action
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Effect.Companion.effect
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.ScaledExpression.Companion.scaledEx
import dev.martianzoo.tfm.pets.ast.classNames
import dev.martianzoo.tfm.testlib.te
import org.junit.jupiter.api.Test

private class ClassDeclarationTest {
  @Test
  fun testExample() {
    val declText =
        """
          ABSTRACT CLASS Foo<Bar> : Baz<Qux> {
            HAS =1 This
            DEFAULT +Foo<Abc>?
            DEFAULT Foo<Xyz>
            
            This: DoStuff
            Steel -> 5
          }
        """
            .trimIndent()

    val decl: ClassDeclaration = Parsing.parseClassDeclarations(declText).single()

    val foo = cn("Foo")
    val dep = cn("Bar").expr
    val sup = te("Baz<Qux>")
    val inv = Requirement.Exact(scaledEx(1, THIS.expr))
    val eff = effect("This: DoStuff")
    val act = actionToEffect(action("Steel -> 5"), 1)
    val gain = cn("Abc").expr
    val univ = cn("Xyz").expr

    assertThat(decl.shortName).isEqualTo(foo)
    assertThat(decl.className).isEqualTo(foo)
    assertThat(decl.abstract).isTrue()
    assertThat(decl.dependencies).containsExactly(dep)
    assertThat(decl.supertypes).containsExactly(sup)
    assertThat(decl.invariants.map { it.unprocessed }).containsExactly(inv)
    assertThat(decl.effects.map { it.effect.unprocessed }).containsExactly(eff, act)
    assertThat(decl.defaultsDeclaration.gainOnlySpecs).containsExactly(gain)
    assertThat(decl.defaultsDeclaration.universalSpecs).containsExactly(univ)
    assertThat(decl.defaultsDeclaration.gainIntensity).isEqualTo(Intensity.OPTIONAL)
    assertThat(decl.extraNodes).isEmpty()

    assertThat(decl.supertypes.classNames()).containsExactly(cn("Baz"))

    assertThat(decl.allNodes).containsExactly(foo, dep, sup, inv, eff, act, gain, univ)
  }
}
