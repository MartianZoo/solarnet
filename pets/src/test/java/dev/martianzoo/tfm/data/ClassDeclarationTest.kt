package dev.martianzoo.tfm.data

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.api.SpecialClassNames.THIS
import dev.martianzoo.tfm.pets.HasClassName.Companion.classNames
import dev.martianzoo.tfm.pets.Parsing
import dev.martianzoo.tfm.pets.Parsing.parse
import dev.martianzoo.tfm.pets.Transforming.actionToEffect
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.ScaledExpression.Companion.scaledEx
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

    val decl: ClassDeclaration = Parsing.parseClasses(declText).single()

    val foo = cn("Foo")
    val dep = cn("Bar").expression
    val sup = te("Baz<Qux>")

    val inv: Requirement = Requirement.Exact(scaledEx(1, THIS.expression))
    val eff: Effect = parse<Effect>("This: DoStuff")
    val act = actionToEffect(parse("Steel -> 5"), 1)
    val gain = cn("Abc").expression
    val univ = cn("Xyz").expression

    assertThat(decl.shortName).isEqualTo(foo)
    assertThat(decl.className).isEqualTo(foo)
    assertThat(decl.abstract).isTrue()
    assertThat(decl.dependencies).containsExactly(dep)
    assertThat(decl.supertypes).containsExactly(sup)
    assertThat(decl.invariants).containsExactly(inv)
    assertThat(decl.effects).containsExactly(eff, act)
    assertThat(decl.defaultsDeclaration.gainOnly.specs).containsExactly(gain)
    assertThat(decl.defaultsDeclaration.universal.specs).containsExactly(univ)
    assertThat(decl.defaultsDeclaration.gainOnly.intensity).isEqualTo(Intensity.OPTIONAL)
    assertThat(decl.extraNodes).isEmpty()

    assertThat(decl.supertypes.classNames()).containsExactly(cn("Baz"))

    assertThat(decl.allNodes).containsExactly(foo, dep, sup, inv, eff, act, gain, univ)
  }
}
