package dev.martianzoo.state

import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.api.Exceptions.InvalidPetDefinitionException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.pets.systemClassDeclarations
import dev.martianzoo.pets.types.ClassLoader
import dev.martianzoo.pets.types.PremiseClassTable
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.test.assertNotSame
import kotlin.test.assertSame

/** Exercises the complete Pets construction boundary from a consuming module. */
internal class ClassTableConstructionTest {
  private val master =
      ClassLoader(
              ClassDeclaration.indexByName(
                  systemClassDeclarations +
                      parseClasses(
                          """
                          ABSTRACT CLASS Domain
                          CLASS Source { This IF Flag: Product }
                          CLASS Flag
                          CLASS Product
                          CLASS RuntimeDependency
                          """
                              .trimIndent()
                      )
              )
          )
          .loadEverything()

  @Test
  internal fun `completed views share master identities but keep local classes and selections separate`() {
    val declarations = PremiseClassTable(master, parseClasses("CLASS Local : Domain").toSet())
    val roots = setOf(cn("Source"), cn("Local"))
    val dormant =
        ClassLoader.forPremise(
            declarations,
            roots,
            exactCount = { expression, _ -> if (expression.className == cn("Flag")) 0 else null },
        )
    val active =
        ClassLoader.forPremise(
            declarations,
            roots,
            additionalRequiredClasses = { declaration ->
              if (declaration.className == cn("Source")) setOf(cn("RuntimeDependency"))
              else emptySet()
            },
            exactCount = { expression, _ -> if (expression.className == cn("Flag")) 1 else null },
        )

    assertSame(master.getClass(cn("Source")), active.getClass(cn("Source")))
    assertSame(master.getClass(cn("Product")), dormant.getClass(cn("Product")))
    assertNotSame(dormant.getClass(cn("Local")), active.getClass(cn("Local")))
    dormant.allClassNames.contains(cn("Local")) shouldBe true
    active.allSubclasses(master.getClass(cn("Domain"))).map { it.className }.toSet() shouldBe
        setOf(cn("Domain"), cn("Local"))
    dormant.allClassNames.contains(cn("Product")) shouldBe false
    active.allClassNames.contains(cn("Product")) shouldBe true
    dormant.allClassNames.contains(cn("RuntimeDependency")) shouldBe false
    active.allClassNames.contains(cn("RuntimeDependency")) shouldBe true
  }

  @Test
  internal fun `invalid local declarations fail even outside the selected roots`() {
    val declarations =
        PremiseClassTable(master, parseClasses("CLASS Invalid { Ok: Product }").toSet())

    shouldThrow<InvalidPetDefinitionException> {
      ClassLoader.forPremise(declarations, setOf(cn("Source")))
    }
  }
}
