package dev.martianzoo.tfm.data

import dev.martianzoo.tfm.pets.ComponentDef

interface Definition {
  val toComponentDef: ComponentDef
}
