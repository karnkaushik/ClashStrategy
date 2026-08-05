package com.clashgame.strategy.model

class Phoenix : GameCharacter(
    name = "Phoenix",
    type = "Phoenix",
    health = 300,
    damage = 85,
    rarity = Rarity.LEGENDARY,
    element = Element.FIRE,
    spriteName = "phoenix",
    ability = "Revives after defeat",
    description = "Legendary golden bird with crimson wings and magical flames",
    speed = 1.2f,
    isFlying = true,
    isRanged = true
)
