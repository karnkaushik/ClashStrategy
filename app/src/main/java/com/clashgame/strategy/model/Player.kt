package com.clashgame.strategy.model

class Player(val username: String, var resources: Int) {
    val baseBuildings = mutableListOf<Building>()
    val army = mutableListOf<GameCharacter>()

    val tower: DefenseTower
        get() = baseBuildings[0] as DefenseTower

    init {
        baseBuildings.add(DefenseTower())
        army.add(GoblinWarrior())
        army.add(Dragon())
    }

    fun attackPlayer(target: Player): String {
        val enemyTower = target.baseBuildings[0] as DefenseTower
        return if (enemyTower.health > 0) {
            this.resources += 80
            target.resources -= 80
            "${enemyTower.name} defended the base, but ${this.username} looted 80 resources! " +
                "${target.username} can now REVENGE!"
        } else {
            this.resources += 150
            target.resources -= 150
            "Base destroyed! Looted 150 resources!"
        }
    }
}
