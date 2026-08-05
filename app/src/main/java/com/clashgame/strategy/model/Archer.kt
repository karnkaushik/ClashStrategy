package com.clashgame.strategy.model

class Archer : GameCharacter(
    name = "Royal Archer",
    type = "Archer",
    health = 120,
    damage = 55,
    rarity = Rarity.RARE,
    element = Element.WATER,
    spriteName = "archer",
    ability = "Long-range precision attacks",
    description = "Elegant female ranger with emerald cloak and magical bow",
    speed = 1.1f,
    isFlying = false,
    isRanged = true
)
