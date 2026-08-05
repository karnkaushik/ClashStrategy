package com.clashgame.strategy.model

class Dragon : GameCharacter(
    name = "Fire Dragon",
    type = "Dragon",
    health = 500,
    damage = 100,
    rarity = Rarity.LEGENDARY,
    element = Element.FIRE,
    spriteName = "dragon",
    ability = "Flying fire attacker",
    description = "Massive crimson dragon with lava cracks and fire particles",
    speed = 0.9f,
    isFlying = true,
    isRanged = true
)
