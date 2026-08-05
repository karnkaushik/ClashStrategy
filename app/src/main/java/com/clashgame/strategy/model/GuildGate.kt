package com.clashgame.strategy.model

class GuildGate(val connectedGuild: Guild) {

    fun getMemberDetails(): List<String> {
        val details = mutableListOf<String>()
        details.add("Guild: ${connectedGuild.guildName}")
        for (player in connectedGuild.members) {
            details.add(" - Member: ${player.username} | Resources: ${player.resources}")
        }
        return details
    }
}
