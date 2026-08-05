package com.clashgame.strategy.model

class StoneGiant : GameCharacter(
    name = "Stone Giant",
    type = "Giant",
    health = 800,
    damage = 30,
    rarity = Rarity.EPIC,
    element = Element.EARTH,
    spriteName = "giant",
    ability = "Tank with enormous health",
    description = "Massive rock body with blue glowing crystals and ancient runes",
    speed = 0.5f,
    isFlying = false,
    isRanged = false
)
