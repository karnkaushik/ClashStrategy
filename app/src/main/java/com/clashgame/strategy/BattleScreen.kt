package com.clashgame.strategy

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.graphicsLayer
import com.clashgame.strategy.model.GameCharacter
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

private val BgCard = Color(0xFF1E1E30)
private val BgPanel = Color(0xFF141428)
private val TextWhite = Color(0xFFE0E0E0)
private val TextGray = Color(0xFF9E9E9E)

// =================== CARD DATA ===================
private enum class AbilityType { NONE, FAST, RANGED, HEALER, SPLASH, STEALTH, CHARGE, REVIVE, FREEZE, RAGE, SIEGE, SUMMON }

private data class CardDef(
    val name: String, val spriteName: String, val cost: Int,
    val hp: Int, val damage: Int, val speed: Float,
    val isRanged: Boolean, val ability: AbilityType, val color: Color
)

private val ALL_CARDS = listOf(
    CardDef("Goblin","goblin",2,80,20,2.2f,false,AbilityType.FAST,Color(0xFF66BB6A)),
    CardDef("Skeleton","skeleton",1,50,12,1.8f,false,AbilityType.SUMMON,Color(0xFFBDBDBD)),
    CardDef("Archer","archer",3,100,35,1.3f,true,AbilityType.RANGED,Color(0xFF2E7D32)),
    CardDef("Barbarian","barbarian",4,220,45,1.0f,false,AbilityType.RAGE,Color(0xFFD4A017)),
    CardDef("Knight","knight",5,300,40,0.9f,false,AbilityType.NONE,Color(0xFF90CAF9)),
    CardDef("Healer","healer",4,80,0,1.1f,false,AbilityType.HEALER,Color(0xFFFFF9C4)),
    CardDef("Wizard","wizard",5,90,55,1.0f,true,AbilityType.SPLASH,Color(0xFF1565C0)),
    CardDef("Giant","giant",7,600,20,0.5f,false,AbilityType.SIEGE,Color(0xFF78909C)),
    CardDef("Assassin","assassin",4,90,75,2.0f,false,AbilityType.STEALTH,Color(0xFF4A148C)),
    CardDef("Sorceress","sorceress",5,85,45,1.0f,true,AbilityType.FREEZE,Color(0xFF03A9F4)),
    CardDef("Dragon","dragon",7,250,65,1.4f,true,AbilityType.SPLASH,Color(0xFFEF5350)),
    CardDef("Minotaur","minotaur",6,400,55,0.8f,false,AbilityType.CHARGE,Color(0xFF8D6E63)),
    CardDef("Phoenix","phoenix",6,150,50,1.3f,true,AbilityType.REVIVE,Color(0xFFFF8F00)),
    CardDef("Golem","golem",8,800,25,0.3f,false,AbilityType.SIEGE,Color(0xFF546E7A)),
    CardDef("Demon","demon",10,1000,90,0.7f,false,AbilityType.SPLASH,Color(0xFFC62828))
)

private class DeployedUnit(
    val card: CardDef, var x: Float, var y: Float,
    var hp: Float, val maxHp: Float, val damage: Int, val speed: Float,
    var attackCooldown: Float = 0f, var targetBuilding: Int = -1,
    var abilityTimer: Float = 0f, var stealthed: Boolean = false, var stealthTimer: Float = 2.5f,
    var charged: Boolean = false, var chargeTimer: Float = 1.5f,
    var revived: Boolean = false, var dead: Boolean = false, var deathAnim: Float = 0f,
    var spawnAnim: Float = 1f, var attackAnim: Float = 0f, var hitFlash: Float = 0f,
    var healCooldown: Float = 0f, var frozen: Float = 0f, var rageActive: Boolean = false
)

private class EnemyBuilding(
    val x: Float, val y: Float, val w: Float, val h: Float, val name: String,
    var hp: Float, val maxHp: Float, val color: Color, val roofColor: Color,
    val attackDamage: Int = 15, var attackCooldown: Float = 0f, var hitFlash: Float = 0f,
    var destroyed: Boolean = false, var deathAnim: Float = 0f, val isResource: Boolean = false
)

private class Projectile(var x: Float, var y: Float, val tx: Float, val ty: Float, var progress: Float = 0f, val damage: Int, val fromEnemy: Boolean, val color: Color = Color(0xFFFFAB40), val trail: MutableList<Offset> = mutableListOf())
private class SpawnEffect(var x: Float, var y: Float, var life: Float = 1f)
private class HitParticle(var x: Float, var y: Float, var vx: Float, var vy: Float, var life: Float, val color: Color, val size: Float)
private class FloatingText(var text: String, var x: Float, var y: Float, var life: Float, val color: Int)
@Composable
fun BattleScreen(army: List<GameCharacter>, towerName: String, towerHp: Float, onFinish: (Boolean) -> Unit) {
val context = LocalContext.current
    val spriteNames = listOf("goblin","dragon","barbarian","archer","knight","giant","wizard","healer","assassin","sorceress","skeleton","minotaur","phoenix","golem","demon")
    var spriteMap by remember { mutableStateOf<Map<String, ImageBitmap?>>(emptyMap()) }
    LaunchedEffect(Unit) {
        spriteMap = withContext(Dispatchers.Default) { spriteNames.associateWith { loadBattleBitmap(context, it) } }
    }
    var elapsed by remember { mutableFloatStateOf(0f) }
    var elixir by remember { mutableFloatStateOf(5f) }
    var selectedCard by remember { mutableIntStateOf(-1) }
    var finished by remember { mutableStateOf(false) }
    var victory by remember { mutableStateOf(false) }
    var resultAlpha by remember { mutableFloatStateOf(0f) }
    var groundYState by remember { mutableFloatStateOf(0f) }
    var battleW by remember { mutableFloatStateOf(0f) }
    var battleH by remember { mutableFloatStateOf(0f) }
    var screenShakeX by remember { mutableFloatStateOf(0f) }
    var screenShakeY by remember { mutableFloatStateOf(0f) }
    val deck = remember { mutableStateListOf<CardDef>().also { it.addAll(ALL_CARDS.shuffled()) } }
    val hand = remember { mutableStateListOf<Int>().also { repeat(4) { i -> if (i < deck.size) it.add(i) } } }
    val deployed = remember { mutableStateListOf<DeployedUnit>() }
    val buildings = remember { mutableStateListOf<EnemyBuilding>() }
    val projectiles = remember { mutableStateListOf<Projectile>() }
    val spawnEffects = remember { mutableStateListOf<SpawnEffect>() }
    val particles = remember { mutableStateListOf<HitParticle>() }
    val floatingTexts = remember { mutableStateListOf<FloatingText>() }
    val dmgPaint = remember { Paint().apply { textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true } }
    val titlePaint = remember { Paint().apply { textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true } }
    var buildingsInit by remember { mutableStateOf(false) }
    var nextCardIdx by remember { mutableIntStateOf(4) }

LaunchedEffect(Unit) {
        while (true) {
            delay(16)
            try {
            val dt = 0.016f
            elapsed += dt
            screenShakeX *= 0.85f; screenShakeY *= 0.85f
            elixir = (elixir + dt / 2.8f).coerceAtMost(10f)
            if (!buildingsInit && groundYState > 0f && battleW > 0f) {
                val w = battleW; val gy = groundYState
                buildings.addAll(listOf(
                    EnemyBuilding(w*0.78f, gy-65f, 100f, 130f, "HQ", 500f, 500f, Color(0xFF37474F), Color(0xFF263238), 20),
                    EnemyBuilding(w*0.60f, gy-35f, 55f, 70f, "Barracks", 250f, 250f, Color(0xFF5D4037), Color(0xFF4E342E), 12),
                    EnemyBuilding(w*0.86f, gy-32.5f, 45f, 65f, "Watchtower", 200f, 200f, Color(0xFF455A64), Color(0xFF37474F), 18),
                    EnemyBuilding(w*0.66f, gy-20f, 65f, 40f, "Walls", 180f, 180f, Color(0xFF6D4C41), Color(0xFF5D4037), 0),
                    EnemyBuilding(w*0.88f, gy-27.5f, 40f, 55f, "Cannon", 150f, 150f, Color(0xFF455A64), Color(0xFF37474F), 22),
                    EnemyBuilding(w*0.74f, gy-21f, 42f, 42f, "Gold Storage", 200f, 200f, Color(0xFFFF8F00), Color(0xFFEF6C00), 0, isResource=true),
                    EnemyBuilding(w*0.54f, gy-21f, 42f, 42f, "Elixir Storage", 200f, 200f, Color(0xFF9C27B0), Color(0xFF7B1FA2), 0, isResource=true)
                ))
                buildingsInit = true
            }
            spawnEffects.forEach { it.life -= dt * 2f }
            spawnEffects.removeAll { it.life <= 0f }
            val aliveBuildings = buildings.withIndex().filter { !it.value.destroyed }

            deployed.forEach { unit ->
                if (unit.dead) { unit.deathAnim -= dt * 1.5f; return@forEach }
                if (unit.spawnAnim > 0f) unit.spawnAnim -= dt * 3f
                if (unit.card.ability == AbilityType.STEALTH && unit.stealthed) { unit.stealthTimer -= dt; if (unit.stealthTimer <= 0f) unit.stealthed = false }
                if (unit.frozen > 0f) { unit.frozen -= dt; return@forEach }
                if (unit.attackCooldown > 0f) unit.attackCooldown -= dt
                if (unit.attackAnim > 0f) unit.attackAnim -= dt * 3f
                if (unit.hitFlash > 0f) unit.hitFlash -= dt * 3f
                if (unit.card.ability == AbilityType.HEALER) {
                    unit.healCooldown -= dt
                    if (unit.healCooldown <= 0f) {
                        unit.healCooldown = 2.5f
                        val nearby = deployed.filter { !it.dead && it != unit && abs(it.x - unit.x) < 120f }
                        val weakest = nearby.minByOrNull { it.hp / it.maxHp }
                        if (weakest != null && weakest.hp < weakest.maxHp) {
                            weakest.hp = (weakest.hp + 30f).coerceAtMost(weakest.maxHp)
                            floatingTexts.add(FloatingText("+30", weakest.x, weakest.y - 40f, 1f, android.graphics.Color.rgb(76,175,80)))
                            repeat(5) { particles.add(HitParticle(weakest.x + Random.nextFloat()*20f-10f, weakest.y-20f, Random.nextFloat()*10f-5f, -Random.nextFloat()*30f-10f, 0.8f, Color(0xFF4CAF50), 3f)) }
                        }
                    }
                    return@forEach
                }
                if (unit.card.ability == AbilityType.CHARGE && unit.chargeTimer > 0f) { unit.chargeTimer -= dt; if (unit.chargeTimer <= 0f) unit.charged = true }
                if (unit.targetBuilding < 0 || unit.targetBuilding >= buildings.size || buildings[unit.targetBuilding].destroyed) {
                    val targetIdx = if (unit.card.ability == AbilityType.FAST) {
                        aliveBuildings.indexOfFirst { it.value.isResource }.let { if (it >= 0) aliveBuildings[it].index else aliveBuildings.minByOrNull { dist(unit.x, unit.y, it.value.x, it.value.y) }?.index ?: -1 }
                    } else aliveBuildings.minByOrNull { dist(unit.x, unit.y, it.value.x, it.value.y) }?.index ?: -1
                    unit.targetBuilding = targetIdx
                }
                if (unit.targetBuilding >= 0 && unit.targetBuilding < buildings.size) {
                    val b = buildings[unit.targetBuilding]
                    if (b.destroyed) { unit.targetBuilding = -1; return@forEach }
                    val d = dist(unit.x, unit.y, b.x, b.y)
                    val attackRange = if (unit.card.isRanged) 160f else 55f
                    if (d > attackRange) {
                        val spd = unit.speed * 120f * if (unit.charged) 2.5f else 1f
                        val nd = d.coerceAtLeast(0.01f)
                        val dx = (b.x - unit.x) / nd; val dy = (b.y - unit.y) / nd
                        unit.x += dx * spd * dt; unit.y += dy * spd * dt
                        if (unit.charged && d < attackRange * 2f) {
                            unit.charged = false
                            val cDmg = unit.damage * 3
                            b.hp -= cDmg; b.hitFlash = 1f; unit.attackAnim = 1f
                            screenShakeX = Random.nextFloat()*10f-5f; screenShakeY = Random.nextFloat()*6f-3f
                            floatingTexts.add(FloatingText("-$cDmg", b.x, b.y-b.h/2-10f, 1f, android.graphics.Color.rgb(255,82,82)))
                            repeat(10) { particles.add(HitParticle(b.x+Random.nextFloat()*30f-15f, b.y, Random.nextFloat()*80f-40f, -Random.nextFloat()*60f, 0.6f, Color(0xFFFF6F00), 4f)) }
                            if (b.hp <= 0f) { b.destroyed = true; b.deathAnim = 1f }
                            unit.targetBuilding = -1
                        }
                    } else if (unit.attackCooldown <= 0f) {
                        unit.attackCooldown = if (unit.card.isRanged) 1.2f else 1.0f
                        unit.attackAnim = 1f
                        var dmg = unit.damage
                        if (unit.card.ability == AbilityType.RAGE && unit.hp < unit.maxHp * 0.5f) dmg *= 2
                        if (unit.card.isRanged) {
                            projectiles.add(Projectile(unit.x, unit.y-20f, b.x, b.y, 0f, dmg, false, unit.card.color))
                        } else {
                            b.hp -= dmg; b.hitFlash = 1f
                            screenShakeX = Random.nextFloat()*6f-3f
                            floatingTexts.add(FloatingText("-$dmg", b.x, b.y-b.h/2-10f, 1f, android.graphics.Color.rgb(255,82,82)))
                            if (unit.card.ability == AbilityType.SPLASH) {
                                aliveBuildings.forEach { (idx, ob) ->
                                    if (idx != unit.targetBuilding && !ob.destroyed && dist(b.x,b.y,ob.x,ob.y) < 100f) { ob.hp -= dmg/2; ob.hitFlash = 1f }
                                }
                                repeat(8) { particles.add(HitParticle(b.x+Random.nextFloat()*30f-15f, b.y, Random.nextFloat()*100f-50f, -Random.nextFloat()*80f, 0.5f, unit.card.color, 5f)) }
                            }
                            if (unit.card.ability == AbilityType.FREEZE) { aliveBuildings.forEach { (_, ob) -> if (!ob.destroyed && dist(b.x,b.y,ob.x,ob.y) < 100f) ob.hitFlash = 0.5f } }
                            repeat(5) { particles.add(HitParticle(b.x+Random.nextFloat()*20f-10f, b.y, Random.nextFloat()*60f-30f, -Random.nextFloat()*50f, 0.5f, unit.card.color, 3f)) }
                        }
                        if (b.hp <= 0f) { b.destroyed = true; b.deathAnim = 1f; unit.targetBuilding = -1 }
                    }
                }
            }

            val toSpawn = mutableListOf<DeployedUnit>()
            deployed.filter { !it.dead && it.card.ability == AbilityType.SUMMON && it.abilityTimer == 0f && elapsed > 2f }.forEach { u ->
                if (Random.nextFloat() < 0.005f) {
                    u.abilityTimer = 5f
                    val sc = CardDef("Skeleton","skeleton",0,40,10,1.5f,false,AbilityType.NONE,Color(0xFFBDBDBD))
                    toSpawn.add(DeployedUnit(sc, u.x+Random.nextFloat()*40f-20f, u.y+Random.nextFloat()*20f-10f, 40f, 40f, 10, 1.5f))
                }
            }
            deployed.forEach { if (it.abilityTimer > 0f) it.abilityTimer -= dt }
            deployed.addAll(toSpawn)

            val projIter = projectiles.iterator()
            while (projIter.hasNext()) {
                val p = projIter.next()
                p.progress += dt / 0.4f
                p.trail.add(Offset(p.x+(p.tx-p.x)*p.progress, p.y+(p.ty-p.y)*p.progress-(1f-p.progress)*30f))
                if (p.trail.size > 6) p.trail.removeAt(0)
                if (p.progress >= 1f) {
                    if (p.fromEnemy) {
                        val target = deployed.filter { !it.dead && !it.stealthed }.minByOrNull { dist(p.tx,p.ty,it.x,it.y) }
                        if (target != null && dist(p.tx,p.ty,target.x,target.y) < 60f) {
                            target.hp -= p.damage; target.hitFlash = 1f
                            floatingTexts.add(FloatingText("-${p.damage}", target.x, target.y-40f, 1f, android.graphics.Color.rgb(255,82,82)))
                            if (target.hp <= 0f) {
                                if (target.card.ability == AbilityType.REVIVE && !target.revived) {
                                    target.revived = true; target.hp = target.maxHp * 0.5f
                                    floatingTexts.add(FloatingText("REVIVE!", target.x, target.y-50f, 1.5f, android.graphics.Color.rgb(255,152,0)))
                                    repeat(10) { particles.add(HitParticle(target.x, target.y-20f, Random.nextFloat()*80f-40f, -Random.nextFloat()*80f, 1f, Color(0xFFFF8F00), 4f)) }
                                } else { target.dead = true; target.deathAnim = 1f; repeat(8) { particles.add(HitParticle(target.x, target.y, Random.nextFloat()*60f-30f, -Random.nextFloat()*60f, 0.8f, target.card.color, 3f)) } }
                            }
                        }
                    } else {
                        val bi = buildings.indexOfFirst { !it.destroyed && abs(it.x-p.tx) < it.w && abs(it.y-p.ty) < it.h }
                        if (bi >= 0) { val b = buildings[bi]; b.hp -= p.damage; b.hitFlash = 1f; screenShakeX = Random.nextFloat()*4f-2f; floatingTexts.add(FloatingText("-${p.damage}", b.x, b.y-b.h/2, 1f, android.graphics.Color.rgb(255,82,82))); if (b.hp <= 0f) { b.destroyed = true; b.deathAnim = 1f } }
                    }
                    projIter.remove()
                }
            }

            buildings.forEach { b ->
                if (b.destroyed) { b.deathAnim = (b.deathAnim - dt * 1.5f).coerceAtLeast(0f); return@forEach }
                if (b.hitFlash > 0f) b.hitFlash -= dt * 3f
                if (b.attackDamage <= 0) return@forEach
                b.attackCooldown -= dt
                if (b.attackCooldown <= 0f) {
                    val target = deployed.filter { !it.dead && !it.stealthed }.minByOrNull { dist(b.x,b.y,it.x,it.y) }
                    if (target != null && dist(b.x,b.y,target.x,target.y) < 250f) {
                        b.attackCooldown = 1.8f
                        projectiles.add(Projectile(b.x, b.y-b.h*0.3f, target.x, target.y, 0f, b.attackDamage, true, Color(0xFFFF5252)))
                    }
                }
            }

            particles.forEach { it.life -= dt*2f; it.x += it.vx*dt; it.y += it.vy*dt; it.vy += 80f*dt }
            particles.removeAll { it.life <= 0f }
            floatingTexts.forEach { it.life -= dt*1.2f; it.y -= 30f*dt }
            floatingTexts.removeAll { it.life <= 0f }

            if (!finished && buildingsInit) {
                if (buildings.all { it.destroyed }) { victory = true; finished = true }
                else if (deployed.isNotEmpty() && deployed.all { it.dead } && elapsed > 3f) { victory = false; finished = true }
                else if (deployed.isEmpty() && elapsed > 5f && elixir < 2f) { victory = false; finished = true }
            }
            if (finished) resultAlpha = (resultAlpha + dt * 1.5f).coerceAtMost(1f)
            if (finished && resultAlpha >= 0.95f) { delay(2000); onFinish(victory); return@LaunchedEffect }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("BattleScreen", "frame error", e)
            }
        }
    }
    Box(Modifier.fillMaxSize().graphicsLayer {
        val ent = (elapsed * 3f).coerceAtMost(1f)
        alpha = ent; scaleX = 0.85f + 0.15f * ent; scaleY = 0.85f + 0.15f * ent
    }) {
        Canvas(Modifier.fillMaxSize().pointerInput(selectedCard, buildingsInit) {
detectTapGestures { offset ->
                if (selectedCard >= 0 && hand[selectedCard] < deck.size && groundYState > 0f) {
                    val cardIdx = hand[selectedCard]
                    val card = deck[cardIdx]
                    if (elixir >= card.cost) {
                        elixir -= card.cost
                        val minY = groundYState * 0.4f
                        val maxY = (groundYState - 5f).coerceAtLeast(minY)
                        val y = offset.y.coerceIn(minY, maxY)
                        val unit = DeployedUnit(card = card, x = offset.x, y = y, hp = card.hp.toFloat(), maxHp = card.hp.toFloat(), damage = card.damage, speed = card.speed, stealthed = card.ability == AbilityType.STEALTH)
                        deployed.add(unit)
                        spawnEffects.add(SpawnEffect(offset.x, y))
                        if (nextCardIdx < deck.size) { hand[selectedCard] = nextCardIdx; nextCardIdx = (nextCardIdx + 1) % deck.size }
                        selectedCard = -1
                    }
                }
            }
        }) {
            val w = size.width; val h = size.height; val groundY = h * 0.72f
            groundYState = groundY; battleW = w; battleH = h
            withTransform({ translate(screenShakeX, screenShakeY) }) {
                drawBattleBg(w, h, groundY, elapsed)
                buildings.forEach { b ->
                    if (!b.destroyed) drawBattleBuilding(b, elapsed)
                    else if (b.deathAnim > 0f) { drawRect(Color.White.copy(alpha = b.deathAnim.coerceIn(0f,1f)*0.5f), topLeft=Offset(b.x-b.w/2,b.y-b.h/2), size=Size(b.w,b.h)); drawCircle(Color(0xFFFF6F00).copy(alpha=b.deathAnim.coerceIn(0f,1f)*0.3f), radius=(1f-b.deathAnim)*60f, center=Offset(b.x,b.y)) }
                }
                if (selectedCard >= 0) drawRect(Color(0xFF4CAF50).copy(alpha=0.05f+0.03f*sin(elapsed*4f).toFloat()), topLeft=Offset(0f,groundY*0.4f), size=Size(w,groundY*0.55f))
                projectiles.forEach { p ->
                    val pos = Offset(p.x+(p.tx-p.x)*p.progress, p.y+(p.ty-p.y)*p.progress-(1f-p.progress)*30f)
                    if (p.trail.size >= 2) { for (i in 1 until p.trail.size) { val a=i.toFloat()/p.trail.size; drawLine(p.color.copy(alpha=a*0.5f),p.trail[i-1],p.trail[i],strokeWidth=2f*a,cap=StrokeCap.Round) } }
                    drawCircle(p.color,radius=4f,center=pos); drawCircle(p.color.copy(alpha=0.3f),radius=8f,center=pos)
                }
                deployed.forEach { u ->
                    if (u.dead) { if (u.deathAnim > 0f) { drawCircle(Color.White.copy(alpha=u.deathAnim.coerceIn(0f,1f)*0.7f),radius=(1f-u.deathAnim)*40f+5f,center=Offset(u.x,u.y)) }; return@forEach }
                    val spawnScale = if (u.spawnAnim > 0f) 1f+u.spawnAnim*0.5f else 1f
                    val idle = sin(elapsed*2f+u.x*0.01f).toFloat()*2f; val drawY = u.y+idle
                    drawOval(Color.Black.copy(alpha=0.25f),topLeft=Offset(u.x-16f,drawY+2f),size=Size(32f,8f))
                    val alpha = if (u.stealthed) 0.3f else 1f
                    if (u.spawnAnim > 0f) drawCircle(u.card.color.copy(alpha=u.spawnAnim*0.4f),radius=30f*(1f-u.spawnAnim*0.3f),center=Offset(u.x,drawY))
                    if (u.attackAnim > 0.3f) drawCircle(u.card.color.copy(alpha=u.attackAnim*0.3f),radius=25f,center=Offset(u.x,drawY-15f))
                    val bmp = spriteMap[u.card.spriteName]
                    if (bmp != null) { val sz=(36f*spawnScale).toInt(); drawImage(bmp,dstOffset=IntOffset((u.x-sz/2).toInt(),(drawY-sz-5f).toInt()),dstSize=IntSize(sz,sz)) }
                    else drawCircle(u.card.color.copy(alpha=alpha),radius=14f*spawnScale,center=Offset(u.x,drawY-18f))
                    if (u.hitFlash > 0f) drawCircle(Color.White.copy(alpha=u.hitFlash*0.7f),radius=18f,center=Offset(u.x,drawY-15f))
                    val hpRatio = u.hp/u.maxHp; val barW=30f; val barH=4f; val barTop=drawY-40f
                    drawRect(Color(0x66000000),topLeft=Offset(u.x-barW/2,barTop),size=Size(barW,barH))
                    val hpC = if (hpRatio>0.5f) Color(0xFF4CAF50) else if (hpRatio>0.25f) Color(0xFFFFC107) else Color(0xFFEF5350)
                    drawRect(hpC,topLeft=Offset(u.x-barW/2,barTop),size=Size(barW*hpRatio.coerceIn(0f,1f),barH))
                    if (u.card.ability == AbilityType.HEALER) drawCircle(Color(0xFF4CAF50).copy(alpha=0.2f+0.1f*sin(elapsed*3f).toFloat()),radius=35f,center=Offset(u.x,drawY-15f))
                }
                spawnEffects.forEach { s -> drawCircle(Color(0xFF4FC3F7).copy(alpha=s.life.coerceIn(0f,1f)*0.3f),radius=30f*(1f-s.life),center=Offset(s.x,s.y)); drawCircle(Color(0xFF81D4FA).copy(alpha=s.life.coerceIn(0f,1f)*0.5f),radius=15f*(1f-s.life*0.5f),center=Offset(s.x,s.y)) }
                particles.forEach { p -> drawCircle(p.color.copy(alpha=p.life.coerceIn(0f,1f)),radius=p.size*p.life.coerceIn(0f,1f),center=Offset(p.x,p.y)) }
                floatingTexts.forEach { ft -> dmgPaint.textSize=26f; dmgPaint.color=android.graphics.Color.argb((ft.life.coerceIn(0f,1f)*255).toInt(),android.graphics.Color.red(ft.color),android.graphics.Color.green(ft.color),android.graphics.Color.blue(ft.color)); drawContext.canvas.nativeCanvas.drawText(ft.text,ft.x,ft.y,dmgPaint) }
                if (finished) drawResultOverlay(w,h,victory,resultAlpha,titlePaint)
            }
        }
        Row(Modifier.align(Alignment.TopStart).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.background(Brush.verticalGradient(listOf(Color(0xFF37474F), Color(0xFF263238))), RoundedCornerShape(8.dp)).border(1.dp, Color(0xFFFFD54F).copy(alpha = 0.4f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                Text("⚔️ RAID", color = Color(0xFFFFD54F), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Spacer(Modifier.width(6.dp))
            Box(Modifier.background(Brush.verticalGradient(listOf(Color(0xFF263238), Color(0xFF1A1F24))), RoundedCornerShape(8.dp)).border(1.dp, Color(0xFF90A4AE).copy(alpha = 0.3f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                Text("🏰 ${buildings.count { !it.destroyed }} LEFT", color = Color(0xFFB0BEC5), fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
        Column(
            Modifier.align(Alignment.BottomCenter).padding(horizontal = 8.dp, vertical = 8.dp).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0x33000000), Color(0xCC000000))), RoundedCornerShape(14.dp)).padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                hand.forEachIndexed { idx, deckIdx -> if (deckIdx < deck.size) { val card=deck[deckIdx]; val isSel=idx==selectedCard; val canAff=elixir>=card.cost; BattleCard(card=card,selected=isSel,canAfford=canAff,spriteMap=spriteMap,onClick={ if(canAff) selectedCard=if(selectedCard==idx)-1 else idx }) } }
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⚡", color = Color(0xFFE040FB), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(Modifier.width(6.dp))
                Box(Modifier.width(220.dp).height(16.dp).shadow(2.dp, RoundedCornerShape(8.dp)).background(Color(0xFF1A1A2E), RoundedCornerShape(8.dp)).border(1.dp, Color(0xFF9C27B0).copy(alpha = 0.6f), RoundedCornerShape(8.dp))) {
                    Box(Modifier.fillMaxWidth((elixir / 10f).coerceIn(0f, 1f)).height(16.dp).background(Brush.horizontalGradient(listOf(Color(0xFF9C27B0), Color(0xFFE040FB))), RoundedCornerShape(8.dp)))
                }
                Spacer(Modifier.width(6.dp))
                Text("${elixir.toInt()}/10", color = Color(0xFFE040FB), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
        if (elapsed < 2f && deployed.isEmpty()) { val a=(1f-elapsed/2f).coerceIn(0f,1f); Text("SELECT A CARD, THEN TAP TO DEPLOY",color=Color(0xFFFFD54F).copy(alpha=a),fontWeight=FontWeight.Bold,fontSize=14.sp,modifier=Modifier.align(Alignment.Center).padding(top=80.dp)) }
    }
}

@Composable
private fun BattleCard(card: CardDef, selected: Boolean, canAfford: Boolean, spriteMap: Map<String, ImageBitmap?>, onClick: () -> Unit) {
    val bmp = spriteMap[card.spriteName]
    val infinite = rememberInfiniteTransition()
    val floatY by infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Reverse))
    val borderC = if (selected) Color(0xFFFFD54F) else if (canAfford) card.color.copy(alpha=0.5f) else Color(0xFF424242)
    Column(Modifier.graphicsLayer { rotationX=(floatY-0.5f)*10f; rotationY=(floatY-0.5f)*14f; cameraDistance=8f*density }.scale(if(selected)1.12f else 1f).shadow(if(selected)10.dp else 3.dp,RoundedCornerShape(10.dp)).size(width=68.dp,height=90.dp).background(Brush.verticalGradient(listOf(if(canAfford)BgCard else Color(0xFF1A1A1A),BgPanel)),RoundedCornerShape(10.dp)).border(2.dp,borderC,RoundedCornerShape(10.dp)).clickable(enabled=canAfford){onClick()}.padding(4.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        Box(Modifier.size(40.dp).background(card.color.copy(alpha=if(canAfford)0.15f else 0.05f),RoundedCornerShape(6.dp)),contentAlignment=Alignment.Center) {
            if (bmp != null) Image(bitmap=bmp,contentDescription=card.name,modifier=Modifier.size(32.dp),contentScale=ContentScale.Fit) else Box(Modifier.size(24.dp).background(card.color.copy(alpha=0.6f),CircleShape))
        }
        Spacer(Modifier.height(2.dp))
        Text(card.name,color=if(canAfford)TextWhite else TextGray,fontWeight=FontWeight.Bold,fontSize=7.sp,maxLines=1)
        Box(Modifier.size(18.dp).background(Color(0xFF9C27B0),CircleShape).border(1.dp,Color(0xFFE040FB),CircleShape),contentAlignment=Alignment.Center) { Text("${card.cost}",color=Color.White,fontWeight=FontWeight.Bold,fontSize=9.sp) }
    }
}
private fun dist(x1: Float, y1: Float, x2: Float, y2: Float): Float { val dx=x2-x1; val dy=y2-y1; return sqrt(dx*dx+dy*dy) }

private fun DrawScope.drawBattleBg(w: Float, h: Float, groundY: Float, elapsed: Float) {
    drawRect(Brush.verticalGradient(listOf(Color(0xFF0A0E27),Color(0xFF141B3D),Color(0xFF1A2347))),size=Size(w,h+20f))
    val moonX=w*0.85f; val moonY=h*0.1f; val moonR=w*0.05f
    drawCircle(Color(0x30FFFFFF),radius=moonR*2.5f,center=Offset(moonX,moonY))
    drawCircle(Color(0x50FFFFFF),radius=moonR*1.8f,center=Offset(moonX,moonY))
    drawCircle(Color(0xFFE8EAF6),radius=moonR,center=Offset(moonX,moonY))
    drawCircle(Color(0xFF141B3D),radius=moonR*0.7f,center=Offset(moonX+moonR*0.25f,moonY-moonR*0.15f))
    repeat(30) { i -> val sx=w*((i*37%100)/100f); val sy=h*(0.02f+0.18f*(i*13%7)/6f); val tw=(0.2f+0.3f*sin(elapsed*2f+i*0.7f).toFloat()).coerceIn(0f,1f); drawCircle(Color.White.copy(alpha=tw),radius=1f+(i%3)*0.5f,center=Offset(sx,sy)) }
    repeat(4) { i -> val cx=((elapsed*(6f+i*3f)+i*300f)%(w+300f))-150f; val cy=h*(0.08f+i*0.06f); val cs=100f+i*30f; drawOval(Color(0xFF2A3055).copy(alpha=0.08f+i*0.02f),topLeft=Offset(cx,cy),size=Size(cs,cs*0.35f)) }
    val mtn1=Path().apply { moveTo(0f,groundY+10f); lineTo(w*0.08f,groundY-h*0.15f); lineTo(w*0.2f,groundY-h*0.22f); lineTo(w*0.35f,groundY-h*0.12f); lineTo(w*0.5f,groundY-h*0.18f); lineTo(w*0.65f,groundY-h*0.08f); lineTo(w*0.8f,groundY-h*0.14f); lineTo(w,groundY+10f); close() }
    drawPath(mtn1,Color(0xFF1A2744))
    val mtn2=Path().apply { moveTo(0f,groundY+10f); lineTo(w*0.1f,groundY-h*0.08f); lineTo(w*0.25f,groundY-h*0.14f); lineTo(w*0.4f,groundY-h*0.06f); lineTo(w*0.55f,groundY-h*0.1f); lineTo(w*0.7f,groundY-h*0.04f); lineTo(w*0.85f,groundY-h*0.08f); lineTo(w,groundY+10f); close() }
    drawPath(mtn2,Color(0xFF15203A))
    drawRect(Brush.verticalGradient(listOf(Color(0xFF1B3D1B),Color(0xFF153015),Color(0xFF0D200D))),topLeft=Offset(0f,groundY),size=Size(w,h-groundY))
    drawLine(Color(0xFF2E5E2E),Offset(0f,groundY),Offset(w,groundY),strokeWidth=3f)
}

private fun DrawScope.drawBattleBuilding(b: EnemyBuilding, elapsed: Float) {
    val cx=b.x; val cy=b.y; val top=cy-b.h/2
    val flashC = if(b.hitFlash>0f) Color.White.copy(alpha=b.hitFlash*0.6f) else null
    drawOval(Color(0x40000000),topLeft=Offset(cx-b.w*0.4f,cy+b.h*0.1f),size=Size(b.w*0.8f,b.h*0.15f))
    when(b.name) {
        "HQ" -> { drawRoundRect(Brush.verticalGradient(listOf(b.color,b.roofColor)),topLeft=Offset(cx-b.w/2,top),size=Size(b.w,b.h),cornerRadius=CornerRadius(4f)); val roof=Path().apply { moveTo(cx-b.w*0.6f,top); lineTo(cx,top-b.h*0.25f); lineTo(cx+b.w*0.6f,top); close() }; drawPath(roof,b.roofColor); drawPath(roof,Color.White.copy(alpha=0.1f),style=Stroke(2f)); val fw=sin(elapsed*4f).toFloat()*4f; drawLine(Color(0xFF5D4037),Offset(cx,top-b.h*0.25f),Offset(cx,top-b.h*0.55f),strokeWidth=3f); val flag=Path().apply { moveTo(cx,top-b.h*0.55f); lineTo(cx+18f,top-b.h*0.5f+fw); lineTo(cx,top-b.h*0.42f); close() }; drawPath(flag,Color(0xFFE53935)); drawCircle(Color(0xFFFFD54F).copy(alpha=0.3f+0.2f*sin(elapsed*3f).toFloat()),radius=8f,center=Offset(cx,top+b.h*0.6f)) }
        "Barracks" -> { drawRoundRect(Brush.verticalGradient(listOf(b.color,b.roofColor)),topLeft=Offset(cx-b.w/2,top),size=Size(b.w,b.h),cornerRadius=CornerRadius(3f)); val roof=Path().apply { moveTo(cx-b.w*0.55f,top); lineTo(cx,top-b.h*0.2f); lineTo(cx+b.w*0.55f,top); close() }; drawPath(roof,b.roofColor) }
        "Watchtower" -> { drawRoundRect(Brush.verticalGradient(listOf(b.color,Color(0xFF2C3E50))),topLeft=Offset(cx-b.w*0.3f,top),size=Size(b.w*0.6f,b.h),cornerRadius=CornerRadius(2f)); drawRoundRect(b.roofColor,topLeft=Offset(cx-b.w/2,top),size=Size(b.w,12f),cornerRadius=CornerRadius(3f)); val tf=0.6f+0.3f*sin(elapsed*8f).toFloat(); drawCircle(Color(0xFFFF8F00).copy(alpha=tf),radius=4f,center=Offset(cx-b.w*0.35f,top+2f)); drawCircle(Color(0xFFFFD54F).copy(alpha=tf*0.5f),radius=8f,center=Offset(cx-b.w*0.35f,top+2f)); drawCircle(Color(0xFFFF8F00).copy(alpha=tf),radius=4f,center=Offset(cx+b.w*0.35f,top+2f)); drawCircle(Color(0xFFFFD54F).copy(alpha=tf*0.5f),radius=8f,center=Offset(cx+b.w*0.35f,top+2f)) }
        "Walls" -> { drawRoundRect(Brush.verticalGradient(listOf(b.color,b.roofColor)),topLeft=Offset(cx-b.w/2,top),size=Size(b.w,b.h),cornerRadius=CornerRadius(2f)); val mw=b.w/6f; repeat(6) { i -> drawRect(b.color,topLeft=Offset(cx-b.w/2+i*mw,top-8f),size=Size(mw*0.6f,8f)) } }
        "Cannon" -> { drawOval(Brush.verticalGradient(listOf(b.color,b.roofColor)),topLeft=Offset(cx-b.w/2,top+b.h*0.3f),size=Size(b.w,b.h*0.5f)); val ba=-25f+sin(elapsed*0.5f).toFloat()*5f; withTransform({ translate(cx,top+b.h*0.4f); rotate(ba) }) { drawRoundRect(Color(0xFF37474F),topLeft=Offset(0f,-4f),size=Size(b.w*0.5f,8f),cornerRadius=CornerRadius(3f)) }; drawCircle(Color(0xFF5D4037),radius=6f,center=Offset(cx,top+b.h*0.5f)) }
        else -> { drawRoundRect(Brush.verticalGradient(listOf(b.color,b.roofColor)),topLeft=Offset(cx-b.w/2,top),size=Size(b.w,b.h),cornerRadius=CornerRadius(3f)); if(b.isResource) { val glowPulse=0.3f+0.2f*sin(elapsed*2f).toFloat(); drawCircle(b.color.copy(alpha=glowPulse),radius=10f,center=Offset(cx,top+b.h*0.5f)) } }
    }
    if(flashC!=null) drawRect(flashC,topLeft=Offset(cx-b.w/2,top),size=Size(b.w,b.h))
    val hpRatio=b.hp/b.maxHp; val barW=b.w*1.2f; val barH=8f; val barTop=top-12f
    drawRoundRect(Color(0xFF111111),topLeft=Offset(cx-barW/2-2f,barTop-2f),size=Size(barW+4f,barH+4f),cornerRadius=CornerRadius(4f))
    val bc=if(hpRatio>0.5f) Color(0xFF4CAF50) else if(hpRatio>0.25f) Color(0xFFFFC107) else Color(0xFFEF5350)
    drawRoundRect(bc,topLeft=Offset(cx-barW/2,barTop),size=Size(barW*hpRatio.coerceIn(0f,1f),barH),cornerRadius=CornerRadius(4f))
    val tp = titleTextPaint().apply { textSize=16f; color=android.graphics.Color.rgb(200,200,200) }
    drawContext.canvas.nativeCanvas.drawText(b.name,cx,cy+b.h/2+16f,tp)
}

private fun DrawScope.drawResultOverlay(w: Float, h: Float, victory: Boolean, alpha: Float, paint: Paint) {
    drawRect(if(victory) Color.Black.copy(alpha=alpha*0.7f) else Color(0xFF2A0505).copy(alpha=alpha*0.7f),size=Size(w,h))
    val cx=w/2f; val cy=h*0.34f; val s=w*0.13f
    val shield=Path().apply { moveTo(cx,cy-s); cubicTo(cx+s*0.95f,cy-s*0.8f,cx+s*1.08f,cy-s*0.1f,cx+s*0.75f,cy+s*0.45f); lineTo(cx,cy+s*1.05f); lineTo(cx-s*0.75f,cy+s*0.45f); cubicTo(cx-s*1.08f,cy-s*0.1f,cx-s*0.95f,cy-s*0.8f,cx,cy-s); close() }
    if(victory) {
        val crown=Path().apply { moveTo(cx-s*0.45f,cy-s*1.15f); lineTo(cx-s*0.28f,cy-s*1.5f); lineTo(cx-s*0.12f,cy-s*1.2f); lineTo(cx,cy-s*1.55f); lineTo(cx+s*0.12f,cy-s*1.2f); lineTo(cx+s*0.28f,cy-s*1.5f); lineTo(cx+s*0.45f,cy-s*1.15f); close() }
        drawPath(shield,Brush.linearGradient(listOf(Color(0xFFF5A623).copy(alpha=alpha),Color(0xFFC4841D).copy(alpha=alpha))))
        drawPath(crown,Color(0xFFF5A623).copy(alpha=alpha))
    } else {
        drawPath(shield,Brush.linearGradient(listOf(Color(0xFF9E9E9E).copy(alpha=alpha),Color(0xFF616161).copy(alpha=alpha))))
        val crack=Path().apply { moveTo(cx-s*0.15f,cy-s); lineTo(cx+s*0.1f,cy-s*0.45f); lineTo(cx-s*0.05f,cy-s*0.15f); lineTo(cx+s*0.18f,cy+s*0.35f); lineTo(cx-s*0.05f,cy+s*0.75f) }
        drawPath(crack,Color(0xFF212121).copy(alpha=alpha),style=Stroke(width=s*0.09f,cap=StrokeCap.Round))
    }
    val bannerY=cy+s*1.7f; val bannerW=w*0.72f
    drawRect(Brush.horizontalGradient(listOf(Color(0xFF0D47A1).copy(alpha=alpha),Color(0xFF1565C0).copy(alpha=alpha),Color(0xFF0D47A1).copy(alpha=alpha))),topLeft=Offset(cx-bannerW/2,bannerY),size=Size(bannerW,h*0.07f))
    paint.textSize=h*0.055f; paint.color=android.graphics.Color.argb((alpha*255).toInt(),255,255,255)
    drawContext.canvas.nativeCanvas.drawText(if (victory) "VICTORY!" else "DEFEAT", cx, bannerY + h * 0.048f, paint)
    paint.textSize = h * 0.045f; paint.color = android.graphics.Color.argb((alpha * 255).toInt(), if (victory) 255 else 255, if (victory) 213 else 120, if (victory) 79 else 120)
    drawContext.canvas.nativeCanvas.drawText(if (victory) "+30 TROPHIES +150 GOLD" else "-30 TROPHIES +60 GOLD", cx, bannerY + h * 0.10f, paint)
    paint.textSize=h*0.03f; paint.color=android.graphics.Color.argb((alpha*200).toInt(),180,190,200)
    drawContext.canvas.nativeCanvas.drawText("TAP TO CONTINUE",cx,h*0.90f,paint)
}

private fun titleTextPaint(): Paint { return Paint().apply { textAlign=Paint.Align.CENTER; typeface=Typeface.DEFAULT_BOLD; isAntiAlias=true } }

private fun loadBattleBitmap(context: Context, name: String): ImageBitmap? {
    val id=context.resources.getIdentifier(name,"drawable",context.packageName)
    if(id==0) return null
    return runCatching {
        if(android.os.Build.VERSION.SDK_INT>=28) {
            val src=android.graphics.ImageDecoder.createSource(context.resources,id)
            android.graphics.ImageDecoder.decodeBitmap(src) { decoder, _, _ -> decoder.setTargetSampleSize(4) }.asImageBitmap()
        } else {
            val opts=android.graphics.BitmapFactory.Options().apply { inSampleSize=4 }
            android.graphics.BitmapFactory.decodeResource(context.resources,id,opts)?.asImageBitmap()
        }
    }.getOrNull()
}
