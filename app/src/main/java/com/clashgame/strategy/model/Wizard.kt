package com.clashgame.strategy.model

class Wizard : GameCharacter(
    name = "Arcane Wizard",
    type = "Wizard",
    health = 100,
    damage = 80,
    rarity = Rarity.EPIC,
    element = Element.ARCANE,
    spriteName = "wizard",
    ability = "Area magic attacks",
    description = "Elder mage with crystal staff and floating spell book",
    speed = 0.8f,
    isFlying = false,
    isRanged = true
)
