package dev.martianzoo.tfm.engine

internal sealed interface TestSelection

internal fun exclude(option: TestOption): TestSelection = ExcludedTestOption(option)
