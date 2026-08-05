package com.clashgame.strategy.model

class Sorceress : GameCharacter(
    name = "Ice Sorceress",
    type = "Sorceress",
    health = 110,
    damage = 75,
    rarity = Rarity.EPIC,
    element = Element.ICE,
    spriteName = "sorceress",
    ability = "Freezes enemies",
    description = "Elegant ice queen with crystal armor and snow particles",
    speed = 0.8f,
    isFlying = false,
    isRanged = true
)
