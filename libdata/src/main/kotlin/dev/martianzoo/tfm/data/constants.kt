package dev.martianzoo.tfm.data

internal val CTYPE_PATTERN = Regex("^[A-Z][a-z][A-Za-z0-9_]*$") // TODO: it's repeated 3 times
internal val RESERVED_CTYPES = setOf("This", "Me")
