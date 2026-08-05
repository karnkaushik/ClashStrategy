package com.clashgame.strategy.model

abstract class Building(val name: String) {
    var level: Int = 1
    var health: Int = 100
    var lastUpgradedTime: Long = System.currentTimeMillis()

    fun upgrade() {
        level++
        health += 50
        lastUpgradedTime = System.currentTimeMillis()
    }

    fun checkDegradation(): Boolean {
        val currentTime = System.currentTimeMillis()
        return if (currentTime - lastUpgradedTime > 5000) {
            health -= 10
            true
        } else {
            false
        }
    }
}
