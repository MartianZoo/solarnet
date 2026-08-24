package dev.martianzoo.tfm.language

import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.types.ClassTable

/** The Authority-wide structural universe used while compiling canonical language metadata. */
internal val canonClassUniverse: ClassTable by lazy { Canon.classTable }
