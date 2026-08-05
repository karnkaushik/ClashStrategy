package com.clashgame.strategy

import android.graphics.Paint
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.clashgame.strategy.model.Dragon
import com.clashgame.strategy.model.GoblinWarrior
import com.clashgame.strategy.model.Guild
import com.clashgame.strategy.model.GuildGate
import com.clashgame.strategy.model.Player
import kotlin.random.Random
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.lerp

// =================== COLOR PALETTE ===================
private val BgDeep = Color(0xFF0D0D1A)
private val BgDark = Color(0xFF12121F)
private val BgPanel = Color(0xFF1A1A2E)
private val BgPanelLight = Color(0xFF252540)
private val BgCard = Color(0xFF1E1E35)

private val GoldBright = Color(0xFFFFD700)
private val GoldMedium = Color(0xFFFFA500)
private val GoldDark = Color(0xFFCC8400)
private val GoldGlow = Color(0x66FFD700)

private val RoyalBlue = Color(0xFF4FC3F7)
private val RoyalBlueDark = Color(0xFF0288D1)
private val RoyalBlueGlow = Color(0x444FC3F7)

private val MysticPurple = Color(0xFF9C27B0)
private val MysticPurpleLight = Color(0xFFCE93D8)

private val Emerald = Color(0xFF00B894)
private val EmeraldLight = Color(0xFF55EFC4)
private val EmeraldDark = Color(0xFF00796B)

private val Ruby = Color(0xFFE74C3C)
private val RubyLight = Color(0xFFFF6B6B)

private val TextWhite = Color(0xFFF5F5F5)
private val TextGold = Color(0xFFFFD54F)
private val TextGray = Color(0xFF9E9E9E)
private val TextDim = Color(0xFF616161)

// Rarity colors
private val RarityCommon = Color(0xFF9E9E9E)
private val RarityRare = Color(0xFF2196F3)
private val RarityEpic = Color(0xFF9C27B0)
private val RarityLegendary = Color(0xFFFF9800)
private val RarityMythic = Color(0xFFF44336)

// Element colors
private val ElementFire = Color(0xFFFF5722)
private val ElementEarth = Color(0xFF4CAF50)
private val ElementWater = Color(0xFF03A9F4)
private val ElementArcane = Color(0xFF9C27B0)
private val ElementThunder = Color(0xFFFFEB3B)
private val ElementIce = Color(0xFF00BCD4)

// =================== DATA MODELS ===================
private enum class Rarity { COMMON, RARE, EPIC, LEGENDARY, MYTHIC }
private enum class Element { FIRE, EARTH, WATER, ARCANE, THUNDER, ICE }

private data class HeroData(
    val icon: String, val name: String, val rarity: Rarity,
    val element: Element, val level: Int, val maxLevel: Int,
    val hp: Int, val maxHp: Int, val dmg: Int, val maxDmg: Int,
    val xp: Int, val xpMax: Int, val stars: Int
)

private data class ShopItemData(
    val icon: String, val name: String, val desc: String,
    val cost: Int, val statLabel: String, val statValue: String,
    val accent: Color
)

private sealed class Screen {
    object Home : Screen()
    object Heroes : Screen()
    object Shop : Screen()
    object Clan : Screen()
}

// =================== MAIN ACTIVITY ===================
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GameApp() }
    }
}

// =================== GAME APP ROOT ===================
@Composable
fun GameApp() {
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    var battleActive by remember { mutableStateOf(false) }
    var showUpgrade by remember { mutableStateOf(false) }
    var showPremium by remember { mutableStateOf(false) }
    var showHeroDetail by remember { mutableStateOf<HeroData?>(null) }
    var message by remember { mutableStateOf("") }
    var messageColor by remember { mutableStateOf(TextGold) }
    var version by remember { mutableIntStateOf(0) }

    val player = remember { Player("Chief Kyle", 500) }
    val guild = remember {
        Guild("Dragon Slayers").also {
            it.addMember(player)
            it.addMember(Player("Vikram", 400))
            it.addMember(Player("Maya", 300))
        }
    }
    val guildGate = remember { GuildGate(guild) }
    val tower = player.tower

    val heroes = remember {
        listOf(
            HeroData("🐉", "Fire Drake", Rarity.LEGENDARY, Element.FIRE, 5, 10, 500, 500, 100, 100, 184, 200, 5),
            HeroData("👺", "Shadow Goblin", Rarity.RARE, Element.EARTH, 3, 10, 150, 200, 40, 60, 80, 120, 3),
            HeroData("🧙", "Arcane Mage", Rarity.EPIC, Element.ARCANE, 1, 10, 120, 300, 80, 120, 20, 100, 1),
            HeroData("🗡️", "Iron Knight", Rarity.COMMON, Element.EARTH, 2, 10, 200, 250, 50, 70, 60, 80, 2),
            HeroData("🏹", "Swift Archer", Rarity.RARE, Element.WATER, 4, 10, 100, 180, 60, 90, 140, 160, 4),
            HeroData("⚡", "Storm Caller", Rarity.MYTHIC, Element.THUNDER, 1, 10, 180, 400, 120, 180, 10, 200, 1),
            HeroData("❄️", "Frost Queen", Rarity.EPIC, Element.ICE, 3, 10, 160, 280, 70, 110, 100, 140, 3),
            HeroData("🔥", "Inferno Lord", Rarity.LEGENDARY, Element.FIRE, 2, 10, 400, 450, 90, 130, 50, 160, 2),
        )
    }

    Box(Modifier.fillMaxSize().background(BgDeep)) {
        when (screen) {
            Screen.Home -> HomeScreen(
                player = player, towerLevel = tower.level, towerHp = tower.health,
                message = message, messageColor = messageColor, version = version,
                onBattle = { battleActive = true },
                onHeroes = { screen = Screen.Heroes },
                onShop = { screen = Screen.Shop },
                onClan = { screen = Screen.Clan },
                onUpgrade = { showUpgrade = true },
                onPremium = { showPremium = true }
            )
            Screen.Heroes -> HeroesScreen(
                heroes = heroes, player = player,
                onBack = { screen = Screen.Home },
                onHeroClick = { showHeroDetail = it }
            )
            Screen.Shop -> ShopScreen(
                player = player, onBack = { screen = Screen.Home },
                onBuyGoblin = {
                    version++
                    if (player.resources >= 50) {
                        player.resources -= 50; player.army.add(GoblinWarrior())
                        message = "Recruited Goblin Warrior!"; messageColor = Emerald
                    } else { message = "Not enough gold!"; messageColor = Ruby }
                },
                onBuyDragon = {
                    version++
                    if (player.resources >= 150) {
                        player.resources -= 150; player.army.add(Dragon())
                        message = "Recruited Fire Dragon!"; messageColor = Emerald
                    } else { message = "Not enough gold!"; messageColor = Ruby }
                },
                onPremium = { showPremium = true }
            )
            Screen.Clan -> ClanScreen(
                guild = guild, guildGate = guildGate, player = player,
                onBack = { screen = Screen.Home }
            )
        }

        if (battleActive) {
            Dialog(
                onDismissRequest = { battleActive = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                BattleScreen(
                    army = player.army.toList(), towerName = "Enemy Tower", towerHp = 400f,
                    onFinish = { won ->
                        if (won) { player.resources += 150; message = "VICTORY! +150 Gold"; messageColor = GoldBright }
                        else { player.resources += 60; message = "DEFEAT +60 Gold"; messageColor = Ruby }
                        version++; battleActive = false
                    }
                )
            }
        }

        if (showUpgrade) {
            UpgradeDialog(
                towerLevel = tower.level, towerHp = tower.health,
                onDismiss = { showUpgrade = false },
                onUpgrade = {
                    if (player.resources >= 200) {
                        player.resources -= 200; tower.upgrade(); version++
                        message = "Upgraded to Level ${tower.level}!"; messageColor = GoldBright
                    } else { message = "Need 200 Gold!"; messageColor = Ruby; version++ }
                    showUpgrade = false
                }
            )
        }

        if (showPremium) {
            PremiumDialog(
                onDismiss = { showPremium = false },
                onBuy = { amount, gems ->
                    version++; message = "Purchased $gems Gems!"; messageColor = EmeraldLight
                    showPremium = false
                }
            )
        }

        showHeroDetail?.let { hero ->
            HeroDetailDialog(hero = hero, onDismiss = { showHeroDetail = null })
        }
    }
}

// =================== HOME SCREEN (VILLAGE) ===================
@Composable
private fun HomeScreen(
    player: Player, towerLevel: Int, towerHp: Int,
    message: String, messageColor: Color, version: Int,
    onBattle: () -> Unit, onHeroes: () -> Unit, onShop: () -> Unit,
    onClan: () -> Unit, onUpgrade: () -> Unit, onPremium: () -> Unit
) {
    val infinite = rememberInfiniteTransition()
    val glowPulse by infinite.animateFloat(0.5f, 1f, infiniteRepeatable(tween(1200), RepeatMode.Reverse))
    val arrowBounce by infinite.animateFloat(0f, 8f, infiniteRepeatable(tween(600), RepeatMode.Reverse))

    Column(Modifier.fillMaxSize().background(BgDeep)) {
        ResourceBar(player = player, towerLevel = towerLevel, onPremium = onPremium)

        if (version > 0 && message.isNotEmpty()) {
            Text(
                message, color = messageColor, fontWeight = FontWeight.Bold, fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 2.dp)
                    .background(BgPanel, RoundedCornerShape(8.dp)).padding(4.dp)
            )
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            IsometricVillage(towerLevel = towerLevel, arrowOffset = arrowBounce)

            Column(
                Modifier.align(Alignment.Center).padding(top = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GlossyButton(
                    text = "⚔️  BATTLE", width = 200.dp, height = 56.dp,
                    gradient = listOf(GoldBright, GoldDark), glowAlpha = glowPulse,
                    onClick = onBattle
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlossyButton("🏰 Upgrade", 100.dp, 36.dp, listOf(Emerald, EmeraldDark), onClick = onUpgrade)
                    GlossyButton("🎁 Free", 80.dp, 36.dp, listOf(RoyalBlue, RoyalBlueDark), onClick = onShop)
                }
            }
        }

        BottomNavBar(
            currentScreen = "home",
            onAttack = onBattle, onHeroes = onHeroes,
            onShop = onShop, onClan = onClan
        )
    }
}

// =================== ISOMETRIC VILLAGE ===================
@Composable
private fun IsometricVillage(towerLevel: Int, arrowOffset: Float) {
    Canvas(Modifier.fillMaxSize().background(BgDeep)) {
        val w = size.width
        val h = size.height
        val tileW = 80f
        val tileH = 40f
        val centerX = w * 0.5f
        val centerY = h * 0.45f

        // Sky gradient
        drawRect(
            Brush.verticalGradient(listOf(Color(0xFF0A0A1A), Color(0xFF151530), Color(0xFF1A1A2E))),
            size = Size(w, h)
        )

        // Stars
        repeat(20) { i ->
            val sx = w * (0.05f + 0.9f * (i * 7 % 20) / 20f)
            val sy = h * (0.03f + 0.15f * (i * 3 % 5))
            drawCircle(Color.White.copy(alpha = 0.3f + 0.2f * (i % 3)), radius = 1.5f, center = Offset(sx, sy))
        }

        // Ground plane (isometric)
        val groundColor = Color(0xFF1B5E20)
        val groundLight = Color(0xFF2E7D32)
        val rows = 6
        val cols = 8

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val isoX = centerX + (col - cols / 2f) * tileW * 0.5f + (row - rows / 2f) * tileW * 0.5f
                val isoY = centerY + (col - cols / 2f) * tileH * 0.5f - (row - rows / 2f) * tileH * 0.5f + 60f

                if (isoX > -tileW && isoX < w + tileW && isoY > 0 && isoY < h) {
                    val tile = Path().apply {
                        moveTo(isoX, isoY - tileH / 2)
                        lineTo(isoX + tileW / 2, isoY)
                        lineTo(isoX, isoY + tileH / 2)
                        lineTo(isoX - tileW / 2, isoY)
                        close()
                    }
                    val tileColor = if ((row + col) % 2 == 0) groundColor else groundLight
                    drawPath(tile, tileColor)
                    drawPath(tile, Color(0xFF1B5E20).copy(alpha = 0.3f), style = Stroke(1f))
                }
            }
        }

        // Buildings
        drawIsometricBuilding(centerX - 120f, centerY + 30f, 50f, 70f, Color(0xFF795548), Color(0xFF5D4037), "🏠")
        drawIsometricBuilding(centerX + 80f, centerY + 10f, 40f, 55f, Color(0xFFFF8F00), Color(0xFFEF6C00), "⛏️")
        drawIsometricBuilding(centerX - 20f, centerY - 30f, 60f, 90f, Color(0xFF1565C0), Color(0xFF0D47A1), "🏰")
        drawIsometricBuilding(centerX + 140f, centerY + 50f, 35f, 50f, Color(0xFF388E3C), Color(0xFF2E7D32), "🌾")
        drawIsometricBuilding(centerX - 180f, centerY + 60f, 45f, 60f, Color(0xFF616161), Color(0xFF424242), "🛡️")

        // Trees
        drawTree(centerX - 250f, centerY + 40f)
        drawTree(centerX + 220f, centerY + 70f)
        drawTree(centerX - 60f, centerY + 80f)

        // River
        val riverPath = Path().apply {
            moveTo(0f, centerY + 100f)
            cubicTo(w * 0.25f, centerY + 80f, w * 0.5f, centerY + 120f, w * 0.75f, centerY + 90f)
            cubicTo(w * 0.85f, centerY + 80f, w * 0.95f, centerY + 100f, w, centerY + 95f)
        }
        drawPath(riverPath, Color(0xFF0288D1).copy(alpha = 0.5f), style = Stroke(12f, cap = StrokeCap.Round))
        drawPath(riverPath, Color(0xFF4FC3F7).copy(alpha = 0.3f), style = Stroke(6f, cap = StrokeCap.Round))

        // Upgrade arrow on main tower
        val arrowCX = centerX - 20f
        val arrowCY = centerY - 80f - arrowOffset
        drawCircle(Emerald.copy(alpha = 0.6f), radius = 14f, center = Offset(arrowCX, arrowCY))
        val arrowPath = Path().apply {
            moveTo(arrowCX, arrowCY - 8f)
            lineTo(arrowCX + 6f, arrowCY)
            lineTo(arrowCX - 6f, arrowCY)
            close()
        }
        drawPath(arrowPath, EmeraldLight)

        // Build radius circle
        drawCircle(
            RoyalBlue.copy(alpha = 0.1f),
            radius = 160f,
            center = Offset(centerX - 20f, centerY),
            style = Stroke(2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
        )
    }
}

private fun DrawScope.drawIsometricBuilding(cx: Float, cy: Float, bw: Float, bh: Float, color: Color, darkColor: Color, icon: String) {
    // Shadow
    drawOval(Color(0x44000000), topLeft = Offset(cx - bw, cy + bh * 0.1f), size = Size(bw * 2, bh * 0.2f))

    // Body (isometric box)
    val body = Path().apply {
        moveTo(cx - bw / 2, cy)
        lineTo(cx, cy - bh * 0.4f)
        lineTo(cx + bw / 2, cy)
        lineTo(cx, cy + bh * 0.4f)
        close()
    }
    drawPath(body, color)

    // Top face
    val top = Path().apply {
        moveTo(cx, cy - bh * 0.4f)
        lineTo(cx + bw / 2, cy - bh * 0.4f + bh * 0.2f)
        lineTo(cx, cy - bh * 0.4f + bh * 0.4f)
        lineTo(cx - bw / 2, cy - bh * 0.4f + bh * 0.2f)
        close()
    }
    drawPath(top, lerp(color, Color.White, 0.2f))

    // Roof
    drawRect(darkColor, topLeft = Offset(cx - bw * 0.3f, cy - bh * 0.7f), size = Size(bw * 0.6f, bh * 0.35f))

    // Icon text
    android.graphics.Paint().apply {
        textAlign = android.graphics.Paint.Align.CENTER
        textSize = 24f
        isAntiAlias = true
    }.let { paint ->
        drawContext.canvas.nativeCanvas.drawText(icon, cx, cy + bh * 0.1f, paint)
    }
}

private fun DrawScope.drawTree(x: Float, y: Float) {
    // Trunk
    drawRect(Color(0xFF5D4037), topLeft = Offset(x - 3f, y), size = Size(6f, 20f))
    // Foliage
    drawCircle(Color(0xFF2E7D32), radius = 14f, center = Offset(x, y - 8f))
    drawCircle(Color(0xFF43A047), radius = 10f, center = Offset(x - 4f, y - 12f))
    drawCircle(Color(0xFF388E3C), radius = 8f, center = Offset(x + 5f, y - 10f))
}

// =================== RESOURCE BAR ===================
@Composable
private fun ResourceBar(player: Player, towerLevel: Int, onPremium: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(44.dp)
            .background(Brush.horizontalGradient(listOf(BgPanel, BgPanelLight, BgPanel)))
            .border(1.dp, GoldGlow)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Settings
        Box(Modifier.size(32.dp).background(BgCard, CircleShape).clickable { }, contentAlignment = Alignment.Center) {
            Text("⚙️", fontSize = 16.sp)
        }

        Spacer(Modifier.width(6.dp))

        // Level badge
        Box(Modifier.background(Brush.verticalGradient(listOf(RoyalBlue, RoyalBlueDark)), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
            Text("Lv$towerLevel", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }

        Spacer(Modifier.width(8.dp))

        // HP bar
        Box(Modifier.width(50.dp).height(8.dp).background(Color(0xFF1C1C1E), RoundedCornerShape(4.dp))) {
            Box(Modifier.fillMaxWidth(0.8f).height(8.dp).background(Brush.horizontalGradient(listOf(Emerald, EmeraldLight)), RoundedCornerShape(4.dp)))
        }

        Spacer(Modifier.width(8.dp))

        // Player name
        Text(player.username, color = TextGold, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))

        // Resource pills
        ResourcePill("💰", player.resources.toString(), GoldBright)
        Spacer(Modifier.width(4.dp))
        ResourcePill("💎", "99559", RoyalBlue, onClick = onPremium)
        Spacer(Modifier.width(4.dp))
        ResourcePill("🧪", "1250", MysticPurple)
        Spacer(Modifier.width(4.dp))
        ResourcePill("🍖", "847", ElementFire)
    }
}

@Composable
private fun ResourcePill(icon: String, value: String, color: Color, onClick: () -> Unit = {}) {
    Row(
        Modifier.background(Brush.verticalGradient(listOf(BgCard, BgPanel)), RoundedCornerShape(8.dp))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 11.sp)
        Spacer(Modifier.width(2.dp))
        Text(value, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 10.sp)
    }
}

// =================== HEROES SCREEN ===================
@Composable
private fun HeroesScreen(
    heroes: List<HeroData>, player: Player,
    onBack: () -> Unit, onHeroClick: (HeroData) -> Unit
) {
    Column(Modifier.fillMaxSize().background(BgDeep).padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            GlossyButton("◀ Back", 80.dp, 34.dp, listOf(BgPanelLight, BgPanel), onClick = onBack)
            Spacer(Modifier.weight(1f))
            Text("HEROES", color = TextGold, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.weight(1f))
            ResourcePill("💰", player.resources.toString(), GoldBright)
        }
        Spacer(Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(heroes) { hero ->
                HeroCard(hero = hero, onClick = { onHeroClick(hero) })
            }
        }
    }
}

@Composable
private fun HeroCard(hero: HeroData, onClick: () -> Unit) {
    val rarityColor = when (hero.rarity) {
        Rarity.COMMON -> RarityCommon
        Rarity.RARE -> RarityRare
        Rarity.EPIC -> RarityEpic
        Rarity.LEGENDARY -> RarityLegendary
        Rarity.MYTHIC -> RarityMythic
    }
    val elementColor = when (hero.element) {
        Element.FIRE -> ElementFire
        Element.EARTH -> ElementEarth
        Element.WATER -> ElementWater
        Element.ARCANE -> ElementArcane
        Element.THUNDER -> ElementThunder
        Element.ICE -> ElementIce
    }

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, spring(dampingRatio = 0.5f, stiffness = 300f))

    Column(
        Modifier.fillMaxWidth().scale(scale)
            .shadow(6.dp, RoundedCornerShape(12.dp))
            .background(Brush.verticalGradient(listOf(BgCard, BgPanel)), RoundedCornerShape(12.dp))
            .border(2.dp, rarityColor.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .padding(8.dp)
    ) {
        // Icon + Rarity badge
        Box(Modifier.fillMaxWidth().height(80.dp).background(rarityColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
            Text(hero.icon, fontSize = 40.sp)
            // Level badge
            Box(Modifier.align(Alignment.TopStart).background(rarityColor, RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 1.dp)) {
                Text("Lv${hero.level}", color = TextWhite, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
            // Element badge
            Box(Modifier.align(Alignment.TopEnd).background(elementColor, CircleShape).padding(4.dp), contentAlignment = Alignment.Center) {
                Text("●", color = TextWhite, fontSize = 8.sp)
            }
        }

        Spacer(Modifier.height(4.dp))

        // Name + Rarity
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(hero.name, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        }
        Text(hero.rarity.name, color = rarityColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)

        Spacer(Modifier.height(4.dp))

        // HP bar
        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("HP", color = TextGray, fontSize = 7.sp)
                Text("${hero.hp}/${hero.maxHp}", color = Emerald, fontSize = 7.sp, fontWeight = FontWeight.Bold)
            }
            Box(Modifier.fillMaxWidth().height(5.dp).background(Color(0xFF1C1C1E), RoundedCornerShape(3.dp))) {
                Box(Modifier.fillMaxWidth(hero.hp.toFloat() / hero.maxHp).height(5.dp).background(Brush.horizontalGradient(listOf(Emerald, EmeraldLight)), RoundedCornerShape(3.dp)))
            }
        }

        Spacer(Modifier.height(2.dp))

        // DMG bar
        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("DMG", color = TextGray, fontSize = 7.sp)
                Text("${hero.dmg}/${hero.maxDmg}", color = Ruby, fontSize = 7.sp, fontWeight = FontWeight.Bold)
            }
            Box(Modifier.fillMaxWidth().height(5.dp).background(Color(0xFF1C1C1E), RoundedCornerShape(3.dp))) {
                Box(Modifier.fillMaxWidth(hero.dmg.toFloat() / hero.maxDmg).height(5.dp).background(Brush.horizontalGradient(listOf(Ruby, RubyLight)), RoundedCornerShape(3.dp)))
            }
        }

        Spacer(Modifier.height(4.dp))

        // Stars
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(5) { i ->
                Box(Modifier.size(16.dp).background(if (i < hero.stars) rarityColor.copy(alpha = 0.3f) else Color.Transparent, RoundedCornerShape(3.dp)), contentAlignment = Alignment.Center) {
                    Text("★", color = if (i < hero.stars) GoldBright else TextDim, fontSize = 10.sp)
                }
            }
        }
    }
}

// =================== HERO DETAIL DIALOG ===================
@Composable
private fun HeroDetailDialog(hero: HeroData, onDismiss: () -> Unit) {
    val rarityColor = when (hero.rarity) {
        Rarity.COMMON -> RarityCommon; Rarity.RARE -> RarityRare
        Rarity.EPIC -> RarityEpic; Rarity.LEGENDARY -> RarityLegendary; Rarity.MYTHIC -> RarityMythic
    }
    val elementColor = when (hero.element) {
        Element.FIRE -> ElementFire; Element.EARTH -> ElementEarth; Element.WATER -> ElementWater
        Element.ARCANE -> ElementArcane; Element.THUNDER -> ElementThunder; Element.ICE -> ElementIce
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(BgCard, BgDeep)), RoundedCornerShape(16.dp))
                .border(2.dp, rarityColor, RoundedCornerShape(16.dp)).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero icon
            Box(Modifier.size(100.dp).background(rarityColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp)).border(2.dp, rarityColor, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Text(hero.icon, fontSize = 50.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(hero.name, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(hero.rarity.name, color = rarityColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Level ${hero.level}/${hero.maxLevel}", color = TextGray, fontSize = 11.sp)

            Spacer(Modifier.height(12.dp))

            // Stats
            HeroStatBar("Hitpoints", hero.hp, hero.maxHp, Emerald)
            Spacer(Modifier.height(6.dp))
            HeroStatBar("Damage", hero.dmg, hero.maxDmg, Ruby)
            Spacer(Modifier.height(6.dp))
            HeroStatBar("Experience", hero.xp, hero.xpMax, RoyalBlue)

            Spacer(Modifier.height(8.dp))

            // Element
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(16.dp).background(elementColor, CircleShape), contentAlignment = Alignment.Center) {
                    Text("●", color = TextWhite, fontSize = 8.sp)
                }
                Spacer(Modifier.width(6.dp))
                Text("Element: ${hero.element.name}", color = TextWhite, fontSize = 12.sp)
            }

            Spacer(Modifier.height(8.dp))

            // Stars
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(5) { i ->
                    Text("★", color = if (i < hero.stars) GoldBright else TextDim, fontSize = 18.sp)
                }
            }

            Spacer(Modifier.height(12.dp))

            GlossyButton("Close", 120.dp, 36.dp, listOf(BgPanelLight, BgPanel), onClick = onDismiss)
        }
    }
}

@Composable
private fun HeroStatBar(label: String, current: Int, max: Int, color: Color) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = TextGray, fontSize = 10.sp)
            Text("$current/$max", color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.fillMaxWidth().height(8.dp).background(Color(0xFF1C1C1E), RoundedCornerShape(4.dp))) {
            Box(Modifier.fillMaxWidth((current.toFloat() / max).coerceIn(0f, 1f)).height(8.dp).background(Brush.horizontalGradient(listOf(color, lerp(color, Color.White, 0.3f))), RoundedCornerShape(4.dp)))
        }
    }
}

// =================== SHOP SCREEN ===================
@Composable
private fun ShopScreen(
    player: Player, onBack: () -> Unit,
    onBuyGoblin: () -> Unit, onBuyDragon: () -> Unit,
    onPremium: () -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("TROOPS", "SPELLS", "RESOURCES", "PREMIUM")

    val troopItems = listOf(
        ShopItemData("👺", "Goblin Warrior", "Fast melee fighter", 50, "DMG", "40", Emerald),
        ShopItemData("🐉", "Fire Dragon", "Flying terror", 150, "DMG", "100", Ruby),
        ShopItemData("🗡️", "Iron Knight", "Heavy tank", 100, "HP", "250", RoyalBlue),
        ShopItemData("🏹", "Swift Archer", "Ranged DPS", 80, "DMG", "60", ElementWater),
    )
    val spellItems = listOf(
        ShopItemData("🛡️", "Shield Spell", "+30 DEF for 10s", 80, "DEF", "+30", RoyalBlue),
        ShopItemData("🔮", "Heal Spell", "Restores 50% HP", 100, "HEAL", "50%", Emerald),
        ShopItemData("⚡", "Lightning", "Chain damage", 120, "DMG", "80", ElementThunder),
    )
    val resourceItems = listOf(
        ShopItemData("💰", "Gold Mine", "Produces gold", 200, "Rate", "50/h", GoldBright),
        ShopItemData("🧪", "Elixir Collector", "Produces elixir", 250, "Rate", "40/h", MysticPurple),
        ShopItemData("💎", "Gem Mine", "Produces gems", 500, "Rate", "10/h", RoyalBlue),
    )

    Column(Modifier.fillMaxSize().background(BgDeep).padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            GlossyButton("◀ Back", 80.dp, 34.dp, listOf(BgPanelLight, BgPanel), onClick = onBack)
            Spacer(Modifier.weight(1f))
            Text("SHOP", color = TextGold, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.weight(1f))
            ResourcePill("💰", player.resources.toString(), GoldBright)
        }
        Spacer(Modifier.height(6.dp))

        // Tabs
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            tabs.forEachIndexed { i, t ->
                Box(
                    Modifier.padding(horizontal = 2.dp)
                        .background(if (tab == i) Brush.verticalGradient(listOf(GoldBright, GoldMedium)) else Brush.verticalGradient(listOf(BgCard, BgPanel)), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .border(1.dp, if (tab == i) GoldBright else Color.Transparent, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .clickable { tab = i }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(t, color = if (tab == i) BgDeep else TextGray, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
            }
        }

        Column(
            Modifier.weight(1f).background(BgPanel).border(1.dp, GoldGlow).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            when (tab) {
                0 -> troopItems.forEach { item -> ShopItemCard(item = item, onBuy = if (item.name == "Goblin Warrior") onBuyGoblin else onBuyDragon) }
                1 -> spellItems.forEach { item -> ShopItemCard(item = item, onBuy = onBuyGoblin) }
                2 -> resourceItems.forEach { item -> ShopItemCard(item = item, onBuy = onBuyGoblin) }
                3 -> PremiumShopCard(onPremium = onPremium)
            }
        }
    }
}

@Composable
private fun ShopItemCard(item: ShopItemData, onBuy: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(BgCard, RoundedCornerShape(10.dp))
            .border(1.dp, item.accent.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(48.dp).background(item.accent.copy(alpha = 0.15f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
            Text(item.icon, fontSize = 26.sp)
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(item.name, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(item.desc, color = TextGray, fontSize = 10.sp)
            Text("${item.statLabel}: ${item.statValue}", color = item.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("💰 ${item.cost}", color = GoldBright, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            GlossyButton("BUY", 60.dp, 28.dp, listOf(GoldBright, GoldMedium), onClick = onBuy)
        }
    }
}

@Composable
private fun PremiumShopCard(onPremium: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(BgCard, RoundedCornerShape(12.dp))
            .border(2.dp, RoyalBlue, RoundedCornerShape(12.dp)).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("💎 PREMIUM GEMS 💎", color = RoyalBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.height(4.dp))
        Text("Get premium gems to speed up upgrades and buy exclusive items!", color = TextGray, fontSize = 11.sp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniGemPack("100", "$0.99", RoyalBlue, onPremium)
            MiniGemPack("500", "$3.99", MysticPurple, onPremium)
            MiniGemPack("1200", "$7.99", GoldBright, onPremium)
        }
        Spacer(Modifier.height(8.dp))
        GlossyButton("VIEW ALL PACKS", 160.dp, 36.dp, listOf(RoyalBlue, RoyalBlueDark), onClick = onPremium)
    }
}

@Composable
private fun MiniGemPack(gems: String, price: String, color: Color, onClick: () -> Unit) {
    Column(
        Modifier.background(BgPanel, RoundedCornerShape(8.dp)).border(1.dp, color, RoundedCornerShape(8.dp))
            .clickable { onClick() }.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("💎", fontSize = 20.sp)
        Text(gems, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text(price, color = color, fontSize = 10.sp)
    }
}

// =================== PREMIUM DIALOG ===================
@Composable
private fun PremiumDialog(onDismiss: () -> Unit, onBuy: (Int, Int) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(BgCard, BgDeep)), RoundedCornerShape(16.dp))
                .border(2.dp, RoyalBlue, RoundedCornerShape(16.dp)).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("💎 GET MORE GEMS! 💎", color = RoyalBlue, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GemPackCard("100", "$0.99", 100, onBuy)
                GemPackCard("500", "$3.99", 500, onBuy)
                GemPackCard("1200", "$7.99", 1200, onBuy)
            }

            Spacer(Modifier.height(8.dp))

            // Best value
            Column(
                Modifier.fillMaxWidth().background(GoldBright.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .border(2.dp, GoldBright, RoundedCornerShape(12.dp)).padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("⭐ BEST VALUE! ⭐", color = GoldBright, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("5000 Gems", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("$19.99", color = GoldBright, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                GlossyButton("BUY NOW - $19.99", 180.dp, 40.dp, listOf(GoldBright, GoldMedium), onClick = { onBuy(1999, 5000) })
            }

            Spacer(Modifier.height(8.dp))
            GlossyButton("Close", 100.dp, 32.dp, listOf(BgPanelLight, BgPanel), onClick = onDismiss)
        }
    }
}

@Composable
private fun GemPackCard(gems: String, price: String, gemAmount: Int, onBuy: (Int, Int) -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, spring(dampingRatio = 0.5f, stiffness = 300f))

    Column(
        Modifier.scale(scale).background(BgPanel, RoundedCornerShape(10.dp))
            .border(1.5.dp, RoyalBlue.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            .clickable(interactionSource = interaction, indication = null) { onBuy(gemAmount * 10, gemAmount) }
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("💎", fontSize = 24.sp)
        Text(gems, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.height(2.dp))
        Text(price, color = RoyalBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

// =================== CLAN SCREEN ===================
@Composable
private fun ClanScreen(
    guild: Guild, guildGate: GuildGate, player: Player,
    onBack: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(BgDeep).padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            GlossyButton("◀ Back", 80.dp, 34.dp, listOf(BgPanelLight, BgPanel), onClick = onBack)
            Spacer(Modifier.weight(1f))
            Text("CLAN", color = TextGold, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))

        // Guild info card
        Column(
            Modifier.fillMaxWidth().background(BgCard, RoundedCornerShape(12.dp))
                .border(2.dp, RoyalBlue.copy(alpha = 0.5f), RoundedCornerShape(12.dp)).padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).background(RoyalBlue.copy(alpha = 0.2f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                    Text("⚔️", fontSize = 28.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(guild.guildName, color = TextGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("${guild.members.size} Members", color = TextGray, fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Members list
        Text("MEMBERS", color = TextGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))

        guild.members.forEach { member ->
            Row(
                Modifier.fillMaxWidth().background(BgCard, RoundedCornerShape(8.dp))
                    .border(1.dp, BgPanelLight, RoundedCornerShape(8.dp)).padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(36.dp).background(RoyalBlue.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                    Text(member.username.first().toString(), color = RoyalBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(member.username, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Resources: ${member.resources}", color = TextGray, fontSize = 10.sp)
                }
                Text("💰 ${member.resources}", color = GoldBright, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

// =================== UPGRADE DIALOG ===================
@Composable
private fun UpgradeDialog(
    towerLevel: Int, towerHp: Int,
    onDismiss: () -> Unit, onUpgrade: () -> Unit
) {
    val nextLevel = towerLevel + 1
    val nextHp = towerHp + 100
    val nextStorage = nextLevel * 100000
    val nextDmg = towerLevel * 10 + 10

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgCard,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text("UPGRADE TO LEVEL $nextLevel?", color = TextGold, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), fontSize = 16.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Building preview
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(64.dp).background(RoyalBlue.copy(alpha = 0.2f), RoundedCornerShape(12.dp)).border(2.dp, RoyalBlue, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Text("🏰", fontSize = 32.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Castle Tower", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Upgrade time: 6d", color = TextGray, fontSize = 11.sp)
                    }
                }

                // Stats
                UpgradeStatBar("Hitpoints", towerHp, nextHp, Emerald)
                UpgradeStatBar("Storage Capacity", towerLevel * 100000, nextStorage, GoldBright)
                UpgradeStatBar("Defense Power", towerLevel * 10, nextDmg, Ruby)

                // Unlocks
                Text("Unlocks:", color = TextGold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(4) { i ->
                        Box(Modifier.size(40.dp).background(BgPanel, RoundedCornerShape(8.dp)).border(1.dp, RoyalBlue.copy(alpha = 0.3f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                            Text(listOf("🏗️", "🏰", "🛡️", "⚔️")[i], fontSize = 18.sp)
                        }
                    }
                }

                Text("Note: Upgrading reduces loot from opponents.", color = TextDim, fontSize = 9.sp)
            }
        },
        confirmButton = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                GlossyButton("💰 200 Gold", 180.dp, 44.dp, listOf(GoldBright, GoldMedium), onClick = onUpgrade)
            }
        },
        dismissButton = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                TextButton(onClick = onDismiss) { Text("Cancel", color = TextGray) }
            }
        }
    )
}

@Composable
private fun UpgradeStatBar(label: String, current: Int, next: Int, color: Color) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = TextWhite, fontSize = 10.sp)
            Text("$current → $next", color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.fillMaxWidth().height(8.dp).background(Color(0xFF1C1C1E), RoundedCornerShape(4.dp))) {
            Box(Modifier.fillMaxWidth((current.toFloat() / next).coerceIn(0f, 1f)).height(8.dp).background(Brush.horizontalGradient(listOf(color, lerp(color, Color.White, 0.3f))), RoundedCornerShape(4.dp)))
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("${((current.toFloat() / next) * 100).toInt()}%", color = TextWhite, fontSize = 7.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// =================== BOTTOM NAV BAR ===================
@Composable
private fun BottomNavBar(
    currentScreen: String,
    onAttack: () -> Unit, onHeroes: () -> Unit,
    onShop: () -> Unit, onClan: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().height(48.dp)
            .background(Brush.verticalGradient(listOf(BgPanelLight, BgPanel)))
            .border(1.dp, GoldGlow)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavItem("⚔️", "Attack", currentScreen == "attack", onAttack)
        NavItem("🛡️", "Heroes", currentScreen == "heroes", onHeroes)
        NavItem("🏪", "Shop", currentScreen == "shop", onShop)
        NavItem("👥", "Clan", currentScreen == "clan", onClan)
        NavItem("⚙️", "Settings", currentScreen == "settings", {})
    }
}

@Composable
private fun NavItem(icon: String, label: String, selected: Boolean, onClick: () -> Unit) {
    val bgColor = if (selected) RoyalBlue.copy(alpha = 0.2f) else Color.Transparent
    val borderColor = if (selected) RoyalBlue else Color.Transparent

    Column(
        Modifier.background(bgColor, RoundedCornerShape(8.dp)).border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable { onClick() }.padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(icon, fontSize = 18.sp)
        Text(label, color = if (selected) RoyalBlue else TextGray, fontSize = 8.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

// =================== GLOSSY BUTTON ===================
@Composable
private fun GlossyButton(
    text: String, width: Dp, height: Dp,
    gradient: List<Color>, glowAlpha: Float = 0f,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, spring(dampingRatio = 0.45f, stiffness = 280f))

    Box(
        Modifier.width(width).height(height).scale(scale)
            .shadow(4.dp, RoundedCornerShape(10.dp))
            .background(Brush.verticalGradient(gradient), RoundedCornerShape(10.dp))
            .border(1.5.dp, GoldBright.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            .clickable(interactionSource = interaction, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (glowAlpha > 0f) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(GoldGlow.copy(alpha = glowAlpha * 0.3f), radius = size.width * 0.4f)
            }
        }
        Text(text, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}
