package dev.martianzoo.tfm.testlib

import org.junit.jupiter.api.assertThrows

public fun assertFails(thing: () -> Unit) = assertThrows<RuntimeException>(thing)
