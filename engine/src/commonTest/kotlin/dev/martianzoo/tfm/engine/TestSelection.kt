package dev.martianzoo.tfm.engine

public sealed interface TestSelection

internal fun exclude(option: TestOption): TestSelection = ExcludedTestOption(option)
