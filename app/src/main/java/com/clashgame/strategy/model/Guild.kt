package com.clashgame.strategy.model

class Guild(val guildName: String) {
    val members = mutableListOf<Player>()

    fun addMember(player: Player) {
        members.add(player)
    }
}
