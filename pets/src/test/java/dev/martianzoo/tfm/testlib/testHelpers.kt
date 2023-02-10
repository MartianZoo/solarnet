package dev.martianzoo.tfm.testlib

import dev.martianzoo.tfm.pets.ast.TypeExpr.Companion.typeExpr
import org.junit.jupiter.api.assertThrows

public fun te(s: String) = typeExpr(s)

public fun assertFails(message: String, shouldFail: () -> Unit) =
    assertThrows<RuntimeException>(message, shouldFail)

public fun assertFails(shouldFail: () -> Unit) = assertThrows<RuntimeException>(shouldFail)
