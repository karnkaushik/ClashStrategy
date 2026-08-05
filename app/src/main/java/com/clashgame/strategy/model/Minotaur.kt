package com.clashgame.strategy.model

class Minotaur : GameCharacter(
    name = "Minotaur Berserker",
    type = "Minotaur",
    health = 600,
    damage = 70,
    rarity = Rarity.LEGENDARY,
    element = Element.FIRE,
    spriteName = "minotaur",
    ability = "Heavy melee beast",
    description = "Giant bull warrior with bronze armor and double battle axes",
    speed = 0.7f,
    isFlying = false,
    isRanged = false
)
