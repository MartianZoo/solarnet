package dev.martianzoo.pets.types

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** T13-2 and T13-5: class literals choose classes independently of their component dependencies. */
internal class Spec13RecursiveClassBoundsTest {
  @Test
  internal fun concreteClassLiteralDoesNotDeclareAVariableForItsOpenOwner() {
    val table =
        loadTypes(
            "CLASS Token : Owned",
            "CLASS Receipt<Class<Token>>",
        )

    table.resolve(te("Token")).abstract shouldBe true
    table.resolve(te("Class<Token>")).abstract shouldBe false
    table.getClass(cn("Receipt")).typeVariables shouldBe emptyList()
  }

  @Test
  internal fun recursiveResourceHolderPreservesTheResourceKindAndSharedOwner() {
    // The same cycle occurs between Canon's CardResource and ResourceHolder: a resource's
    // holder accepts its class, and that holder and resource must have the same owner.
    val table =
        loadTypes(
            "CLASS Player1 : Owner",
            "ABSTRACT CLASS Holder<Class<Resource>> : Owned",
            "ABSTRACT CLASS Resource<Holder<Class<This>, @Owner>> : Owned<@Owner>",
            "CLASS Animal : Resource",
            "CLASS Pets : Holder<Class<Animal>>",
        )

    val resourceKind = table.getClass(cn("Holder")).typeVariables.single { it.selectsClass }
    resourceKind.bound shouldBe table.resolve(te("Resource"))
    resourceKind.declaration.groundType shouldBe resourceKind.bound
    val animal = table.resolve(te("Animal<Pets<Player1>>"))
    animal.isSubtypeOf(table.resolve(te("Owned<Player1>"))) shouldBe true
    animal.isSubtypeOf(table.resolve(te("Resource<Holder<Class<Animal>, Player1>>"))) shouldBe true
  }
}
