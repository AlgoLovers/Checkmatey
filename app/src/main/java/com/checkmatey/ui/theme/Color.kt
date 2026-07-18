package com.checkmatey.ui.theme

import androidx.compose.ui.graphics.Color

// Checkmatey bright palette (2026-07 redesign) — clean, floating, "toss/kakao"-style.
// A soft tinted canvas + white cards that float (no glare), one brand colour used sparingly on
// CTAs and key accents, and charcoal text. Colour is a role, not decoration.
// Semantic accents line up with the review board arrows: green = good, blue = the better move,
// amber = guidance, coral = a mistake.

// ── Brand (Primary) — emerald green (the board's colour, refined) ──
val Accent = Color(0xFF1E9E5B)
val AccentSoft = Color(0xFFD3F1E0) // primaryContainer (soft brand background)
val AccentDark = Color(0xFF0A5230) // onPrimaryContainer (text on the soft background)
val AccentLight = Color(0xFF7BD6A6) // dark-theme primary

// ── Info / the better move (Secondary) — indigo blue ──
val Info = Color(0xFF3D6EF0)
val InfoSoft = Color(0xFFE1E7FF)
val InfoDark = Color(0xFF122B84)
val InfoLight = Color(0xFFAEBFFF)

// ── Guidance / streak / promo (Tertiary) — amber ──
val Amber = Color(0xFFC77D0E)
val AmberSoft = Color(0xFFFFF1D8)
val AmberDark = Color(0xFF5A3800)
val AmberLight = Color(0xFFFFC46A)

// ── Mistake / wrong (Error) — coral ──
val Coral = Color(0xFFE24A40)
val CoralSoft = Color(0xFFFDE7E5)
val CoralDark = Color(0xFF77140E)
val CoralLight = Color(0xFFFFB4AD)

// ── Neutrals: light (tinted canvas + white floating cards) ──
val CanvasLight = Color(0xFFF3F5F3) // background (warm green-grey — softer than pure white)
val CardLight = Color(0xFFFFFFFF) // surfaceContainer (cards float in white)
val CardHighLight = Color(0xFFEAEEEA) // surfaceContainerHigh
val BorderLight = Color(0xFFE1E6E1) // outlineVariant (faint hairline)
val OutlineLight = Color(0xFFAFB8B0) // outline
val InkLight = Color(0xFF16201A) // onSurface (charcoal — softer than pure black)
val BodyLight = Color(0xFF586158) // onSurfaceVariant (body / caption)

// ── Neutrals: dark ──
val CanvasDark = Color(0xFF111512)
val CardDark = Color(0xFF1D231F)
val CardHighDark = Color(0xFF272E28)
val BorderDark = Color(0xFF333B35)
val OutlineDark = Color(0xFF69736B)
val InkDark = Color(0xFFE5E9E5)
val BodyDark = Color(0xFFA7B1A8)

// ── Chess board square colours (used by ui/board/ChessBoard) — classic, unchanged ──
val BoardLight = Color(0xFFEEEED2)
val BoardDark = Color(0xFF769656)
val BoardHighlight = Color(0xFFF6F669)
