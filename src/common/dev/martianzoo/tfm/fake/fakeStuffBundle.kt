package dev.martianzoo.tfm.fake

import dev.martianzoo.tfm.canon.StandardFormBundle

internal val fakeStuffBundle: StandardFormBundle =
    StandardFormBundle(
        name = "FakeStuffBundle",
        resourceFilenames = FakeCanonResources.filenames("bundles/FakeStuffBundle"),
        resourceReader = FakeCanonResources::read,
    )
