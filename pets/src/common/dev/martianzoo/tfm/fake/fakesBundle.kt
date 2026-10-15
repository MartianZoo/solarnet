package dev.martianzoo.tfm.fake

import dev.martianzoo.tfm.canon.StandardFormBundle

internal val fakesBundle: StandardFormBundle =
    StandardFormBundle(
        name = "fakes",
        resourceFilenames = FakeCanonResources.filenames("bundles/fakes"),
        resourceReader = FakeCanonResources::read,
    )
