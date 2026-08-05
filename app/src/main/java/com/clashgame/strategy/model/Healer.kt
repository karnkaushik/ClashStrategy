package com.clashgame.strategy.model

class Healer : GameCharacter(
    name = "Holy Healer",
    type = "Healer",
    health = 90,
    damage = 15,
    rarity = Rarity.EPIC,
    element = Element.HOLY,
    spriteName = "healer",
    ability = "Restores nearby allies",
    description = "Holy priestess with white and gold robes and angelic glow",
    speed = 0.7f,
    isFlying = false,
    isRanged = true
)
