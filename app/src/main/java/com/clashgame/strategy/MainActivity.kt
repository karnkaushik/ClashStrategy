package com.clashgame.strategy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.clashgame.strategy.model.Dragon
import com.clashgame.strategy.model.GoblinWarrior
import com.clashgame.strategy.model.Guild
import com.clashgame.strategy.model.GuildGate
import com.clashgame.strategy.model.Player
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay

private val PanelColor = Color(0xE616283B)
private val AccentGold = Color(0xFFFFC107)
private val AccentGreen = Color(0xFF4CAF50)
private val AccentRed = Color(0xFFEF5350)
private val TextColor = Color(0xFFE8ECF1)

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
    var messageColor by remember { mutableStateOf(AccentGold) }
    var showShop by remember { mutableStateOf(false) }
    var showGate by remember { mutableStateOf(false) }
    var battleActive by remember { mutableStateOf(false) }

    val tower = player1.tower

    val animatedResources by animateIntAsState(player1.resources, tween(500))
    val animatedTowerHp by animateIntAsState(tower.health, tween(500))

    val infinite = rememberInfiniteTransition()
    val towerBounce by infinite.animateFloat(
        0f, -10f,
        infiniteRepeatable(tween(600), RepeatMode.Reverse)
    )
    val lowHpPulse by infinite.animateFloat(
        1f, 1.07f,
        infiniteRepeatable(tween(500), RepeatMode.Reverse)
    )

    val sparkles = remember { mutableStateListOf<Sparkle>() }
    var sparkleTrigger by remember { mutableStateOf(0) }
    val displayMetrics = LocalDisplayMetrics()

    LaunchedEffect(sparkleTrigger) {
        if (sparkleTrigger > 0) {
            repeat(12) {
                sparkles.add(
                    Sparkle(
                        x = displayMetrics.width * 0.5f + Random.nextFloat() * 160f - 80f,
                        y = displayMetrics.height * 0.4f - Random.nextFloat() * 130f
                    )
                )
            }
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(40)
            sparkles.forEach { it.life -= 0.06f }
            sparkles.removeAll { it.life <= 0f }
        }
    }

    val maxTowerHp = 100 + (tower.level - 1) * 50
    val hpRatio = if (maxTowerHp > 0) animatedTowerHp.toFloat() / maxTowerHp else 1f
    val goblinCount = player1.army.count { it.type == "Goblin" }
    val dragonCount = player1.army.count { it.type == "Dragon" }

    Box(Modifier.fillMaxSize()) {
        VillageBackground(
            towerLevel = tower.level,
            hpRatio = hpRatio,
            bounce = towerBounce,
            lowHp = tower.health <= 50,
            lowHpPulse = lowHpPulse,
            sparkles = sparkles
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            HudBar(
                username = player1.username,
                resources = animatedResources,
                towerLevel = tower.level
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ArmyPanel(goblinCount = goblinCount, dragonCount = dragonCount)

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("🏴 ${player2.username}", color = AccentRed, fontWeight = FontWeight.SemiBold)
                    Text("💰 ${player2.resources}", color = AccentGold, fontWeight = FontWeight.SemiBold)
                }

                if (message.isNotEmpty() || version > 0) {
                    AnimatedVisibility(
                        visible = message.isNotEmpty() || version > 0,
                        enter = fadeIn(tween(250)) + scaleIn(initialScale = 0.85f, animationSpec = tween(250)),
                        exit = fadeOut(tween(200))
                    ) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                message.ifEmpty { " " },
                                color = messageColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                modifier = Modifier
                                    .background(PanelColor, RoundedCornerShape(14.dp))
                                    .border(1.dp, AccentGold.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GameButton("🚪 Guild Gate", Modifier.weight(1f)) { showGate = true }
                    GameButton("⚡ Upgrade", Modifier.weight(1f)) {
                        tower.upgrade()
                        version++
                        sparkleTrigger++
                        message = "[UPGRADE SUCCESS] ${tower.name} now Level ${tower.level}! HP: ${tower.health}"
                        messageColor = AccentGold
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GameButton("⏳ Check Tower", Modifier.weight(1f)) {
                        val degraded = tower.checkDegradation()
                        version++
                        message = if (degraded) {
                            messageColor = AccentRed
                            "⚠️ ${tower.name} is degrading! -10 HP"
                        } else {
                            messageColor = AccentGreen
                            "✅ ${tower.name} is in good condition."
                        }
                    }
                    GameButton("⚔️ Attack", Modifier.weight(1f), listOf(Color(0xFF8E2A2A), Color(0xFF5C1A1A))) {
                        battleActive = true
                    }
                }

                GameButton(
                    "🛒 Resource Shop",
                    Modifier.fillMaxWidth().height(50.dp),
                    listOf(Color(0xFF8E6B1F), Color(0xFF5C4516))
                ) { showShop = true }
            }
        }
    }

    if (battleActive) {
        Dialog(
            onDismissRequest = { },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            BattleScreen(
                army = player1.army.toList(),
                towerName = player2.tower.name,
                towerHp = player2.tower.health.toFloat(),
                onFinish = { won ->
                    if (won) {
                        player1.resources += 150
                        player2.resources -= 150
                        message = "🏆 VICTORY! ${player2.username}'s base destroyed! +150"
                        messageColor = AccentGold
                    } else {
                        player1.resources += 80
                        player2.resources -= 80
                        message = "☠️ DEFEAT! ${player2.tower.name} held the line. +80"
                        messageColor = AccentRed
                    }
                    version++
                    battleActive = false
                }
            )
        }
    }

    if (showGate) {
        AlertDialog(
            onDismissRequest = { showGate = false },
            containerColor = PanelColor,
            shape = RoundedCornerShape(24.dp),
            title = { Text("🚪 GUILD GATE", color = AccentGold, fontWeight = FontWeight.Bold) },
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
            containerColor = PanelColor,
            shape = RoundedCornerShape(24.dp),
            title = { Text("🛒 RESOURCE SHOP", color = AccentGold, fontWeight = FontWeight.Bold) },
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

private class Sparkle(var x: Float, var y: Float) {
    var life = 1f
}

private data class DisplayMetrics(val width: Int, val height: Int)

@Composable
private fun LocalDisplayMetrics(): DisplayMetrics {
    val dm = android.content.res.Resources.getSystem().displayMetrics
    return remember { DisplayMetrics(dm.widthPixels, dm.heightPixels) }
}

@Composable
private fun VillageBackground(
    towerLevel: Int,
    hpRatio: Float,
    bounce: Float,
    lowHp: Boolean,
    lowHpPulse: Float,
    sparkles: List<Sparkle>
) {
    val infinite = rememberInfiniteTransition()
    val cloudT by infinite.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(30000, easing = LinearEasing), RepeatMode.Restart)
    )

    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val groundY = h * 0.74f

        // Sky
        drawRect(
            Brush.verticalGradient(listOf(Color(0xFF0B1B33), Color(0xFF1B3A5C), Color(0xFF2E5D6E))),
            size = Size(w, h)
        )

        // Twinkling stars
        repeat(22) { i ->
            val sx = w * ((i * 37) % 100) / 100f
            val sy = h * ((i * 53) % 42) / 100f
            val tw = 0.3f + 0.7f * (0.5f + 0.5f * sin(cloudT * 45f + i * 1.7f)).toFloat()
            drawCircle(Color(1f, 1f, 1f, tw), radius = 1.6f, center = Offset(sx, sy))
        }

        // Moon
        drawCircle(Color(0xFFFFF3C4), radius = w * 0.09f, center = Offset(w * 0.84f, h * 0.14f))
        drawCircle(Color(0xFF0B1B33), radius = w * 0.075f, center = Offset(w * 0.88f, h * 0.12f))

        // Drifting clouds
        drawCloud(-w * 0.25f + cloudT * w * 1.5f, h * 0.20f, w * 0.30f)
        drawCloud(-w * 0.50f + ((cloudT + 0.5f) % 1f) * w * 1.5f, h * 0.32f, w * 0.22f)

        // Ground
        drawRect(
            Brush.verticalGradient(listOf(Color(0xFF3E8E41), Color(0xFF2E7D32))),
            topLeft = Offset(0f, groundY),
            size = Size(w, h - groundY)
        )

        // Trees
        drawTree(w * 0.08f, groundY, 1f)
        drawTree(w * 0.92f, groundY, 0.9f)

        // Player tower
        val pulse = if (lowHp) lowHpPulse else 1f
        drawVillageTower(w * 0.5f, groundY + bounce, towerLevel, hpRatio, pulse)

        // Sparkles (upgrade effect)
        sparkles.forEach { s ->
            drawCircle(
                Color(0xFFFFD54F).copy(alpha = s.life.coerceIn(0f, 1f)),
                radius = 4f,
                center = Offset(s.x, s.y)
            )
        }
    }
}

private fun DrawScope.drawCloud(cx: Float, cy: Float, scale: Float) {
    val c = Color(1f, 1f, 1f, 0.14f)
    drawCircle(c, radius = scale * 0.18f, center = Offset(cx, cy))
    drawCircle(c, radius = scale * 0.14f, center = Offset(cx - scale * 0.12f, cy + scale * 0.05f))
    drawCircle(c, radius = scale * 0.15f, center = Offset(cx + scale * 0.13f, cy + scale * 0.04f))
}

private fun DrawScope.drawTree(x: Float, groundY: Float, s: Float) {
    drawRect(
        Color(0xFF5D4037),
        topLeft = Offset(x - 6f * s, groundY - 60f * s),
        size = Size(12f * s, 60f * s)
    )
    drawCircle(Color(0xFF1B5E20), radius = 34f * s, center = Offset(x, groundY - 70f * s))
    drawCircle(Color(0xFF1B5E20), radius = 26f * s, center = Offset(x - 24f * s, groundY - 55f * s))
    drawCircle(Color(0xFF1B5E20), radius = 26f * s, center = Offset(x + 24f * s, groundY - 55f * s))
}

private fun DrawScope.drawVillageTower(
    cx: Float,
    groundY: Float,
    level: Int,
    hpRatio: Float,
    towerScale: Float
) {
    val bodyW = 78f
    val bodyH = 140f
    val top = groundY - bodyH

    drawOval(Color(0x33000000), topLeft = Offset(cx - 60f, groundY + 4f), size = Size(120f, 18f))

    withTransform({
        translate(cx, 0f)
        scale(towerScale, towerScale, pivot = Offset(0f, groundY))
    }) {
        drawRoundRect(
            Color(0xFF5D6D7E),
            topLeft = Offset(-bodyW / 2, top),
            size = Size(bodyW, bodyH),
            cornerRadius = CornerRadius(8f)
        )
        repeat(3) { i ->
            val yy = top + bodyH * (i + 1) / 4f
            drawLine(Color(0xFF4A5560), Offset(-bodyW / 2, yy), Offset(bodyW / 2, yy), strokeWidth = 2.5f)
        }
        repeat(2) { i ->
            drawRoundRect(
                Color(0xFFFFE082),
                topLeft = Offset(-13f, top + 18f + i * 42f),
                size = Size(26f, 22f),
                cornerRadius = CornerRadius(4f)
            )
            drawLine(
                Color(0xFF5D6D7E),
                Offset(0f, top + 18f + i * 42f),
                Offset(0f, top + 18f + i * 42f + 22f),
                strokeWidth = 2f
            )
        }
        drawArc(
            Color(0xFF34495E), 180f, 180f,
            useCenter = false,
            topLeft = Offset(-16f, groundY - 46f),
            size = Size(32f, 48f)
        )
        repeat(3) { i ->
            drawRect(
                Color(0xFF5D6D7E),
                topLeft = Offset(-bodyW / 2 + 8f + i * 26f, top - 18f),
                size = Size(18f, 18f)
            )
        }
        val roof = Path().apply {
            moveTo(-bodyW / 2 - 10f, top)
            lineTo(0f, top - 46f)
            lineTo(bodyW / 2 + 10f, top)
            close()
        }
        drawPath(roof, Color(0xFFB03A2E))
        drawLine(Color(0xFF8B4513), Offset(0f, top - 46f), Offset(0f, top - 70f), strokeWidth = 3f)
        drawRect(Color(0xFFF1C40F), topLeft = Offset(0f, top - 70f), size = Size(22f, 14f))
    }

    // HP bar above the tower
    val barW = 110f
    val barH = 12f
    val barTop = top - 44f
    drawRoundRect(
        Color(0x88000000),
        topLeft = Offset(cx - barW / 2 - 2f, barTop - 2f),
        size = Size(barW + 4f, barH + 4f),
        cornerRadius = CornerRadius(4f)
    )
    val barColor = if (hpRatio > 0.5f) Color(0xFF4CAF50)
    else if (hpRatio > 0.25f) Color(0xFFFFC107) else Color(0xFFEF5350)
    drawRoundRect(
        barColor,
        topLeft = Offset(cx - barW / 2, barTop),
        size = Size(barW * hpRatio.coerceIn(0f, 1f), barH),
        cornerRadius = CornerRadius(4f)
    )
}

@Composable
private fun HudBar(username: String, resources: Int, towerLevel: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PanelColor, RoundedCornerShape(16.dp))
            .border(1.dp, AccentGold.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("👑 $username", color = AccentGold, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text("🏰 Lv $towerLevel", color = TextColor, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(12.dp))
        Text("💰 $resources", color = AccentGold, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ArmyPanel(goblinCount: Int, dragonCount: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TroopChip("👺", "Goblin", goblinCount, "DMG 40", Color(0xFF66BB6A), Modifier.weight(1f))
        TroopChip("🐲", "Dragon", dragonCount, "DMG 100", Color(0xFFEF5350), Modifier.weight(1f))
    }
}

@Composable
private fun TroopChip(
    icon: String,
    name: String,
    count: Int,
    info: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(PanelColor, RoundedCornerShape(14.dp))
            .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 22.sp)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(name, color = TextColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("×$count  $info", color = TextColor.copy(alpha = 0.75f), fontSize = 11.sp)
        }
    }
}

@Composable
private fun GameButton(
    text: String,
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(Color(0xFF2A4A6E), Color(0xFF16283B)),
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.93f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )
    Box(
        modifier = modifier
            .height(52.dp)
            .scale(scale)
            .shadow(6.dp, RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(colors), RoundedCornerShape(14.dp))
            .border(2.dp, AccentGold.copy(alpha = 0.65f), RoundedCornerShape(14.dp))
            .clickable(interactionSource = interaction, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
