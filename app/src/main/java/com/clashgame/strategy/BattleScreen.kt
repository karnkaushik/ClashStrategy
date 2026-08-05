package com.clashgame.strategy

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.clashgame.strategy.model.GameCharacter
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlin.random.Random

private class BattleUnit(
    val type: String,
    val spriteName: String,
    val maxHp: Float,
    var hp: Float,
    val damage: Int,
    val speed: Float,
    val fly: Boolean,
    var x: Float,
    var attackAnim: Float = 0f,
    var hitFlash: Float = 0f,
    var dead: Boolean = false,
    var deathAnim: Float = 0f
)

private class Arrow(
    val target: BattleUnit,
    val fromX: Float,
    val fromY: Float,
    var progress: Float = 0f
)

private class DamageNumber(val text: String, val unit: BattleUnit?) {
    var life = 1f
}

private class Particle(var x: Float, var y: Float, val vx: Float, val vy: Float) {
    var life = 1f
}

private class Confetti(
    var x: Float,
    var y: Float,
    val vx: Float,
    var vy: Float,
    val color: Color,
    val size: Float,
    val spin: Float
) {
    var rot = Random.nextFloat() * 6.28f
    var life = 1f
}

@Composable
fun BattleScreen(
    army: List<GameCharacter>,
    towerName: String,
    towerHp: Float,
    onFinish: (Boolean) -> Unit
) {
    val units = remember(army) {
        army.mapIndexed { i, ch ->
            BattleUnit(
                type = ch.type,
                spriteName = ch.spriteName,
                maxHp = ch.health.toFloat(),
                hp = ch.health.toFloat(),
                damage = ch.damage,
                speed = ch.speed * 150f,
                fly = ch.isFlying,
                x = -80f - i * 70f
            )
        }
    }

    var towerCurrentHp by remember { mutableStateOf(towerHp) }
    val towerMaxHp = towerHp
    var elapsed by remember { mutableStateOf(0f) }
    var towerShake by remember { mutableStateOf(0f) }
    var towerX by remember { mutableStateOf(600f) }
    var groundYState by remember { mutableStateOf(0f) }
    var finished by remember { mutableStateOf(false) }
    var victory by remember { mutableStateOf(false) }
    val damageNumbers = remember { mutableStateListOf<DamageNumber>() }
    val arrows = remember { mutableStateListOf<Arrow>() }
    val particles = remember { mutableStateListOf<Particle>() }
    val confetti = remember { mutableStateListOf<Confetti>() }

    val context = LocalContext.current
    val towerBmp = remember { loadOptionalBitmap(context, "tower") }

    val spriteNames = listOf(
        "goblin", "dragon", "barbarian", "archer", "knight",
        "giant", "wizard", "healer", "assassin", "sorceress",
        "skeleton", "minotaur", "phoenix", "golem", "demon"
    )
    val spriteMap = remember {
        spriteNames.associateWith { name -> loadOptionalBitmap(context, name) }
    }

    val titlePaint = remember {
        Paint().apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
    }
    val dmgPaint = remember {
        Paint().apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
    }

    LaunchedEffect(units) {
        var troopTimer = 0.6f
        var towerTimer = 1.0f
        var arrowSpeed = 0.55f

        while (true) {
            delay(16)
            val dt = 0.016f
            elapsed += dt
            towerShake *= 0.85f

            val alive = units.filter { !it.dead && it.hp > 0f }

            units.forEachIndexed { i, u ->
                val stopX = towerX - 110f - i * 40f
                if (!u.dead && u.x < stopX) u.x += u.speed * dt
            }

            troopTimer -= dt
            if (troopTimer <= 0f && alive.isNotEmpty()) {
                val attackers = alive.filter { it.x >= towerX - 160f }
                if (attackers.isNotEmpty()) {
                    troopTimer = 0.8f
                    attackers.forEach { it.attackAnim = 1f }
                    val totalDmg = attackers.sumOf { it.damage }
                    towerCurrentHp = (towerCurrentHp - totalDmg).coerceAtLeast(0f)
                    towerShake = 9f
                    damageNumbers.add(DamageNumber("-$totalDmg", null))
                }
            }

            towerTimer -= dt
            if (towerTimer <= 0f && alive.isNotEmpty()) {
                towerTimer = 1.2f
                val nearest = alive.minByOrNull { it.x } ?: alive[0]
                arrows.add(
                    Arrow(
                        target = nearest,
                        fromX = towerX - 40f,
                        fromY = groundYState - 120f
                    )
                )
            }

            val iterator = arrows.iterator()
            while (iterator.hasNext()) {
                val arrow = iterator.next()
                arrow.progress += dt / arrowSpeed
                if (arrow.progress >= 1f) {
                    val target = arrow.target
                    if (!target.dead) {
                        target.hp = (target.hp - 30f).coerceAtLeast(0f)
                        target.hitFlash = 1f
                        damageNumbers.add(DamageNumber("-30", target))
                        if (target.hp <= 0f) {
                            target.dead = true
                            target.deathAnim = 1f
                            for (p in 0 until 10) {
                                particles.add(
                                    Particle(
                                        x = target.x,
                                        y = if (target.fly) groundYState - 150f else groundYState - 30f,
                                        vx = Random.nextFloat() * 140f - 70f,
                                        vy = -Random.nextFloat() * 140f
                                    )
                                )
                            }
                        }
                    }
                    iterator.remove()
                }
            }

            units.forEach {
                if (it.attackAnim > 0f) it.attackAnim -= dt * 3f
                if (it.hitFlash > 0f) it.hitFlash -= dt * 2.5f
                if (it.deathAnim > 0f) it.deathAnim -= dt * 1.5f
            }
            particles.forEach {
                it.life -= dt * 2f
                it.x += it.vx * dt
                it.y += it.vy * dt
            }
            particles.removeAll { it.life <= 0f }

            damageNumbers.forEach { it.life -= dt * 0.8f }
            damageNumbers.removeAll { it.life <= 0f }

            if (!finished) {
                if (towerCurrentHp <= 0f) {
                    victory = true
                    finished = true
                    spawnConfetti(confetti)
                } else if (alive.isEmpty() && elapsed > 3f) {
                    victory = false
                    finished = true
                    spawnConfetti(confetti)
                }
            }

            confetti.forEach { c ->
                c.life -= dt * 0.35f
                c.y += c.vy * dt
                c.x += c.vx * dt
                c.vy += 60f * dt
                c.rot += c.spin * dt
            }
            confetti.removeAll { it.life <= 0f }

            if (finished) {
                delay(1600)
                onFinish(victory)
                return@LaunchedEffect
            }
        }
    }

    Canvas(Modifier.fillMaxSize()) {
        towerX = size.width * 0.74f
        val groundY = size.height * 0.74f
        groundYState = groundY
        val w = size.width
        val h = size.height

        drawRect(Color(0xFF16233F), size = Size(w, h))
        drawCircle(Color(0xFFF5F5DC), radius = w * 0.06f, center = Offset(w * 0.12f, h * 0.12f))
        drawCircle(Color(0xFF16233F), radius = w * 0.05f, center = Offset(w * 0.14f, h * 0.10f))
        repeat(12) { s ->
            val sx = w * (0.05f + 0.9f * s / 11f)
            val sy = h * (0.04f + 0.1f * (s % 3))
            drawCircle(Color.White, radius = 1.5f, center = Offset(sx, sy))
        }

        drawRect(Color(0xFF2E7D32), topLeft = Offset(0f, groundY), size = Size(w, h - groundY))
        drawLine(Color(0xFF388E3C), Offset(0f, groundY), Offset(w, groundY), strokeWidth = 4f)

        drawTower(towerX, groundY, towerShake, towerCurrentHp / towerMaxHp, towerName, w, towerBmp)

        arrows.forEach { arrow ->
            val tx = arrow.target.x
            val ty = if (arrow.target.fly) groundY - 150f else groundY - 30f
            val ax = arrow.fromX + (tx - arrow.fromX) * arrow.progress
            val ay = arrow.fromY + (ty - arrow.fromY) * arrow.progress
            drawLine(
                Color(0xFFFFF59D),
                Offset(ax, ay),
                Offset(ax + 14f, ay - 10f),
                strokeWidth = 3.5f,
                cap = StrokeCap.Round
            )
        }

        units.forEach { u ->
            if (!u.dead) {
                val lunge = if (u.attackAnim > 0f) sin(u.attackAnim * PI).toFloat() * 18f else 0f
                val yBase = if (u.fly) {
                    groundY - 150f + sin(elapsed * 3f + u.x * 0.01f) * 10f
                } else {
                    groundY - 6f
                }
                val drawX = u.x + lunge

                val bmp = spriteMap[u.spriteName]
                if (bmp != null) {
                    val spriteW = if (u.fly) 64 else 48
                    val spriteH = if (u.fly) 56 else 56
                    drawImage(
                        bmp,
                        dstOffset = IntOffset((drawX - spriteW / 2).toInt(), (yBase - spriteH).toInt()),
                        dstSize = IntSize(spriteW, spriteH)
                    )
                } else {
                    when (u.spriteName) {
                        "goblin" -> drawGoblin(drawX, yBase, null)
                        "dragon" -> drawDragon(drawX, yBase, null)
                        "barbarian" -> drawMeleeHero(drawX, yBase, Color(0xFFD4A017), Color(0xFF8B6914))
                        "archer" -> drawRangedHero(drawX, yBase, Color(0xFF2E7D32), Color(0xFF1B5E20))
                        "knight" -> drawMeleeHero(drawX, yBase, Color(0xFF90CAF9), Color(0xFF1565C0))
                        "giant" -> drawGiant(drawX, yBase)
                        "wizard" -> drawRangedHero(drawX, yBase, Color(0xFF1565C0), Color(0xFF0D47A1))
                        "healer" -> drawRangedHero(drawX, yBase, Color(0xFFFFF9C4), Color(0xFFF57F17))
                        "assassin" -> drawMeleeHero(drawX, yBase, Color(0xFF4A148C), Color(0xFF1A0033))
                        "sorceress" -> drawRangedHero(drawX, yBase, Color(0xFF03A9F4), Color(0xFF01579B))
                        "skeleton" -> drawMeleeHero(drawX, yBase, Color(0xFFBDBDBD), Color(0xFF616161))
                        "minotaur" -> drawGiant(drawX, yBase)
                        "phoenix" -> drawDragon(drawX, yBase, null)
                        "golem" -> drawGiant(drawX, yBase)
                        "demon" -> drawDragon(drawX, yBase, null)
                        else -> drawGoblin(drawX, yBase, null)
                    }
                }

                if (u.hitFlash > 0f) {
                    drawCircle(
                        Color(1f, 1f, 1f, alpha = u.hitFlash.coerceIn(0f, 1f) * 0.8f),
                        radius = if (u.fly) 32f else 22f,
                        center = Offset(drawX, yBase)
                    )
                }
                drawTroopHpBar(u.x, yBase, u.hp / u.maxHp, u.fly)
            } else if (u.deathAnim > 0f) {
                val r = (1f - u.deathAnim) * 70f + 8f
                drawCircle(
                    Color(1f, 1f, 1f, alpha = u.deathAnim.coerceIn(0f, 1f) * 0.9f),
                    radius = r,
                    center = Offset(u.x, if (u.fly) groundY - 150f else groundY - 30f)
                )
            }
        }

        particles.forEach { p ->
            drawCircle(
                Color(0xFFFFC107).copy(alpha = p.life.coerceIn(0f, 1f)),
                radius = 5f,
                center = Offset(p.x, p.y)
            )
        }

        damageNumbers.forEach { d ->
            val alpha = d.life.coerceIn(0f, 1f)
            val dx = d.unit?.x ?: towerX
            val dyBase = d.unit?.let { if (it.fly) groundY - 150f else groundY - 55f }
                ?: (groundY - 140f)
            val dy = dyBase - (1f - d.life) * 70f
            dmgPaint.textSize = 30f
            dmgPaint.color = android.graphics.Color.argb(
                (alpha * 255).toInt(), 255, 82, 82
            )
            drawContext.canvas.nativeCanvas.drawText(d.text, dx, dy, dmgPaint)
        }

        if (elapsed < 1.2f) {
            val alpha = (1f - elapsed / 1.2f)
            titlePaint.textSize = w * 0.11f
            titlePaint.color = android.graphics.Color.argb(
                (alpha * 255).toInt(), 255, 193, 7
            )
            drawContext.canvas.nativeCanvas.drawText("BATTLE", w / 2f, h * 0.42f, titlePaint)
        }

        if (finished) {
            val tint = if (victory) Color(0xCC000000) else Color(0xCC2A0505)
            drawRect(tint, size = Size(w, h))

            confetti.forEach { c ->
                withTransform({
                    translate(c.x, c.y)
                    rotate(c.rot * 57.3f)
                }) {
                    drawRect(
                        c.color.copy(alpha = c.life.coerceIn(0f, 1f)),
                        topLeft = Offset(-c.size / 2, -c.size / 2),
                        size = Size(c.size, c.size * 0.6f)
                    )
                }
            }

            val cx = w / 2f
            val cy = h * 0.34f
            val shieldS = w * 0.13f

            drawResultShield(cx, cy, shieldS, victory)

            val bannerY = cy + shieldS * 1.7f
            val bannerW = w * 0.72f
            drawRect(
                Brush.horizontalGradient(listOf(VictoryBlueDark, VictoryBlue, VictoryBlueDark)),
                topLeft = Offset(cx - bannerW / 2, bannerY),
                size = Size(bannerW, h * 0.07f)
            )
            drawRect(
                Color(0x88FFFFFF),
                topLeft = Offset(cx - bannerW / 2, bannerY - 4f),
                size = Size(bannerW, 4f)
            )
            titlePaint.textSize = h * 0.055f
            titlePaint.color = android.graphics.Color.rgb(255, 255, 255)
            drawContext.canvas.nativeCanvas.drawText(
                if (victory) "VICTORY!" else "DEFEAT",
                cx, bannerY + h * 0.048f, titlePaint
            )

            titlePaint.textSize = h * 0.045f
            titlePaint.color = if (victory) android.graphics.Color.rgb(255, 213, 79)
            else android.graphics.Color.rgb(255, 120, 120)
            drawContext.canvas.nativeCanvas.drawText(
                if (victory) "+30 TROPHIES  +150 GOLD" else "-30 TROPHIES  +60 GOLD",
                cx, bannerY + h * 0.10f, titlePaint
            )

            if (victory) {
                drawChest(cx - w * 0.28f, bannerY + h * 0.12f, w * 0.075f)
                drawChest(cx + w * 0.28f, bannerY + h * 0.12f, w * 0.075f)
            }

            titlePaint.textSize = h * 0.03f
            titlePaint.color = android.graphics.Color.rgb(180, 190, 200)
            drawContext.canvas.nativeCanvas.drawText("TAP THE SCREEN TO CONTINUE", cx, h * 0.90f, titlePaint)
        }
    }
}

private fun DrawScope.drawTower(
    cx: Float, groundY: Float, shake: Float,
    hpRatio: Float, name: String, w: Float, bmp: ImageBitmap?
) {
    val ox = cx + shake
    val bodyW = 90f
    val bodyH = if (bmp != null) 120f else 180f
    val bodyTop = groundY - bodyH

    val barW = 120f
    val barH = 14f
    val barTop = bodyTop - 36f
    drawRoundRect(
        Color(0xFF111111),
        topLeft = Offset(ox - barW / 2 - 3f, barTop - 3f),
        size = Size(barW + 6f, barH + 6f),
        cornerRadius = CornerRadius(5f)
    )
    val barColor = if (hpRatio > 0.5f) Color(0xFF4CAF50)
    else if (hpRatio > 0.25f) Color(0xFFFFC107) else Color(0xFFEF5350)
    drawRoundRect(
        barColor,
        topLeft = Offset(ox - barW / 2, barTop),
        size = Size(barW * hpRatio.coerceIn(0f, 1f), barH),
        cornerRadius = CornerRadius(5f)
    )

    if (bmp != null) {
        drawImage(
            bmp,
            dstOffset = IntOffset((ox - 60f).toInt(), (groundY - 120f).toInt()),
            dstSize = IntSize(120, 120)
        )
    } else {
        drawRoundRect(
            Color(0xFF546E7A),
            topLeft = Offset(ox - bodyW / 2, bodyTop),
            size = Size(bodyW, bodyH),
            cornerRadius = CornerRadius(6f)
        )

        repeat(4) { i ->
            drawRect(
                Color(0xFF546E7A),
                topLeft = Offset(ox - bodyW / 2 + 4f + i * 22f, bodyTop - 22f),
                size = Size(18f, 22f)
            )
        }

        drawCircle(Color(0xFFFFE082), radius = 15f, center = Offset(ox, bodyTop + 55f))
        drawCircle(Color(0xFF2C2C2C), radius = 7f, center = Offset(ox, bodyTop + 55f))

        drawArc(
            Color(0xFF37474F), 180f, 180f, useCenter = false,
            topLeft = Offset(ox - 20f, groundY - 55f),
            size = Size(40f, 60f)
        )
    }

    titleTextPaint().apply {
        textSize = 22f
        color = android.graphics.Color.rgb(224, 224, 224)
    }
    drawContext.canvas.nativeCanvas.drawText(name, ox, groundY + 22f, titleTextPaint())
}

private fun DrawScope.drawGoblin(x: Float, y: Float, bmp: ImageBitmap?) {
    if (bmp != null) {
        drawImage(bmp, dstOffset = IntOffset((x - 24f).toInt(), (y - 52f).toInt()), dstSize = IntSize(48, 52))
        return
    }
    val r = 20f
    drawOval(Color(0x44000000), topLeft = Offset(x - r, y + 2f), size = Size(r * 2, 12f))
    drawCircle(Color(0xFF66BB6A), radius = r, center = Offset(x, y))
    drawCircle(Color(0xFF4CAF50), radius = 11f, center = Offset(x, y - 18f))
    drawCircle(Color.White, radius = 3.5f, center = Offset(x - 4f, y - 19f))
    drawCircle(Color.White, radius = 3.5f, center = Offset(x + 4f, y - 19f))
    drawCircle(Color.Black, radius = 1.8f, center = Offset(x - 4f, y - 19f))
    drawCircle(Color.Black, radius = 1.8f, center = Offset(x + 4f, y - 19f))
    drawLine(Color(0xFF90A4AE), Offset(x + 14f, y - 8f), Offset(x + 24f, y - 22f), strokeWidth = 4f, cap = StrokeCap.Round)
}

private fun DrawScope.drawDragon(x: Float, y: Float, bmp: ImageBitmap?) {
    if (bmp != null) {
        drawImage(bmp, dstOffset = IntOffset((x - 32f).toInt(), (y - 44f).toInt()), dstSize = IntSize(64, 48))
        return
    }
    drawArc(Color(0xFFEF9A9A), 180f, 120f, useCenter = true, topLeft = Offset(x - 46f, y - 32f), size = Size(42f, 42f))
    drawArc(Color(0xFFEF9A9A), 240f, 120f, useCenter = true, topLeft = Offset(x + 4f, y - 32f), size = Size(42f, 42f))
    drawCircle(Color(0xFFEF5350), radius = 30f, center = Offset(x, y))
    drawCircle(Color(0xFFFFCDD2), radius = 17f, center = Offset(x, y + 7f))
    drawArc(Color(0xFFEF5350), 90f, 150f, useCenter = false, topLeft = Offset(x - 46f, y - 22f), size = Size(44f, 55f))
    drawCircle(Color(0xFFC62828), radius = 18f, center = Offset(x + 26f, y - 16f))
    drawCircle(Color(0xFFFFF176), radius = 4.5f, center = Offset(x + 30f, y - 20f))
    drawCircle(Color.Black, radius = 2.2f, center = Offset(x + 31f, y - 20f))
}

private fun DrawScope.drawMeleeHero(x: Float, y: Float, primary: Color, dark: Color) {
    val bodyH = 44f
    val bodyW = 24f
    drawOval(Color(0x44000000), topLeft = Offset(x - 18f, y + 2f), size = Size(36f, 10f))
    drawRoundRect(dark, topLeft = Offset(x - bodyW / 2, y - bodyH), size = Size(bodyW, bodyH * 0.6f), cornerRadius = CornerRadius(4f))
    drawRoundRect(primary, topLeft = Offset(x - bodyW / 2, y - bodyH * 0.5f), size = Size(bodyW, bodyH * 0.5f), cornerRadius = CornerRadius(4f))
    drawCircle(Color(0xFFFFCC80), radius = 10f, center = Offset(x, y - bodyH - 6f))
    drawCircle(Color.White, radius = 2.5f, center = Offset(x - 3f, y - bodyH - 7f))
    drawCircle(Color.Black, radius = 1.2f, center = Offset(x - 3f, y - bodyH - 7f))
    drawCircle(Color.White, radius = 2.5f, center = Offset(x + 3f, y - bodyH - 7f))
    drawCircle(Color.Black, radius = 1.2f, center = Offset(x + 3f, y - bodyH - 7f))
    drawLine(Color(0xFF90A4AE), Offset(x + 16f, y - bodyH * 0.6f), Offset(x + 28f, y - bodyH - 4f), strokeWidth = 3f, cap = StrokeCap.Round)
}

private fun DrawScope.drawRangedHero(x: Float, y: Float, primary: Color, dark: Color) {
    val bodyH = 42f
    val bodyW = 20f
    drawOval(Color(0x44000000), topLeft = Offset(x - 16f, y + 2f), size = Size(32f, 10f))
    drawRoundRect(dark, topLeft = Offset(x - bodyW / 2, y - bodyH), size = Size(bodyW, bodyH * 0.55f), cornerRadius = CornerRadius(4f))
    drawRoundRect(primary, topLeft = Offset(x - bodyW / 2, y - bodyH * 0.45f), size = Size(bodyW, bodyH * 0.45f), cornerRadius = CornerRadius(4f))
    drawCircle(Color(0xFFFFCC80), radius = 9f, center = Offset(x, y - bodyH - 5f))
    drawCircle(Color.White, radius = 2.5f, center = Offset(x - 3f, y - bodyH - 6f))
    drawCircle(Color.Black, radius = 1.2f, center = Offset(x - 3f, y - bodyH - 6f))
    drawLine(primary, Offset(x + 12f, y - bodyH * 0.3f), Offset(x + 12f, y - bodyH - 14f), strokeWidth = 2.5f)
    drawLine(primary, Offset(x + 12f, y - bodyH - 14f), Offset(x + 20f, y - bodyH - 8f), strokeWidth = 2.5f)
    drawLine(primary, Offset(x + 12f, y - bodyH - 14f), Offset(x + 4f, y - bodyH - 8f), strokeWidth = 2.5f)
}

private fun DrawScope.drawGiant(x: Float, y: Float) {
    val bodyH = 56f
    val bodyW = 36f
    drawOval(Color(0x44000000), topLeft = Offset(x - 24f, y + 2f), size = Size(48f, 12f))
    drawRoundRect(Color(0xFF78909C), topLeft = Offset(x - bodyW / 2, y - bodyH), size = Size(bodyW, bodyH), cornerRadius = CornerRadius(6f))
    drawRoundRect(Color(0xFF546E7A), topLeft = Offset(x - bodyW / 2 - 4f, y - bodyH + 4f), size = Size(bodyW + 8f, 16f), cornerRadius = CornerRadius(4f))
    drawCircle(Color(0xFF90A4AE), radius = 14f, center = Offset(x, y - bodyH - 8f))
    drawCircle(Color(0xFF4FC3F7), radius = 3f, center = Offset(x - 5f, y - bodyH - 9f))
    drawCircle(Color(0xFF4FC3F7), radius = 3f, center = Offset(x + 5f, y - bodyH - 9f))
    drawLine(Color(0xFF78909C), Offset(x + bodyW / 2, y - bodyH * 0.4f), Offset(x + bodyW / 2 + 20f, y - bodyH * 0.2f), strokeWidth = 8f, cap = StrokeCap.Round)
    drawLine(Color(0xFF78909C), Offset(x - bodyW / 2, y - bodyH * 0.4f), Offset(x - bodyW / 2 - 20f, y - bodyH * 0.2f), strokeWidth = 8f, cap = StrokeCap.Round)
}

private fun DrawScope.drawTroopHpBar(cx: Float, y: Float, ratio: Float, fly: Boolean) {
    val top = if (fly) y - 90f else y - 48f
    val w = 44f
    val h = 6f
    drawRect(Color(0x66000000), topLeft = Offset(cx - w / 2, top), size = Size(w, h))
    drawRect(
        if (ratio > 0.5f) Color(0xFF4CAF50) else if (ratio > 0.25f) Color(0xFFFFC107) else Color(0xFFEF5350),
        topLeft = Offset(cx - w / 2, top),
        size = Size(w * ratio.coerceIn(0f, 1f), h)
    )
}

private var cachedTitlePaint: Paint? = null

private val VictoryBlue = Color(0xFF1565C0)
private val VictoryBlueDark = Color(0xFF0D47A1)
private val VictoryGold = Color(0xFFF5A623)
private val VictoryGoldDark = Color(0xFFC4841D)
private val VictoryGoldBorder = Color(0xFFFFD86B)
private val ShardGray = Color(0xFF9E9E9E)
private val ShardGrayDark = Color(0xFF616161)

private fun spawnConfetti(confetti: SnapshotStateList<Confetti>) {
    val colors = listOf(
        Color(0xFFF5A623), Color(0xFF42A5F5), Color(0xFF66BB6A),
        Color(0xFFEF5350), Color(0xFFAB47BC), Color(0xFFFFF176)
    )
    repeat(60) {
        confetti.add(
            Confetti(
                x = Random.nextFloat() * 800f,
                y = -20f - Random.nextFloat() * 200f,
                vx = Random.nextFloat() * 60f - 30f,
                vy = 90f + Random.nextFloat() * 90f,
                color = colors[Random.nextInt(colors.size)],
                size = 6f + Random.nextFloat() * 8f,
                spin = Random.nextFloat() * 8f - 4f
            )
        )
    }
}

private fun DrawScope.drawResultShield(cx: Float, cy: Float, s: Float, victory: Boolean) {
    val shield = Path().apply {
        moveTo(cx, cy - s)
        cubicTo(cx + s * 0.95f, cy - s * 0.8f, cx + s * 1.08f, cy - s * 0.1f, cx + s * 0.75f, cy + s * 0.45f)
        lineTo(cx, cy + s * 1.05f)
        lineTo(cx - s * 0.75f, cy + s * 0.45f)
        cubicTo(cx - s * 1.08f, cy - s * 0.1f, cx - s * 0.95f, cy - s * 0.8f, cx, cy - s)
        close()
    }
    val border = Path().apply {
        moveTo(cx, cy - s * 1.12f)
        cubicTo(cx + s * 1.05f, cy - s * 0.9f, cx + s * 1.2f, cy - s * 0.1f, cx + s * 0.85f, cy + s * 0.5f)
        lineTo(cx, cy + s * 1.16f)
        lineTo(cx - s * 0.85f, cy + s * 0.5f)
        cubicTo(cx - s * 1.2f, cy - s * 0.1f, cx - s * 1.05f, cy - s * 0.9f, cx, cy - s * 1.12f)
        close()
    }

    if (victory) {
        drawPath(border, Brush.linearGradient(listOf(VictoryGold, VictoryGoldDark, VictoryGoldDark)))
        drawPath(shield, Brush.linearGradient(listOf(VictoryGold, VictoryGoldDark)))
        val crown = Path().apply {
            moveTo(cx - s * 0.45f, cy - s * 1.15f)
            lineTo(cx - s * 0.28f, cy - s * 1.5f)
            lineTo(cx - s * 0.12f, cy - s * 1.2f)
            lineTo(cx, cy - s * 1.55f)
            lineTo(cx + s * 0.12f, cy - s * 1.2f)
            lineTo(cx + s * 0.28f, cy - s * 1.5f)
            lineTo(cx + s * 0.45f, cy - s * 1.15f)
            close()
        }
        drawPath(crown, VictoryGold)
        drawCircle(VictoryGoldBorder, radius = s * 0.08f, center = Offset(cx - s * 0.25f, cy - s * 1.22f))
        drawCircle(VictoryGoldBorder, radius = s * 0.08f, center = Offset(cx, cy - s * 1.26f))
        drawCircle(VictoryGoldBorder, radius = s * 0.08f, center = Offset(cx + s * 0.25f, cy - s * 1.22f))
        drawLine(VictoryGoldBorder, Offset(cx - s * 0.3f, cy - s * 0.2f), Offset(cx + s * 0.3f, cy + s * 0.2f), strokeWidth = s * 0.09f, cap = StrokeCap.Round)
        drawLine(VictoryGoldBorder, Offset(cx + s * 0.3f, cy - s * 0.2f), Offset(cx - s * 0.3f, cy + s * 0.2f), strokeWidth = s * 0.09f, cap = StrokeCap.Round)
    } else {
        drawPath(border, Brush.linearGradient(listOf(ShardGray, ShardGrayDark, Color(0xFF424242))))
        drawPath(shield, Brush.linearGradient(listOf(ShardGray, Color(0xFF757575))))
        val crack = Path().apply {
            moveTo(cx - s * 0.15f, cy - s)
            lineTo(cx + s * 0.1f, cy - s * 0.45f)
            lineTo(cx - s * 0.05f, cy - s * 0.15f)
            lineTo(cx + s * 0.18f, cy + s * 0.35f)
            lineTo(cx - s * 0.05f, cy + s * 0.75f)
        }
        drawPath(crack, Color(0xFF212121), style = Stroke(width = s * 0.09f, cap = StrokeCap.Round))
        val piece = Path().apply {
            moveTo(cx + s * 0.5f, cy - s * 0.5f)
            lineTo(cx + s * 0.85f, cy - s * 0.35f)
            lineTo(cx + s * 0.7f, cy - s * 0.05f)
            close()
        }
        drawPath(piece, ShardGray)
        drawPath(piece, Color(0xFF424242), style = Stroke(width = 3f))
    }
}

private fun DrawScope.drawChest(cx: Float, baseY: Float, s: Float) {
    drawOval(Color(0x55000000), topLeft = Offset(cx - s * 0.7f, baseY + s * 0.5f), size = Size(s * 1.4f, s * 0.25f))
    drawRoundRect(Color(0xFF6D4C2F), topLeft = Offset(cx - s * 0.55f, baseY - s * 0.35f), size = Size(s * 1.1f, s * 0.85f), cornerRadius = CornerRadius(s * 0.05f))
    drawRoundRect(Color(0xFF8B6914), topLeft = Offset(cx - s * 0.6f, baseY - s * 0.6f), size = Size(s * 1.2f, s * 0.3f), cornerRadius = CornerRadius(s * 0.05f))
    drawCircle(Color(0xFFFFD86B), radius = s * 0.09f, center = Offset(cx, baseY - s * 0.28f))
    drawCircle(Color(0xFF3E2723), radius = s * 0.04f, center = Offset(cx, baseY - s * 0.28f))
    drawLine(Color(0xFF8B6914), Offset(cx - s * 0.55f, baseY - s * 0.05f), Offset(cx + s * 0.55f, baseY - s * 0.05f), strokeWidth = s * 0.06f)
}

private fun titleTextPaint(): Paint {
    return cachedTitlePaint ?: Paint().apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }.also { cachedTitlePaint = it }
}

private fun loadOptionalBitmap(context: Context, name: String): ImageBitmap? {
    val id = context.resources.getIdentifier(name, "drawable", context.packageName)
    if (id == 0) return null
    return runCatching {
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            android.graphics.ImageDecoder.decodeBitmap(
                android.graphics.ImageDecoder.createSource(context.resources, id)
            ).asImageBitmap()
        } else {
            android.graphics.BitmapFactory.decodeResource(context.resources, id).asImageBitmap()
        }
    }.getOrNull()
}
