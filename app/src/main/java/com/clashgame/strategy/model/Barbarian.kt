package com.clashgame.strategy.model

class Barbarian : GameCharacter(
    name = "Kingdom Barbarian",
    type = "Barbarian",
    health = 250,
    damage = 65,
    rarity = Rarity.RARE,
    element = Element.EARTH,
    spriteName = "barbarian",
    ability = "Fast melee attacker with high damage",
    description = "Muscular warrior with blonde hair, steel shoulder plates, and a giant broad sword",
    speed = 1.2f,
    isFlying = false,
    isRanged = false
)
