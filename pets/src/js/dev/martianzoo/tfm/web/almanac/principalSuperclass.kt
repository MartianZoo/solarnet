package dev.martianzoo.tfm.web.almanac

import dev.martianzoo.pets.types.Class

/** Prefer the direct parent with the most subclasses; break ties alphabetically. */
internal fun principalSuperclass(klass: Class): Class? =
    klass.directSuperclasses.minWithOrNull(
        compareBy({ -klass.classTable.allSubclasses(it).size }, { it.className })
    )
