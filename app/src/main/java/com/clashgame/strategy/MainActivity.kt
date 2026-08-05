package com.clashgame.strategy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clashgame.strategy.model.Dragon
import com.clashgame.strategy.model.GoblinWarrior
import com.clashgame.strategy.model.Guild
import com.clashgame.strategy.model.GuildGate
import com.clashgame.strategy.model.Player

private val Background = Color(0xFF0D1B2A)
private val CardColor = Color(0xFF16283B)
private val ButtonColor = Color(0xFF1F3B57)
private val AccentGold = Color(0xFFFFC107)
private val AccentGreen = Color(0xFF4CAF50)
private val AccentRed = Color(0xFFEF5350)
private val TextColor = Color(0xFFE0E0E0)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GameScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen() {
    val player1 = remember { Player("Rahul_Warrior", 500) }
    val player2 = remember { Player("Vikram_DragonLord", 400) }
    val guild = remember {
        Guild("Dragon Slayers").also {
            it.addMember(player1)
            it.addMember(player2)
        }
    }
    val guildGate = remember { GuildGate(guild) }

    var version by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf("") }
    var messageColor by remember { mutableStateOf(AccentGreen) }
    var showShop by remember { mutableStateOf(false) }
    var showGate by remember { mutableStateOf(false) }

    val tower = player1.tower

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Text("⚔️ CLASH STRATEGY", fontWeight = FontWeight.Bold, color = AccentGold)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardColor)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = CardColor)) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("👑 ${player1.username}", color = AccentGold, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("💰 Resources: ${player1.resources}", color = TextColor)
                    Text("🏰 ${tower.name}  |  Lv ${tower.level}  |  ❤️ HP ${tower.health}", color = TextColor)
                    Text("🛡️ Army: ${player1.army.size} troops", color = TextColor)
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = CardColor)) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("🪖 YOUR ARMY", color = AccentGold, fontWeight = FontWeight.Bold)
                    player1.army.groupBy { it.type }.forEach { (_, troops) ->
                        val troop = troops.first()
                        Text(
                            "• ${troop.name} ×${troops.size}  (HP ${troop.health} | DMG ${troop.damage})",
                            color = TextColor
                        )
                    }
                }
            }

            if (message.isNotEmpty() || version > 0) {
                Card(colors = CardDefaults.cardColors(containerColor = CardColor)) {
                    Text(
                        message.ifEmpty { " " },
                        color = messageColor,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Text("ACTIONS", color = AccentGold, fontWeight = FontWeight.Bold)

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionButton("🚪 Guild Gate", Modifier.weight(1f)) { showGate = true }
                ActionButton("⚡ Upgrade Tower", Modifier.weight(1f)) {
                    tower.upgrade()
                    version++
                    message = "[UPGRADE SUCCESS] ${tower.name} upgraded to Level ${tower.level}! (HP: ${tower.health})"
                    messageColor = AccentGreen
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionButton("⏳ Check Tower", Modifier.weight(1f)) {
                    val degraded = tower.checkDegradation()
                    version++
                    message = if (degraded) {
                        messageColor = AccentRed
                        "⚠️ [WARNING] ${tower.name} is degrading due to lack of maintenance!"
                    } else {
                        messageColor = AccentGreen
                        "✅ [STATUS] ${tower.name} is in good condition."
                    }
                }
                ActionButton("⚔️ Attack", Modifier.weight(1f)) {
                    val result = player1.attackPlayer(player2)
                    version++
                    message = result
                    messageColor = AccentGreen
                }
            }

            ActionButton("🛒 Visit the Shop", Modifier.fillMaxWidth()) { showShop = true }

            Card(colors = CardDefaults.cardColors(containerColor = CardColor)) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("🏴 ENEMY BASE", color = AccentRed, fontWeight = FontWeight.Bold)
                    Text("👤 ${player2.username}  |  💰 Resources: ${player2.resources}", color = TextColor)
                }
            }
        }
    }

    if (showGate) {
        AlertDialog(
            onDismissRequest = { showGate = false },
            containerColor = CardColor,
            title = {
                Text("🚪 GUILD GATE", color = AccentGold, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    guildGate.getMemberDetails().forEach { detail ->
                        Text(detail, color = TextColor)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGate = false }) { Text("Close", color = AccentGold) }
            }
        )
    }

    if (showShop) {
        AlertDialog(
            onDismissRequest = { showShop = false },
            containerColor = CardColor,
            title = {
                Text("🛒 RESOURCE SHOP", color = AccentGold, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Your Resources: ${player1.resources}",
                        color = TextColor,
                        fontWeight = FontWeight.SemiBold
                    )
                    ShopOption("1. Buy Goblin Warrior", "Cost: 50") {
                        if (player1.resources >= 50) {
                            player1.resources -= 50
                            player1.army.add(GoblinWarrior())
                            showShop = false
                            version++
                            message = "✅ Bought 1 Sneaky Goblin!"
                            messageColor = AccentGreen
                        } else {
                            version++
                            message = "❌ Not enough resources!"
                            messageColor = AccentRed
                        }
                    }
                    ShopOption("2. Buy Fire Dragon", "Cost: 150") {
                        if (player1.resources >= 150) {
                            player1.resources -= 150
                            player1.army.add(Dragon())
                            showShop = false
                            version++
                            message = "✅ Bought 1 Fire Dragon!"
                            messageColor = AccentGreen
                        } else {
                            version++
                            message = "❌ Not enough resources!"
                            messageColor = AccentRed
                        }
                    }
                    ShopOption("3. Repair Tower (+20 HP)", "Cost: 30") {
                        if (player1.resources >= 30) {
                            player1.resources -= 30
                            tower.health += 20
                            showShop = false
                            version++
                            message = "✅ Tower repaired! Current HP: ${tower.health}"
                            messageColor = AccentGreen
                        } else {
                            version++
                            message = "❌ Not enough resources!"
                            messageColor = AccentRed
                        }
                    }
                    Text("4. Go back to menu", color = TextColor)
                }
            },
            confirmButton = {
                TextButton(onClick = { showShop = false }) { Text("Back", color = AccentGold) }
            }
        )
    }
}

@Composable
private fun ActionButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ButtonColor)
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ShopOption(label: String, cost: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentGold),
        border = BorderStroke(1.dp, Color(0xFF3A5570))
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = TextColor)
            Text(cost, color = AccentGold)
        }
    }
}
