package com.clashgame.strategy.model

class Assassin : GameCharacter(
    name = "Shadow Assassin",
    type = "Assassin",
    health = 130,
    damage = 90,
    rarity = Rarity.EPIC,
    element = Element.SHADOW,
    spriteName = "assassin",
    ability = "Fast stealth killer",
    description = "Stealthy killer with purple glowing eyes and dual enchanted daggers",
    speed = 1.5f,
    isFlying = false,
    isRanged = false
)
