package dev.martianzoo.state

import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.api.Exceptions.InvalidPetDefinitionException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect.Trigger.IfTrigger
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
                          CLASS Source { This IF Class<Flag>: Product }
                          CLASS Mixed { This IF Class<Flag>, RuntimeDependency: Product }
                          CLASS GatedSource { This: (Class<Flag>: Product) }
                          CLASS ReachabilityOnly { This IF Selected: ReachableProduct }
                          CLASS InvariantOnly { This IF InvariantSelected: InvariantProduct }
                          CLASS ModuleSource { This IF ModuleSelected: ModuleProduct }
                          CLASS ModuleGated { This: (ModuleSelected: ModuleGatedProduct) }
                          CLASS ClassLiteralThis { This IF Class<This>: ClassLiteralProduct }
                          CLASS Flag
                          CLASS Product
                          CLASS RuntimeDependency
                          CLASS ReachableProduct
                          CLASS InvariantProduct
                          CLASS ModuleProduct
                          CLASS ModuleGatedProduct
                          CLASS ClassLiteralProduct
                          CLASS Selected
                          CLASS InvariantSelected { HAS =1 This }
                          CLASS ModuleSelected
                          """
                              .trimIndent()
                      )
              )
          )
          .loadEverything()

  @Test
  internal fun `completed views share master identities but keep local classes and selections separate`() {
    val declarations = PremiseClassTable(master, parseClasses("CLASS Local : Domain").toSet())
    val roots =
        setOf(
            cn("Source"),
            cn("Mixed"),
            cn("GatedSource"),
            cn("ReachabilityOnly"),
            cn("InvariantOnly"),
            cn("ModuleSource"),
            cn("ModuleGated"),
            cn("ClassLiteralThis"),
            cn("Selected"),
            cn("InvariantSelected"),
            cn("ModuleSelected"),
            cn("Local"),
        )
    val dormant =
        ClassLoader.forPremise(
            declarations,
            roots,
            exactCount = { expression, _ ->
              when (expression.className) {
                cn("Selected") -> 1
                else -> null
              }
            },
            selectedModuleCount = { expression, _ ->
              1.takeIf { expression.className == cn("ModuleSelected") }
            },
        )
    val active =
        ClassLoader.forPremise(
            declarations,
            roots + cn("Flag"),
            additionalRequiredClasses = { declaration ->
              if (declaration.className == cn("Source")) setOf(cn("RuntimeDependency"))
              else emptySet()
            },
            exactCount = { expression, _ ->
              when (expression.className) {
                cn("Selected") -> 1
                else -> null
              }
            },
            selectedModuleCount = { expression, _ ->
              1.takeIf { expression.className == cn("ModuleSelected") }
            },
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

    master.effects(master.getClass(cn("Source"))).single().toString() shouldBe
        "This IF Class<Flag>: Product"
    dormant.effects(dormant.getClass(cn("Source"))) shouldBe emptyList()
    active.effects(active.getClass(cn("Source"))).single().toString() shouldBe "This: Product"
    dormant.effects(dormant.getClass(cn("Mixed"))) shouldBe emptyList()
    active.effects(active.getClass(cn("Mixed"))).single().toString() shouldBe
        "This IF RuntimeDependency: Product"
    dormant.effects(dormant.getClass(cn("GatedSource"))).single().toString() shouldBe
        "This: (Class<Flag>: Product)"
    assertSame(
        master.effects(master.getClass(cn("GatedSource"))).single(),
        dormant.effects(dormant.getClass(cn("GatedSource"))).single(),
    )
    active.effects(active.getClass(cn("GatedSource"))).single().toString() shouldBe "This: Product"
    active.effects(active.getClass(cn("ReachabilityOnly"))).single().toString() shouldBe
        "This IF Selected: ReachableProduct"
    active.effects(active.getClass(cn("InvariantOnly"))).single().toString() shouldBe
        "This IF InvariantSelected: InvariantProduct"
    active.effects(active.getClass(cn("ModuleSource"))).single().toString() shouldBe
        "This: ModuleProduct"
    active.effects(active.getClass(cn("ModuleGated"))).single().toString() shouldBe
        "This: ModuleGatedProduct"
    (active.effects(active.getClass(cn("ClassLiteralThis"))).single().trigger is IfTrigger) shouldBe
        true
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
