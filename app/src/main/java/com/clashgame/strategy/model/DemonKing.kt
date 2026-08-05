package com.clashgame.strategy.model

class DemonKing : GameCharacter(
    name = "Demon King",
    type = "Demon",
    health = 1200,
    damage = 120,
    rarity = Rarity.MYTHIC,
    element = Element.FIRE,
    spriteName = "demon",
    ability = "Legendary final boss",
    description = "Massive dark lord with black wings, flaming sword, and golden crown",
    speed = 0.6f,
    isFlying = true,
    isRanged = true
)
