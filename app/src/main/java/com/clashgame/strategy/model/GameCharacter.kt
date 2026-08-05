package com.clashgame.strategy.model

enum class Rarity { COMMON, RARE, EPIC, LEGENDARY, MYTHIC }
enum class Element { FIRE, EARTH, WATER, ARCANE, THUNDER, ICE, SHADOW, HOLY, NONE }

abstract class GameCharacter(
    val name: String,
    val type: String,
    val health: Int,
    val damage: Int,
    val rarity: Rarity = Rarity.COMMON,
    val element: Element = Element.NONE,
    val spriteName: String = type.lowercase(),
    val ability: String = "",
    val description: String = "",
    val level: Int = 1,
    val maxLevel: Int = 10,
    val speed: Float = 1f,
    val isFlying: Boolean = false,
    val isRanged: Boolean = false
)
