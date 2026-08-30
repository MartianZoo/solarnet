package dev.martianzoo.tfm.tests

import dev.martianzoo.tfm.engine.*

internal sealed interface TestSelection

internal fun exclude(option: TestOption): TestSelection = ExcludedTestOption(option)
