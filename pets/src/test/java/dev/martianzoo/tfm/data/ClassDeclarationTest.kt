package dev.martianzoo.tfm.data

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.SpecialClassNames.COMPONENT
import dev.martianzoo.tfm.api.SpecialClassNames.THIS
import dev.martianzoo.tfm.pets.AstTransforms.actionToEffect
import dev.martianzoo.tfm.pets.Parsing
import dev.martianzoo.tfm.pets.ast.Action.Companion.action
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.classNames
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Effect.Companion.effect
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.ScaledTypeExpr.Companion.scaledType
import dev.martianzoo.tfm.testlib.assertFails
import dev.martianzoo.tfm.testlib.te
import org.junit.jupiter.api.Test

private class ClassDeclarationTest {
  @Test
  fun testValidate() {
    assertFails { ClassDeclaration(COMPONENT, supertypes = setOf(te("Foo<Bar>"))).validate() }
    assertFails { ClassDeclaration(cn("NotComponent")).validate() }

    val cd =
        ClassDeclaration(cn("NotComponent"), supertypes = setOf(COMPONENT.type, te("Baz<Qux>")))
    assertFails { cd.validate() }
  }

  @Test
  fun testExample() {
    val declText =
        """
          ABSTRACT CLASS Foo<Bar>(HAS MAX 3 Blorp) : Baz<Qux> {
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
    val dep = cn("Bar").type
    val sup = te("Baz<Qux>")
    val topInv = Requirement.Max(scaledType(3, cn("Blorp").type))
    val otherInv = Requirement.Exact(scaledType(1, THIS.type))
    val eff = effect("This: DoStuff")
    val act = actionToEffect(action("Steel -> 5"), 1)
    val gain = cn("Abc").type
    val univ = cn("Xyz").type

    assertThat(decl.id).isEqualTo(foo)
    assertThat(decl.className).isEqualTo(foo)
    assertThat(decl.abstract).isTrue()
    assertThat(decl.dependencies.map { it.typeExpr }).containsExactly(dep)
    assertThat(decl.supertypes).containsExactly(sup)
    assertThat(decl.topInvariant).isEqualTo(topInv)
    assertThat(decl.otherInvariants).containsExactly(otherInv)
    assertThat(decl.effectsRaw).containsExactly(eff, act)
    assertThat(decl.defaultsDeclaration.gainOnlySpecs).containsExactly(gain)
    assertThat(decl.defaultsDeclaration.universalSpecs).containsExactly(univ)
    assertThat(decl.defaultsDeclaration.gainIntensity).isEqualTo(Intensity.OPTIONAL)
    assertThat(decl.extraNodes).isEmpty()

    assertThat(decl.supertypes.classNames()).containsExactly(cn("Baz"))

    assertThat(decl.allNodes).containsExactly(foo, dep, sup, topInv, otherInv, eff, act, gain, univ)
  }
}
