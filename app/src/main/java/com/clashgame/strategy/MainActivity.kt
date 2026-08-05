package com.clashgame.strategy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.clashgame.strategy.model.Dragon
import com.clashgame.strategy.model.GoblinWarrior
import com.clashgame.strategy.model.Guild
import com.clashgame.strategy.model.GuildGate
import com.clashgame.strategy.model.Player
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

// ============ CLASH-STYLE COLOR PALETTE ============
private val GoldBright = Color(0xFFF5A623)
private val GoldMedium = Color(0xFFDAA520)
private val GoldDark = Color(0xFFC4841D)
private val GoldDeep = Color(0xFF8B6914)
private val GoldBorder = Color(0xFFFFD86B)
private val PanelBrown = Color(0xFF3D2817)
private val PanelDark = Color(0xFF23150C)
private val PanelLight = Color(0xFF5A3D24)
private val BannerBlue = Color(0xFF1565C0)
private val BannerBlueDark = Color(0xFF0D47A1)
private val GrassGreen = Color(0xFF5D9B3B)
private val GrassDark = Color(0xFF3E7B27)
private val GrassLight = Color(0xFF7CB342)
private val ElixirPurple = Color(0xFF9C27B0)
private val ElixirLight = Color(0xFFCE93D8)
private val EnemyRed = Color(0xFFC62828)
private val EnemyRedLight = Color(0xFFEF5350)
private val TextWhite = Color(0xFFF5F5F5)
private val TextGold = Color(0xFFFFD54F)
private val TextGray = Color(0xFFB0BEC5)

// ============ SCREENS ============
private sealed class Screen {
    object Village : Screen()
    object Store : Screen()
    object Army : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GameApp() }
    }
}

@Composable
fun GameApp() {
    var screen by remember { mutableStateOf<Screen>(Screen.Village) }
    var battleActive by remember { mutableStateOf(false) }
    var showShop by remember { mutableStateOf(false) }
    var showGate by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var messageColor by remember { mutableStateOf(TextGold) }
    var version by remember { mutableStateOf(0) }

    val player = remember { Player("Chief Kyle", 1000) }
    val guild = remember {
        Guild("Dragon Slayers").also {
            it.addMember(player)
            it.addMember(Player("Vikram", 400))
            it.addMember(Player("Maya", 300))
        }
    }
    val guildGate = remember { GuildGate(guild) }
    val tower = player.tower

    Box(Modifier.fillMaxSize().background(PanelDark)) {
        when (screen) {
            Screen.Village -> VillageHome(
                player = player,
                message = message,
                messageColor = messageColor,
                version = version,
                onBattle = { battleActive = true },
                onStore = { screen = Screen.Store },
                onArmy = { screen = Screen.Army },
                onGate = { showGate = true },
                onShop = { showShop = true },
                onUpgrade = {
                    if (player.resources >= 200) {
                        player.resources -= 200
                        tower.upgrade()
                        version++
                        message = "Town Hall Upgraded! Lv ${tower.level} HP ${tower.health}"
                        messageColor = TextGold
                    } else {
                        version++
                        message = "Need 200 Gold to upgrade!"
                        messageColor = EnemyRedLight
                    }
                }
            )
            Screen.Store -> StoreScreen(
                player = player,
                onBack = { screen = Screen.Village },
                onBuyTroop = { kind ->
                    version++
                    if (kind == "Goblin") {
                        if (player.resources >= 50) {
                            player.resources -= 50
                            player.army.add(GoblinWarrior())
                            message = "Bought 1 Goblin!"; messageColor = Color(0xFF66BB6A)
                        } else { message = "Not enough gold!"; messageColor = EnemyRedLight }
                    } else {
                        if (player.resources >= 150) {
                            player.resources -= 150
                            player.army.add(Dragon())
                            message = "Bought 1 Dragon!"; messageColor = Color(0xFF66BB6A)
                        } else { message = "Not enough gold!"; messageColor = EnemyRedLight }
                    }
                }
            )
            Screen.Army -> ArmyScreen(
                player = player,
                onBack = { screen = Screen.Village }
            )
        }

        if (battleActive) {
            Dialog(
                onDismissRequest = { battleActive = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                BattleScreen(
                    army = player.army.toList(),
                    towerName = "Enemy Archer Tower",
                    towerHp = 400f,
                    onFinish = { won ->
                        if (won) {
                            player.resources += 150
                            message = "VICTORY! +150 Gold +30 Trophies"
                            messageColor = TextGold
                        } else {
                            player.resources += 60
                            message = "DEFEAT - 30 Trophies, +60 Gold"
                            messageColor = EnemyRedLight
                        }
                        version++
                        battleActive = false
                    }
                )
            }
        }

        if (showShop) {
            AlertDialog(
                onDismissRequest = { showShop = false },
                containerColor = PanelBrown,
                shape = RoundedCornerShape(24.dp),
                title = { Text("Royal Shop", color = TextGold, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Gold: ${player.resources}", color = TextGold, fontWeight = FontWeight.Bold)
                        ShopRow("Goblin Warrior", "50 Gold", Color(0xFF66BB6A)) {
                            if (player.resources >= 50) {
                                player.resources -= 50
                                player.army.add(GoblinWarrior())
                                message = "Bought Goblin!"; messageColor = Color(0xFF66BB6A)
                                version++; showShop = false
                            } else { message = "Not enough gold!"; messageColor = EnemyRedLight; version++ }
                        }
                        ShopRow("Fire Dragon", "150 Gold", Color(0xFFEF5350)) {
                            if (player.resources >= 150) {
                                player.resources -= 150
                                player.army.add(Dragon())
                                message = "Bought Dragon!"; messageColor = Color(0xFF66BB6A)
                                version++; showShop = false
                            } else { message = "Not enough gold!"; messageColor = EnemyRedLight; version++ }
                        }
                        ShopRow("Repair Tower +25 HP", "40 Gold", Color(0xFF42A5F5)) {
                            if (player.resources >= 40) {
                                player.resources -= 40
                                tower.health += 25
                                message = "Tower repaired! HP ${tower.health}"; messageColor = Color(0xFF42A5F5)
                                version++; showShop = false
                            } else { message = "Not enough gold!"; messageColor = EnemyRedLight; version++ }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showShop = false }) { Text("Close", color = TextGold) }
                }
            )
        }

        if (showGate) {
            AlertDialog(
                onDismissRequest = { showGate = false },
                containerColor = PanelBrown,
                shape = RoundedCornerShape(24.dp),
                title = { Text("Guild Gate", color = TextGold, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        guildGate.getMemberDetails().forEach { d -> Text(d, color = TextWhite) }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showGate = false }) { Text("Close", color = TextGold) }
                }
            )
        }
    }
}

// ============ VILLAGE HOME ============
@Composable
private fun VillageHome(
    player: Player,
    message: String,
    messageColor: Color,
    version: Int,
    onBattle: () -> Unit,
    onStore: () -> Unit,
    onArmy: () -> Unit,
    onGate: () -> Unit,
    onShop: () -> Unit,
    onUpgrade: () -> Unit
) {
    val infinite = rememberInfiniteTransition()
    val bounce by infinite.animateFloat(0f, -12f, infiniteRepeatable(tween(900), RepeatMode.Reverse))

    Box(Modifier.fillMaxSize()) {
        VillageCanvas(
            towerLevel = player.tower.level,
            bounce = bounce
        )

        Column(Modifier.fillMaxSize().padding(12.dp)) {
            HudBar(player = player, towerLevel = player.tower.level)

            Box(Modifier.weight(1f).fillMaxWidth()) {
                // Left: crowned shield info
                Box(Modifier.align(Alignment.TopStart).padding(top = 8.dp)) {
                    ShieldBadge(towerLevel = player.tower.level, towerHp = player.tower.health)
                }

                // Center: troop preview cards
                Column(
                    Modifier.align(Alignment.TopEnd).padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TroopBadge("Goblin", player.army.count { it.type == "Goblin" }, Color(0xFF66BB6A))
                    TroopBadge("Dragon", player.army.count { it.type == "Dragon" }, Color(0xFFEF5350))
                }
            }

            if (version > 0 && message.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Text(
                        message,
                        color = messageColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .background(PanelBrown.copy(alpha = 0.95f), RoundedCornerShape(12.dp))
                            .border(1.dp, GoldBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GameButton("🏰 Upgrade", Modifier.weight(1f), onUpgrade)
                GameButton("🚪 Guild", Modifier.weight(1f), onGate)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GameButton("🛒 Shop", Modifier.weight(1f), onShop)
                GameButton("⚔️ Battle", Modifier.weight(1f), onBattle)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GameButton("🗡️ Army", Modifier.weight(1f), onArmy)
                GameButton("🏪 Store", Modifier.weight(1f), onStore)
            }
        }
    }
}

// ============ VILLAGE BACKGROUND ============
@Composable
private fun VillageCanvas(towerLevel: Int, bounce: Float) {
    val infinite = rememberInfiniteTransition()
    val cloudT by infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(50000), RepeatMode.Restart))
    val flagFlutter by infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse))

    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val groundY = h * 0.72f

        // Sky gradient (game-like warm sky)
        drawRect(
            Brush.verticalGradient(listOf(Color(0xFF4A7BBB), Color(0xFF79B0E0), Color(0xFFB8E0F0))),
            size = Size(w, h)
        )

        // Clouds
        drawCloud(w * 0.6f - ((cloudT * w * 1.2f) % w), h * 0.10f, w * 0.16f)
        drawCloud(w * 0.2f - ((cloudT * w * 1.2f) % w) * 0.7f + w * 0.4f, h * 0.18f, w * 0.12f)

        // Grass field
        drawRect(
            Brush.verticalGradient(listOf(GrassLight, GrassGreen, GrassDark)),
            topLeft = Offset(0f, groundY),
            size = Size(w, h - groundY)
        )

        // Grass texture stripes
        repeat(6) { i ->
            drawLine(
                GrassDark.copy(alpha = 0.3f),
                Offset(0f, groundY + i * 22f),
                Offset(w, groundY + i * 22f),
                strokeWidth = 4f
            )
        }

        // Village wall fence across the field
        val fenceY = groundY + 28f
        var fx = 20f
        while (fx < w - 20f) {
            drawRect(GoldDark, topLeft = Offset(fx, fenceY - 26f), size = Size(14f, 26f))
            drawRect(GoldMedium, topLeft = Offset(fx, fenceY - 30f), size = Size(14f, 6f))
            fx += 34f
        }

        // Town hall (center)
        val townX = w * 0.5f
        val townY = groundY - 40f + bounce
        drawIsoBuilding(townX, townY, 46f, 40f, 58f,
            Color(0xFF6D4C2F), Color(0xFFB4692E), Color(0xFF8A4F1D))
        // town hall roof peak
        val peak = Path().apply {
            moveTo(townX - 30f, townY - 58f)
            lineTo(townX, townY - 98f)
            lineTo(townX + 30f, townY - 58f)
            close()
        }
        drawPath(peak, Color(0xFF8E3B2E))
        drawRect(GoldBright, topLeft = Offset(townX - 8f, townY - 98f), size = Size(16f, 8f))
        // flag
        val flagX = townX
        drawLine(Color(0xFF5D4037), Offset(flagX, townY - 98f), Offset(flagX, townY - 124f), strokeWidth = 4f)
        val flagPath = Path().apply {
            moveTo(flagX, townY - 124f)
            lineTo(flagX + 26f + 8f * flagFlutter, townY - 118f)
            lineTo(flagX, townY - 112f)
            close()
        }
        drawPath(flagPath, Color(0xFFEF5350))
        // town hall door
        drawArc(
            Color(0xFF3E2723), 180f, 180f, false,
            topLeft = Offset(townX - 14f, townY - 26f), size = Size(28f, 30f)
        )

        // Barracks (left)
        drawIsoBuilding(w * 0.22f, groundY - 12f, 38f, 30f, 44f,
            Color(0xFF5D4037), Color(0xFF9C4A2B), Color(0xFF6E3320))
        drawLine(Color(0xFF3E2723), Offset(w * 0.22f - 16f, groundY - 48f), Offset(w * 0.22f, groundY - 70f), strokeWidth = 3f)
        val bFlag = Path().apply {
            moveTo(w * 0.22f, groundY - 70f)
            lineTo(w * 0.22f + 20f + 6f * flagFlutter, groundY - 64f)
            lineTo(w * 0.22f, groundY - 58f)
            close()
        }
        drawPath(bFlag, Color(0xFFF5A623))

        // Gold mine (right)
        drawIsoBuilding(w * 0.78f, groundY - 12f, 34f, 30f, 40f,
            Color(0xFFF9C74F), Color(0xFFE9A820), Color(0xFFB8860B))
        drawCircle(Color(0xFF8B6914), radius = 10f, center = Offset(w * 0.78f, groundY - 26f))
        drawOval(
            Color(0xFFD4AF37), topLeft = Offset(w * 0.78f - 18f, groundY - 16f),
            size = Size(20f, 14f)
        )

        // Elixir collector (right-bottom)
        drawIsoBuilding(w * 0.66f, groundY - 14f, 30f, 26f, 34f,
            Color(0xFF7B1FA2), Color(0xFF9C27B0), Color(0xFF6A11A0))
        drawOval(
            ElixirLight, topLeft = Offset(w * 0.66f - 9f, groundY - 26f),
            size = Size(18f, 14f)
        )

        // Trees
        drawTree(w * 0.12f, groundY + 6f, 0.9f)
        drawTree(w * 0.88f, groundY + 10f, 1f)
        drawTree(w * 0.40f, groundY + 16f, 0.7f)

        // Rocks
        drawCircle(Color(0xFF8D8D8D), radius = 8f, center = Offset(w * 0.56f, groundY + 14f))
        drawCircle(Color(0xFFB0B0B0), radius = 5f, center = Offset(w * 0.58f, groundY + 12f))
    }
}

private fun DrawScope.drawCloud(cx: Float, cy: Float, s: Float) {
    val c = Color(1f, 1f, 1f, 0.85f)
    drawOval(c, topLeft = Offset(cx - s * 0.6f, cy - s * 0.18f), size = Size(s * 1.2f, s * 0.36f))
    drawCircle(c, radius = s * 0.18f, center = Offset(cx - s * 0.2f, cy))
    drawCircle(c, radius = s * 0.22f, center = Offset(cx + s * 0.1f, cy - s * 0.1f))
    drawCircle(c, radius = s * 0.16f, center = Offset(cx + s * 0.3f, cy))
}

private fun DrawScope.drawTree(x: Float, groundY: Float, s: Float) {
    drawRect(Color(0xFF6F4E37), topLeft = Offset(x - 5f * s, groundY - 40f * s), size = Size(10f * s, 40f * s))
    drawCircle(Color(0xFF2E7D32), radius = 30f * s, center = Offset(x, groundY - 52f * s))
    drawCircle(Color(0xFF1B5E20), radius = 20f * s, center = Offset(x - 18f * s, groundY - 42f * s))
    drawCircle(Color(0xFF1B5E20), radius = 20f * s, center = Offset(x + 18f * s, groundY - 42f * s))
}

// Isometric-style 3D box with roof, left face, right face
private fun DrawScope.drawIsoBuilding(
    cx: Float, groundY: Float,
    w: Float, d: Float, h: Float,
    roof: Color, faceLight: Color, faceDark: Color
) {
    val tw = w * 0.34f
    val dth = d * 0.24f
    val topY = groundY - h

    drawOval(Color(0x44000000), topLeft = Offset(cx - tw - 8f, groundY + 6f), size = Size((tw + 8f) * 2f, 16f))

    val top = Path().apply {
        moveTo(cx, topY - dth * 0.6f)
        lineTo(cx + tw, topY + dth * 0.3f)
        lineTo(cx, topY + dth * 1.3f)
        lineTo(cx - tw, topY + dth * 0.3f)
        close()
    }
    drawPath(top, roof)

    val left = Path().apply {
        moveTo(cx - tw, topY + dth * 0.3f)
        lineTo(cx, topY + dth * 1.3f)
        lineTo(cx, groundY + dth * 0.8f)
        lineTo(cx - tw, groundY)
        close()
    }
    drawPath(left, faceLight)

    val right = Path().apply {
        moveTo(cx + tw, topY + dth * 0.3f)
        lineTo(cx, topY + dth * 1.3f)
        lineTo(cx, groundY + dth * 0.8f)
        lineTo(cx + tw, groundY)
        close()
    }
    drawPath(right, faceDark)
}

// ============ HUD ============
@Composable
private fun HudBar(player: Player, towerLevel: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(GoldDark, GoldMedium, GoldDark)), RoundedCornerShape(14.dp))
            .border(2.dp, GoldBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Level badge
        Box(
            Modifier
                .background(BannerBlue, RoundedCornerShape(10.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text("Lv $towerLevel", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Spacer(Modifier.width(8.dp))
        Text(player.username, color = Color(0xFF402C12), fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
        ResourcePill("🏆", "1013", Color(0xFF3D5AA9))
        Spacer(Modifier.width(6.dp))
        ResourcePill("💰", player.resources.toString(), GoldDark)
    }
}

@Composable
private fun ResourcePill(icon: String, value: String, bg: Color) {
    Row(
        Modifier
            .background(Color(0xCCFFFFFF), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 13.sp)
        Spacer(Modifier.width(4.dp))
        Text(value, color = Color(0xFF3D2817), fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

// ============ SHIELD BADGE ============
@Composable
private fun ShieldBadge(towerLevel: Int, towerHp: Int) {
    Box(
        Modifier
            .width(132.dp)
            .height(150.dp)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f + 8f
            val s = size.width * 0.44f

            // Shield
            val shield = Path().apply {
                moveTo(cx, cy - s)
                cubicTo(cx + s * 0.9f, cy - s * 0.85f, cx + s * 1.05f, cy - s * 0.15f, cx + s * 0.75f, cy + s * 0.4f)
                lineTo(cx, cy + s * 0.98f)
                lineTo(cx - s * 0.75f, cy + s * 0.4f)
                cubicTo(cx - s * 1.05f, cy - s * 0.15f, cx - s * 0.9f, cy - s * 0.85f, cx, cy - s)
                close()
            }
            drawPath(
                shield,
                Brush.linearGradient(listOf(BannerBlue, BannerBlueDark, Color(0xFF0A2F6B)))
            )
            drawPath(shield, Color(0xFFF9D976), style = Stroke(width = 4f))

            // Shield emblem: gold tower
            drawRect(GoldBright, topLeft = Offset(cx - 10f, cy - 10f), size = Size(20f, 26f))
            drawCircle(GoldBright, radius = 12f, center = Offset(cx - 10f, cy - 14f))
            drawCircle(Color(0xFF0A2F6B), radius = 5f, center = Offset(cx, cy - 14f))

            // Crown
            drawCrown(cx, cy - s - 14f, s * 0.42f)
        }
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .width(120.dp)
                .background(PanelBrown.copy(alpha = 0.95f), RoundedCornerShape(10.dp))
                .border(1.dp, GoldBorder, RoundedCornerShape(10.dp))
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Castle $towerLevel", color = TextGold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            Text("HP $towerHp", color = TextWhite, fontSize = 9.sp)
        }
    }
}

private fun DrawScope.drawCrown(cx: Float, cy: Float, s: Float) {
    val crown = Path().apply {
        moveTo(cx - s, cy)
        lineTo(cx - s * 0.62f, cy - s * 0.85f)
        lineTo(cx - s * 0.28f, cy - s * 0.3f)
        lineTo(cx, cy - s)
        lineTo(cx + s * 0.28f, cy - s * 0.3f)
        lineTo(cx + s * 0.62f, cy - s * 0.85f)
        lineTo(cx + s, cy)
        close()
    }
    drawPath(crown, GoldBright)
    drawCircle(GoldBorder, radius = s * 0.09f, center = Offset(cx - s * 0.36f, cy - s * 0.28f))
    drawCircle(Color(0xFF0A2F6B), radius = s * 0.09f, center = Offset(cx, cy - s * 0.34f))
    drawCircle(GoldBorder, radius = s * 0.09f, center = Offset(cx + s * 0.36f, cy - s * 0.28f))
}

// ============ TROOP BADGES ============
@Composable
private fun TroopBadge(name: String, count: Int, accent: Color) {
    Row(
        Modifier
            .background(PanelBrown.copy(alpha = 0.95f), RoundedCornerShape(12.dp))
            .border(1.dp, accent, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(if (name == "Goblin") "👺" else "🐲", fontSize = 18.sp)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(name, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text("x$count", color = accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

// ============ GAME BUTTON ============
@Composable
private fun GameButton(
    text: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val s = if (pressed) 0.92f else 1f
    Box(
        modifier = modifier
            .height(48.dp)
            .scale(s)
            .shadow(6.dp, RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(listOf(Color(0xFF8A6B2F), Color(0xFF5C4618), GoldDark)),
                RoundedCornerShape(12.dp)
            )
            .border(2.dp, GoldBorder, RoundedCornerShape(12.dp))
            .clickable(interactionSource = interaction, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

// ============ STORE SCREEN ============
@Composable
private fun StoreScreen(
    player: Player,
    onBack: () -> Unit,
    onBuyTroop: (String) -> Unit
) {
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(PanelLight, PanelDark))).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            GameButton("◀ Back", Modifier.width(90.dp), onBack)
            Spacer(Modifier.weight(1f))
            Text("TRADE POST", color = TextGold, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.weight(1f))
            ResourcePill("💰", player.resources.toString(), GoldDark)
        }
        Spacer(Modifier.height(14.dp))

        StoreCard("👺", "Goblin Warrior", "50 Gold", "Fast melee. DMG 40 HP 100", Color(0xFF66BB6A)) {
            onBuyTroop("Goblin")
        }
        Spacer(Modifier.height(10.dp))
        StoreCard("🐲", "Fire Dragon", "150 Gold", "Flying terror. DMG 100 HP 250", Color(0xFFEF5350)) {
            onBuyTroop("Dragon")
        }
        Spacer(Modifier.height(10.dp))
        StoreCard("🏰", "Tower Repair", "40 Gold", "+25 HP to your Town Hall", Color(0xFF42A5F5)) { }
    }
}

@Composable
private fun StoreCard(
    icon: String, name: String, cost: String, info: String,
    accent: Color, onBuy: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(PanelBrown, RoundedCornerShape(16.dp))
            .border(2.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 34.sp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(name, color = TextGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(info, color = TextGray, fontSize = 11.sp)
            Text(cost, color = accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        GameButton("Buy", Modifier.width(70.dp).height(40.dp), onBuy)
    }
}

// ============ ARMY SCREEN ============
@Composable
private fun ArmyScreen(player: Player, onBack: () -> Unit) {
    val goblins = player.army.count { it.type == "Goblin" }
    val dragons = player.army.count { it.type == "Dragon" }
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(PanelLight, PanelDark))).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            GameButton("◀ Back", Modifier.width(90.dp), onBack)
            Spacer(Modifier.weight(1f))
            Text("MY ARMY", color = TextGold, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.weight(1f))
            ResourcePill("💰", player.resources.toString(), GoldDark)
        }
        Spacer(Modifier.height(16.dp))

        ArmyCard("👺", "Goblin", goblins, 40, 100, Color(0xFF66BB6A))
        Spacer(Modifier.height(12.dp))
        ArmyCard("🐲", "Dragon", dragons, 100, 250, Color(0xFFEF5350))
    }
}

@Composable
private fun ArmyCard(
    icon: String, name: String, count: Int, dmg: Int, hp: Int, accent: Color
) {
    val xpRatio = 0.66f
    Column(
        Modifier
            .fillMaxWidth()
            .background(PanelBrown, RoundedCornerShape(16.dp))
            .border(2.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 40.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(name, color = TextGold, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("DMG $dmg   HP $hp   Owned: $count", color = TextWhite, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        // XP bar
        Box(Modifier.fillMaxWidth().height(14.dp).background(Color(0xFF1B1008), RoundedCornerShape(7.dp))) {
            Box(
                Modifier
                    .fillMaxWidth(xpRatio)
                    .height(14.dp)
                    .background(Brush.horizontalGradient(listOf(accent, GoldBright)), RoundedCornerShape(7.dp))
            )
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("LVL 3  XP ${(xpRatio * 100).toInt()}/100", color = TextWhite, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ============ SHOP ROW ============
@Composable
private fun ShopRow(label: String, cost: String, accent: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF5A3D24), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextWhite, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Text(cost, color = accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}