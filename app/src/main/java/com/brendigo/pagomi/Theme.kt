package com.brendigo.pagomi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

// Central design tokens for the original pagomi. visual identity.
object Brand {
  // Brand tokens live here. Edit this single object to re-skin the application.
  const val name = "pagomi."
  const val fullName = "pagomi. – PDF Scanner & Editor"

  val primary = Color(0xFF6459D8)
  val primarySoft = Color(0xFFE8E5FF)
  val paper = Color(0xFFF6F7F4)
  val onPrimary = Color.White
  val danger = Color(0xFFB42332)
  val onDanger = Color.White
  val tangerine = primary // compatibility alias; use primary for new components
  val ink = Color(0xFF192B36)
  val cherry = ink // compatibility alias
  val mint = Color(0xFFCDF2DE)
  val cool = mint // compatibility alias
}

@Immutable
data class Palette(
  val bg: Color, // screen background
  val surface: Color, // grouped lists, inputs, dialogs
  val sunken: Color, // tiles behind artwork, pressed rows, search, segmented track
  val page: Color, // paper inside illustrations
  val ink: Color, // text and primary buttons
  val sub: Color,
  val faint: Color,
  val line: Color,
  val lineStrong: Color,
  val accent: Color,
  val accentSoft: Color,
  val accentInk: Color,
  val onInk: Color, // text on primary buttons
  val hero: List<Color>, // the loud surface: base, middle, glow
  val panel: Color, // the floating tab bar's glass
  val onPanel: Color,
  val onPanelSub: Color,
  val dark: Boolean,
)

val LightPalette = Palette(
  bg = Brand.paper, surface = Color(0xFFFFFFFF), sunken = Color(0xFFE9ECE8), page = Color(0xFFFFFFFF),
  ink = Brand.cherry, sub = Color(0xFF52636E), faint = Color(0xFF78858E), line = Color(0xFFE1E5E1),
  lineStrong = Color(0xFFCFD7D9), accent = Brand.tangerine, accentSoft = Brand.primarySoft, accentInk = Color(0xFF5448C3),
  onInk = Color(0xFFFFFFFF),
  hero = listOf(Brand.cherry, Color(0xFF4E459F), Brand.tangerine),
  panel = Brand.cherry, onPanel = Color(0xFFFFFFFF), onPanelSub = Color(0xFFBECBD2),
  dark = false,
)

// Dark is a neutral warm near-black (after Poetica), not a cherry tint: violet is the only saturated colour in it.
val DarkPalette = Palette(
  bg = Color(0xFF101B23), surface = Color(0xFF192B36), sunken = Color(0xFF233742), page = Color(0xFF283D49),
  ink = Color(0xFFEDEDEB), sub = Color(0xFF9C9C99), faint = Color(0xFF6B6A66), line = Color(0xFF30444F),
  lineStrong = Color(0xFF445966), accent = Color(0xFF8B83F4), accentSoft = Color(0xFF272538), accentInk = Color(0xFFC2BDFF),
  onInk = Brand.ink,
  hero = listOf(Color(0xFF1C1B19), Color(0xFF403978), Brand.tangerine), // near-black, the glow alone carries the colour
  panel = Brand.ink, onPanel = Color(0xFFEDEDEB), onPanelSub = Color(0xFF9C9C99),
  dark = true,
)

// The scanner is its own dark, camera-style surface in both themes: the dark palette's near-blacks, with violet for
// detection and actions (and deep ink on top of it).
object ScanUi {
  val bg = Color(0xFF0B0B0A)
  val panel = Brand.ink
  val raised = Color(0xFF30444F)
  val text = Color(0xFFFFFFFF)
  val dim = Color(0xFF9C9C99)
  val line = Color(0xFF2A2A27)
  val accent = Brand.tangerine
  val accentSoft = Color(0x386459D8)
  val onAccent = Brand.onPrimary
  val scrim = Color(0x99000000)
}

val LocalPalette = staticCompositionLocalOf { LightPalette }

/** The active palette. */
val c: Palette @Composable get() = LocalPalette.current

/** Foreground for the selected accent surface, in either theme. */
val Palette.onAccent: Color get() = if (dark) Brand.ink else Brand.onPrimary

// Poppins throughout. It runs tall, so line heights are ~1.3x to keep accents and descenders unclipped.
val Poppins = FontFamily(
  Font(R.font.poppins_regular, FontWeight.Normal),
  Font(R.font.poppins_medium, FontWeight.Medium),
  Font(R.font.poppins_semibold, FontWeight.SemiBold),
)

object Type {
  private fun s(size: Float, weight: FontWeight, line: Float? = null, spacing: Float = 0f) = TextStyle(
    fontFamily = Poppins, fontWeight = weight, fontSize = size.sp,
    lineHeight = (line ?: (size * 1.32f)).sp, letterSpacing = spacing.sp,
  )
  val largeTitle = s(32f, FontWeight.SemiBold, 42f, -0.5f)
  val title = s(25f, FontWeight.SemiBold, 33f, -0.3f)
  val hero = s(25f, FontWeight.SemiBold, 33f, -0.4f)
  val body = s(15f, FontWeight.Normal, 22f)
  val small = s(13f, FontWeight.Normal, 19f)
  val item = s(15f, FontWeight.Medium)
  val label = s(13f, FontWeight.SemiBold)
  val button = s(15f, FontWeight.SemiBold)
  val num = s(12.5f, FontWeight.Medium)
  val med = s(13.5f, FontWeight.Medium)
  val bold = s(14f, FontWeight.SemiBold)
  val big = s(26f, FontWeight.SemiBold, 32f, -0.6f) // the reference's big numbers: sizes and counts
  val caption = s(11.5f, FontWeight.Medium, 16f, 1.4f) // uppercase section labels, spaced out
}

// Deliberately not one radius for everything: small things are tighter.
object Radius {
  val tile = 10.dp
  val control = 12.dp
  val group = 16.dp
  val card = 22.dp
  val hero = 30.dp
}

enum class ThemePref { System, Light, Dark }

object ThemeState {
  private val file get() = File(appContext.filesDir, "theme.txt")
  var pref by mutableStateOf(read())
    private set

  private fun read(): ThemePref = try {
    when (file.takeIf { it.exists() }?.readText()?.trim()) {
      "light" -> ThemePref.Light
      "dark" -> ThemePref.Dark
      else -> ThemePref.System
    }
  } catch (_: Exception) {
    ThemePref.System
  }

  fun set(p: ThemePref) {
    pref = p
    try { file.writeText(p.name.lowercase()) } catch (_: Exception) {}
  }
}
