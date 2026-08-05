package com.clashgame.strategy.model

class Skeleton : GameCharacter(
    name = "Skeleton Warrior",
    type = "Skeleton",
    health = 180,
    damage = 35,
    rarity = Rarity.COMMON,
    element = Element.SHADOW,
    spriteName = "skeleton",
    ability = "Undead melee unit",
    description = "Ancient skeleton with rusty armor and blue ghost eyes",
    speed = 0.7f,
    isFlying = false,
    isRanged = false
)
