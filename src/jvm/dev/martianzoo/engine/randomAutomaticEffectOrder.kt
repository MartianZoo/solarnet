package dev.martianzoo.engine

internal actual val randomAutomaticEffectOrderEnabled: Boolean =
    System.getenv("SOLARNET_RANDOM_AUTOMATIC_EFFECTS").equals("true", ignoreCase = true)
