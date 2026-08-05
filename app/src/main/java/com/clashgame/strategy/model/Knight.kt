package com.clashgame.strategy.model

class Knight : GameCharacter(
    name = "Royal Knight",
    type = "Knight",
    health = 350,
    damage = 50,
    rarity = Rarity.RARE,
    element = Element.HOLY,
    spriteName = "knight",
    ability = "Balanced defense and attack",
    description = "Noble warrior with silver armor, blue cape, and golden emblem",
    speed = 0.9f,
    isFlying = false,
    isRanged = false
)
