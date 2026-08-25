package dev.martianzoo.tfm.text

import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.tfm.canon.Canon

/** The Authority-wide structural universe used while compiling canonical language metadata. */
internal val canonClassUniverse: ClassTable by lazy { Canon.classTable }
