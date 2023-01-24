package dev.martianzoo.tfm.testlib

import org.junit.jupiter.api.assertThrows

public fun assertFails(message: String, shouldFail: () -> Unit) =
    assertThrows<RuntimeException>(message, shouldFail)

public fun assertFails(shouldFail: () -> Unit) = assertThrows<RuntimeException>(shouldFail)
