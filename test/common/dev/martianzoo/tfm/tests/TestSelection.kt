package dev.martianzoo.tfm.tests

import dev.martianzoo.tfm.engine.*

internal sealed interface TestSelection

private fun exclude(option: TestOption): TestSelection = ExcludedTestOption(option)
