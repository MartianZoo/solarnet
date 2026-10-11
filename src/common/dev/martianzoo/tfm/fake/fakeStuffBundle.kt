package dev.martianzoo.tfm.fake

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.StandardFormBundle

internal val fakeStuffBundle: StandardFormBundle =
    StandardFormBundle(
        name = "fake",
        primaryModuleName = cn("FakeStuffBundle"),
        resourceFilenames = FakeCanonResources.filenames("bundles/fake"),
        resourceReader = FakeCanonResources::read,
    )
