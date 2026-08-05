package com.clashgame.strategy.model

class GoblinWarrior : GameCharacter(
    name = "Goblin Raider",
    type = "Goblin",
    health = 150,
    damage = 40,
    rarity = Rarity.COMMON,
    element = Element.EARTH,
    spriteName = "goblin",
    ability = "Steals resources quickly",
    description = "A sneaky green goblin with a gold backpack and sharp dagger",
    speed = 1.3f,
    isFlying = false,
    isRanged = false
)
