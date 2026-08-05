package com.clashgame.strategy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.nativeCanvas
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
import kotlin.math.sin

private val BgDark = Color(0xFF2A2A2E)
private val BgPanel = Color(0xFF3A3A3F)
private val BgPanelDark = Color(0xFF1E1E22)
private val GoldBright = Color(0xFFF5A623)
private val GoldMedium = Color(0xFFDAA520)
private val GoldDark = Color(0xFFC4841D)
private val GoldBorder = Color(0xFFFFD86B)
private val BannerBlue = Color(0xFF1565C0)
private val BannerBlueDark = Color(0xFF0D47A1)
private val GemGreen = Color(0xFF4CAF50)
private val GemGreenLight = Color(0xFF81C784)
private val ElixirPurple = Color(0xFF9C27B0)
private val EnemyRed = Color(0xFFC62828)
private val TextWhite = Color(0xFFF5F5F5)
private val TextGold = Color(0xFFFFD54F)
private val TextGray = Color(0xFFB0BEC5)

private sealed class Screen {
    object Home : Screen()
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
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    var battleActive by remember { mutableStateOf(false) }
    var showUpgrade by remember { mutableStateOf(false) }
    var showGate by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var messageColor by remember { mutableStateOf(TextGold) }
    var version by remember { mutableIntStateOf(0) }

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

    Box(Modifier.fillMaxSize().background(BgDark)) {
        when (screen) {
            Screen.Home -> HomeScreen(
                player = player,
                towerLevel = tower.level,
                towerHp = tower.health,
                message = message,
                messageColor = messageColor,
                version = version,
                onBattle = { battleActive = true },
                onStore = { screen = Screen.Store },
                onArmy = { screen = Screen.Army },
                onUpgrade = { showUpgrade = true },
                onGate = { showGate = true }
            )
            Screen.Store -> StoreScreen(
                player = player,
                onBack = { screen = Screen.Home },
                onBuyGoblin = {
                    version++
                    if (player.resources >= 50) {
                        player.resources -= 50
                        player.army.add(GoblinWarrior())
                        message = "Bought Goblin!"; messageColor = GemGreen
                    } else { message = "Not enough gold!"; messageColor = EnemyRed }
                },
                onBuyDragon = {
                    version++
                    if (player.resources >= 150) {
                        player.resources -= 150
                        player.army.add(Dragon())
                        message = "Bought Dragon!"; messageColor = GemGreen
                    } else { message = "Not enough gold!"; messageColor = EnemyRed }
                }
            )
            Screen.Army -> ArmyScreen(
                player = player,
                onBack = { screen = Screen.Home }
            )
        }

        if (battleActive) {
            Dialog(
                onDismissRequest = { battleActive = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                BattleScreen(
                    army = player.army.toList(),
                    towerName = "Enemy Tower",
                    towerHp = 400f,
                    onFinish = { won ->
                        if (won) { player.resources += 150; message = "VICTORY! +150 Gold"; messageColor = TextGold }
                        else { player.resources += 60; message = "DEFEAT +60 Gold"; messageColor = EnemyRed }
                        version++; battleActive = false
                    }
                )
            }
        }

        if (showUpgrade) {
            AlertDialog(
                onDismissRequest = { showUpgrade = false },
                containerColor = BgPanel,
                shape = RoundedCornerShape(20.dp),
                title = { Text("Upgrade to Level ${tower.level + 1}?", color = TextGold, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(60.dp).background(GoldDark.copy(alpha = 0.3f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) { Text("🏰", fontSize = 32.sp) }
                            Spacer(Modifier.width(12.dp))
                            Column { Text("Town Hall", color = TextWhite, fontWeight = FontWeight.Bold); Text("Upgrade time: 6d", color = TextGray, fontSize = 11.sp) }
                        }
                        StatBar("Hitpoints", tower.health, tower.health + 100, GemGreen)
                        StatBar("Storage", tower.level * 100000, (tower.level + 1) * 100000, GoldBright)
                        Text("Unlocks:", color = TextGold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(4) { Box(Modifier.size(36.dp).background(BannerBlue.copy(alpha = 0.3f), RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) { Text("🏗️", fontSize = 18.sp) } }
                        }
                        Text("Note: Upgrading reduces loot from opponents.", color = TextGray, fontSize = 9.sp)
                    }
                },
                confirmButton = {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        GameButton("💰 200 Gold", Modifier.width(180.dp)) {
                            if (player.resources >= 200) {
                                player.resources -= 200; tower.upgrade(); version++
                                message = "Upgraded! Lv${tower.level}"; messageColor = TextGold
                            } else { message = "Need 200 Gold!"; messageColor = EnemyRed; version++ }
                            showUpgrade = false
                        }
                    }
                }
            )
        }

        if (showGate) {
            AlertDialog(
                onDismissRequest = { showGate = false },
                containerColor = BgPanel,
                shape = RoundedCornerShape(20.dp),
                title = { Text("Guild Gate", color = TextGold, fontWeight = FontWeight.Bold) },
                text = { Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { guildGate.getMemberDetails().forEach { d -> Text(d, color = TextWhite, fontSize = 13.sp) } } },
                confirmButton = { TextButton(onClick = { showGate = false }) { Text("Close", color = TextGold) } }
            )
        }
    }
}

// =================== HOME SCREEN ===================
@Composable
private fun HomeScreen(
    player: Player, towerLevel: Int, towerHp: Int,
    message: String, messageColor: Color, version: Int,
    onBattle: () -> Unit, onStore: () -> Unit, onArmy: () -> Unit,
    onUpgrade: () -> Unit, onGate: () -> Unit
) {
    val infinite = rememberInfiniteTransition()
    val glowPulse by infinite.animateFloat(0.6f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse))

    Column(Modifier.fillMaxSize().background(BgDark)) {
        // ===== TOP HUD BAR =====
        HudBar(player = player, towerLevel = towerLevel)

        // ===== MESSAGE TOAST =====
        if (version > 0 && message.isNotEmpty()) {
            Text(
                message, color = messageColor, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 2.dp)
                    .background(BgPanelDark, RoundedCornerShape(6.dp)).padding(4.dp)
            )
        }

        // ===== MAIN CONTENT =====
        Row(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp)) {
            // LEFT: Chest slots
            Column(
                Modifier.width(90.dp).fillMaxHeight().padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ChestSlot("Open", "2:25", GoldBright, true)
                ChestSlot("Open now", null, GoldBright, true)
                ChestSlot("Locked", "3h", BannerBlue, false)
                ChestSlot("Empty", null, BgPanel, false)
            }

            // CENTER: Shield + Battle
            Column(
                Modifier.weight(1f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ShieldBadge(towerLevel = towerLevel, towerHp = towerHp)
                Spacer(Modifier.height(8.dp))
                BattleButton(glowAlpha = glowPulse, onClick = onBattle)
            }

            // RIGHT: Action panels
            Column(
                Modifier.width(110.dp).fillMaxHeight().padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ActionPanel("🏆 Victory Chest", "6:10", GoldBright, onStore)
                ActionPanel("🎁 Free Chest", "3:52", BannerBlue, onStore)
                ActionPanel("💰 Store", null, GoldBright, onStore)
                ActionPanel("🃏 Cards", null, BannerBlue, onArmy)
            }
        }

        // ===== BOTTOM: Quick actions =====
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            QuickBtn("🏰 Upgrade", GoldDark, Modifier.weight(1f), onUpgrade)
            QuickBtn("🚪 Guild", BannerBlueDark, Modifier.weight(1f), onGate)
            QuickBtn("🛒 Shop", GoldDark, Modifier.weight(1f), onStore)
        }

        // ===== BOTTOM NAV BAR =====
        NavBar()
    }
}

// =================== HUD BAR ===================
@Composable
private fun HudBar(player: Player, towerLevel: Int) {
    Row(
        Modifier.fillMaxWidth().height(36.dp)
            .background(Brush.horizontalGradient(listOf(BgPanelDark, BgPanel, BgPanelDark)))
            .border(1.dp, GoldBorder.copy(alpha = 0.4f))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Settings gear
        Text("⚙️", fontSize = 16.sp)

        // Level badge
        Box(Modifier.padding(start = 6.dp).background(BannerBlue, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
            Text("Lv$towerLevel", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }

        // HP bar
        Box(Modifier.width(60.dp).height(8.dp).padding(start = 4.dp).background(Color(0xFF1C1C1E), RoundedCornerShape(4.dp))) {
            Box(Modifier.fillMaxWidth(0.8f).height(8.dp).background(GemGreen, RoundedCornerShape(4.dp)))
        }

        Spacer(Modifier.width(6.dp))

        // Player name
        Text(player.username, color = TextGold, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))

        // Trophies
        Pill("🏆", "1013")

        Spacer(Modifier.width(4.dp))

        // Gold
        Pill("💰", player.resources.toString())

        Spacer(Modifier.width(4.dp))

        // Gems
        Pill("💎", "99559")
    }
}

@Composable
private fun Pill(icon: String, value: String) {
    Row(
        Modifier.background(Color(0xCCFFFFFF), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 11.sp)
        Spacer(Modifier.width(2.dp))
        Text(value, color = BgDark, fontWeight = FontWeight.Bold, fontSize = 10.sp)
    }
}

// =================== CHEST SLOT ===================
@Composable
private fun ChestSlot(label: String, timer: String?, color: Color, openable: Boolean) {
    Box(
        Modifier.fillMaxWidth().height(56.dp)
            .background(BgPanel, RoundedCornerShape(8.dp))
            .border(2.dp, if (openable) color else color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .clickable { }
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (openable) "📦" else if (label == "Locked") "🔒" else "📭", fontSize = 18.sp)
            Spacer(Modifier.height(2.dp))
            Text(label, color = if (openable) color else TextGray, fontWeight = FontWeight.Bold, fontSize = 9.sp)
            if (timer != null) Text(timer, color = TextGray, fontSize = 8.sp)
        }
    }
}

// =================== SHIELD BADGE ===================
@Composable
private fun ShieldBadge(towerLevel: Int, towerHp: Int) {
    Box(Modifier.width(180.dp).height(190.dp), contentAlignment = Alignment.TopCenter) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f + 12f
            val s = size.width * 0.36f

            // Shield border (gold)
            val border = Path().apply {
                moveTo(cx, cy - s * 1.14f)
                cubicTo(cx + s * 1.1f, cy - s * 0.9f, cx + s * 1.2f, cy - s * 0.05f, cx + s * 0.85f, cy + s * 0.5f)
                lineTo(cx, cy + s * 1.18f)
                lineTo(cx - s * 0.85f, cy + s * 0.5f)
                cubicTo(cx - s * 1.2f, cy - s * 0.05f, cx - s * 1.1f, cy - s * 0.9f, cx, cy - s * 1.14f)
                close()
            }
            drawPath(border, GoldBright)

            // Shield body (blue gradient)
            val shield = Path().apply {
                moveTo(cx, cy - s)
                cubicTo(cx + s * 0.95f, cy - s * 0.8f, cx + s * 1.08f, cy - s * 0.1f, cx + s * 0.75f, cy + s * 0.45f)
                lineTo(cx, cy + s)
                lineTo(cx - s * 0.75f, cy + s * 0.45f)
                cubicTo(cx - s * 1.08f, cy - s * 0.1f, cx - s * 0.95f, cy - s * 0.8f, cx, cy - s)
                close()
            }
            drawPath(shield, Brush.linearGradient(listOf(BannerBlue, BannerBlueDark)))

            // Shield emblem: green field with gold castle
            drawRect(GemGreen.copy(alpha = 0.4f), topLeft = Offset(cx - s * 0.3f, cy - s * 0.15f), size = Size(s * 0.6f, s * 0.6f))
            drawRect(GoldBright, topLeft = Offset(cx - s * 0.15f, cy - s * 0.05f), size = Size(s * 0.3f, s * 0.35f))
            drawRect(BannerBlue, topLeft = Offset(cx - s * 0.08f, cy + s * 0.05f), size = Size(s * 0.16f, s * 0.18f))

            // Crown on top
            val crownY = cy - s - 18f
            val crown = Path().apply {
                moveTo(cx - s * 0.5f, crownY)
                lineTo(cx - s * 0.32f, crownY - s * 0.45f)
                lineTo(cx - s * 0.12f, crownY - s * 0.15f)
                lineTo(cx, crownY - s * 0.5f)
                lineTo(cx + s * 0.12f, crownY - s * 0.15f)
                lineTo(cx + s * 0.32f, crownY - s * 0.45f)
                lineTo(cx + s * 0.5f, crownY)
                close()
            }
            drawPath(crown, GoldBright)
            drawCircle(GoldBorder, radius = s * 0.06f, center = Offset(cx, crownY - s * 0.18f))

            // Swords behind shield
            drawLine(GoldDark, Offset(cx - s * 0.9f, cy + s * 0.3f), Offset(cx + s * 0.5f, cy - s * 0.6f), strokeWidth = s * 0.07f)
            drawLine(GoldDark, Offset(cx + s * 0.9f, cy + s * 0.3f), Offset(cx - s * 0.5f, cy - s * 0.6f), strokeWidth = s * 0.07f)
        }

        // "Castle 1" label
        Column(
            Modifier.align(Alignment.BottomCenter).width(150.dp)
                .background(BannerBlue, RoundedCornerShape(8.dp))
                .border(1.dp, GoldBorder, RoundedCornerShape(8.dp))
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Castle $towerLevel", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("HP $towerHp", color = TextGold, fontSize = 10.sp)
        }
    }
}

// =================== BATTLE BUTTON ===================
@Composable
private fun BattleButton(glowAlpha: Float, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, spring(dampingRatio = 0.45f, stiffness = 280f))

    Box(
        Modifier.width(180.dp).height(64.dp).scale(scale)
            .shadow(8.dp, RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(GoldBright, GoldDark)), RoundedCornerShape(16.dp))
            .border(3.dp, GoldBorder, RoundedCornerShape(16.dp))
            .clickable(interactionSource = interaction, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Color(0x444CAF50), radius = size.width * 0.4f, center = Offset(size.width / 2f, size.height * 0.5f))
        }
        Text("BATTLE", color = TextWhite, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, letterSpacing = 3.sp)
    }
}

// =================== ACTION PANEL ===================
@Composable
private fun ActionPanel(label: String, timer: String?, color: Color, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(42.dp)
            .background(BgPanel, RoundedCornerShape(8.dp))
            .border(1.5.dp, color.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column {
            Text(label, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 10.sp, maxLines = 1)
            if (timer != null) Text("⏰ $timer", color = color, fontSize = 9.sp)
        }
    }
}

// =================== QUICK BUTTONS ===================
@Composable
private fun QuickBtn(text: String, bg: Color, modifier: Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.93f else 1f, spring(dampingRatio = 0.5f, stiffness = 300f))
    Box(
        modifier.height(32.dp).scale(s)
            .shadow(4.dp, RoundedCornerShape(8.dp))
            .background(Brush.verticalGradient(listOf(bg.copy(alpha = 0.9f), bg)), RoundedCornerShape(8.dp))
            .border(1.5.dp, GoldBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .clickable(interactionSource = interaction, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) { Text(text, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
}

// =================== BOTTOM NAV BAR ===================
@Composable
private fun NavBar() {
    Row(
        Modifier.fillMaxWidth().height(40.dp)
            .background(BgPanelDark)
            .border(1.dp, GoldBorder.copy(alpha = 0.25f))
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavIcon("💬")
        NavIcon("⚔️")
        NavIcon("🏆")
        NavIcon("⚙️")
    }
}

@Composable
private fun NavIcon(icon: String) {
    Box(Modifier.size(32.dp).background(BgPanel, RoundedCornerShape(6.dp)).clickable { }, contentAlignment = Alignment.Center) {
        Text(icon, fontSize = 18.sp)
    }
}

// =================== STORE SCREEN ===================
@Composable
private fun StoreScreen(player: Player, onBack: () -> Unit, onBuyGoblin: () -> Unit, onBuyDragon: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("ARMY", "RESOURCES", "DEFENSES")

    Column(Modifier.fillMaxSize().background(BgDark).padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            GameButton("◀ Back", Modifier.width(80.dp).height(34.dp), onBack)
            Spacer(Modifier.weight(1f))
            Text("BUILDINGS & TROOPS", color = TextGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.weight(1f))
            Pill("💰", player.resources.toString())
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            tabs.forEachIndexed { i, t ->
                Box(
                    Modifier.padding(horizontal = 3.dp)
                        .background(if (tab == i) GoldBright else BgPanel, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .border(1.dp, if (tab == i) GoldBorder else Color.Transparent, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .clickable { tab = i }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) { Text(t, color = if (tab == i) BgDark else TextGray, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
            }
        }
        Column(Modifier.weight(1f).background(BgPanel).border(1.dp, GoldBorder.copy(alpha = 0.3f)).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            when (tab) {
                0 -> { StoreCard("👺", "Goblin Warrior", "50 Gold", "Fast melee", GemGreen, onBuyGoblin); StoreCard("🐲", "Fire Dragon", "150 Gold", "Flying terror", EnemyRed, onBuyDragon); StoreCard("🛡️", "Shield Spell", "80 Gold", "+30 DEF", BannerBlue, onBuyGoblin); StoreCard("🔮", "Heal Spell", "100 Gold", "Restores HP", ElixirPurple, onBuyDragon) }
                1 -> { StoreCard("💰", "Gold Mine", "200 Gold", "Produces gold", GoldBright, onBuyGoblin); StoreCard("🧪", "Elixir Collector", "250 Gold", "Produces elixir", ElixirPurple, onBuyDragon); StoreCard("💎", "Gem Mine", "500 Gold", "Produces gems", GemGreen, onBuyGoblin) }
                2 -> { StoreCard("🏹", "Archer Tower", "300 Gold", "Long range", EnemyRed, onBuyGoblin); StoreCard("💣", "Bomb Tower", "400 Gold", "Splash DMG", GoldDark, onBuyDragon); StoreCard("🔥", "Inferno Tower", "600 Gold", "Rising DMG", EnemyRed, onBuyGoblin) }
            }
        }
    }
}

@Composable
private fun StoreCard(icon: String, name: String, cost: String, info: String, accent: Color, onBuy: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().fillMaxHeight().background(BgDark, RoundedCornerShape(10.dp))
            .border(1.5.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(48.dp).background(accent.copy(alpha = 0.2f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) { Text(icon, fontSize = 26.sp) }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) { Text(name, color = TextGold, fontWeight = FontWeight.Bold, fontSize = 13.sp); Text(info, color = TextGray, fontSize = 10.sp); Text(cost, color = accent, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
        GameButton("Buy", Modifier.width(52.dp).height(30.dp), onBuy)
    }
}

// =================== ARMY SCREEN ===================
@Composable
private fun ArmyScreen(player: Player, onBack: () -> Unit) {
    val goblins = player.army.count { it.type == "Goblin" }
    val dragons = player.army.count { it.type == "Dragon" }

    Column(Modifier.fillMaxSize().background(BgDark).padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            GameButton("◀ Back", Modifier.width(80.dp).height(34.dp), onBack)
            Spacer(Modifier.weight(1f))
            Text("MY ARMY", color = TextGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.weight(1f))
            Pill("💰", player.resources.toString())
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ArmyCard("👺", "Goblin", goblins, 40, 100, 3, 19, 20, GemGreen, Modifier.weight(1f))
            ArmyCard("🐲", "Dragon", dragons, 100, 250, 5, 184, 200, EnemyRed, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ArmyCard(icon: String, name: String, count: Int, dmg: Int, hp: Int, level: Int, xp: Int, xpMax: Int, accent: Color, modifier: Modifier) {
    Column(
        modifier.background(BgPanel, RoundedCornerShape(12.dp))
            .border(2.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).background(accent.copy(alpha = 0.2f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { Text(icon, fontSize = 30.sp) }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, color = TextGold, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.width(6.dp))
                    Box(Modifier.background(accent, RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 1.dp)) {
                        Text("Lv$level", color = TextWhite, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text("DMG $dmg  HP $hp  Owned: $count", color = TextWhite, fontSize = 10.sp)
            }
        }
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(10.dp).background(Color(0xFF1C1C1E), RoundedCornerShape(5.dp))) {
            Box(Modifier.fillMaxWidth(xp.toFloat() / xpMax).height(10.dp).background(Brush.horizontalGradient(listOf(accent, GoldBright)), RoundedCornerShape(5.dp)))
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("XP $xp/$xpMax", color = TextWhite, fontSize = 7.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(5) { i ->
                Box(Modifier.size(28.dp).background(if (i < level) accent else BgDark, RoundedCornerShape(4.dp)).border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                    Text("★", color = if (i < level) TextGold else TextGray, fontSize = 12.sp)
                }
            }
        }
    }
}

// =================== SHARED COMPONENTS ===================
@Composable
private fun GameButton(text: String, modifier: Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.92f else 1f, spring(dampingRatio = 0.5f, stiffness = 300f))
    Box(
        modifier.scale(s).shadow(4.dp, RoundedCornerShape(8.dp))
            .background(Brush.verticalGradient(listOf(GoldBright, GoldDark)), RoundedCornerShape(8.dp))
            .border(1.5.dp, GoldBorder, RoundedCornerShape(8.dp))
            .clickable(interactionSource = interaction, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) { Text(text, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
}

@Composable
private fun StatBar(label: String, current: Int, max: Int, color: Color) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = TextWhite, fontSize = 10.sp)
            Text("$current/$max", color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.fillMaxWidth().height(7.dp).background(Color(0xFF1C1C1E), RoundedCornerShape(4.dp))) {
            Box(Modifier.fillMaxWidth((current.toFloat() / max).coerceIn(0f, 1f)).height(7.dp).background(color, RoundedCornerShape(4.dp)))
        }
    }
}