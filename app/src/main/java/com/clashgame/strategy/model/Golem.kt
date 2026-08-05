package com.clashgame.strategy.model

class Golem : GameCharacter(
    name = "Ancient Battle Golem",
    type = "Golem",
    health = 900,
    damage = 45,
    rarity = Rarity.LEGENDARY,
    element = Element.ARCANE,
    spriteName = "golem",
    ability = "Siege tank",
    description = "Ancient stone golem with crystal core and blue energy cracks",
    speed = 0.4f,
    isFlying = false,
    isRanged = false
)
