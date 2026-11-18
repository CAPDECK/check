package com.example.data.models

import androidx.compose.ui.graphics.Color
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class CustomPaletteModel(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val primaryHex: String,
    val secondaryHex: String = "",
    val surfaceHex: String = ""
)

object PaletteColorUtils {

    fun parseHexColor(hex: String, fallback: Color = Color(0xFF6366F1)): Color {
        val clean = hex.trim().removePrefix("#").removePrefix("0x")
        return try {
            when (clean.length) {
                6 -> Color(android.graphics.Color.parseColor("#$clean"))
                8 -> Color(android.graphics.Color.parseColor("#$clean"))
                3 -> {
                    val r = clean[0]
                    val g = clean[1]
                    val b = clean[2]
                    Color(android.graphics.Color.parseColor("#$r$r$g$g$b$b"))
                }
                else -> fallback
            }
        } catch (e: Exception) {
            fallback
        }
    }

    fun colorToHex(color: Color): String {
        val r = (color.red * 255).toInt().coerceIn(0, 255)
        val g = (color.green * 255).toInt().coerceIn(0, 255)
        val b = (color.blue * 255).toInt().coerceIn(0, 255)
        return String.format("#%02X%02X%02X", r, g, b)
    }

    fun hsvToColor(hue: Float, saturation: Float, value: Float): Color {
        val hsv = floatArrayOf(
            hue.coerceIn(0f, 360f),
            saturation.coerceIn(0f, 1f),
            value.coerceIn(0f, 1f)
        )
        val intColor = android.graphics.Color.HSVToColor(hsv)
        return Color(intColor)
    }

    fun colorToHsv(color: Color): FloatArray {
        val hsv = FloatArray(3)
        val r = (color.red * 255).toInt().coerceIn(0, 255)
        val g = (color.green * 255).toInt().coerceIn(0, 255)
        val b = (color.blue * 255).toInt().coerceIn(0, 255)
        android.graphics.Color.colorToHSV(android.graphics.Color.rgb(r, g, b), hsv)
        return hsv
    }

    fun isColorLight(color: Color): Boolean {
        val lum = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
        return lum > 0.58f
    }

    fun parseCustomPalettesJson(json: String): List<CustomPaletteModel> {
        if (json.isBlank()) return emptyList()
        val result = mutableListOf<CustomPaletteModel>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                result.add(
                    CustomPaletteModel(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        name = obj.optString("name", "Custom Palette"),
                        primaryHex = obj.optString("primaryHex", "#6366F1"),
                        secondaryHex = obj.optString("secondaryHex", ""),
                        surfaceHex = obj.optString("surfaceHex", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    fun serializeCustomPalettes(list: List<CustomPaletteModel>): String {
        val arr = JSONArray()
        list.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("name", item.name)
            obj.put("primaryHex", item.primaryHex)
            obj.put("secondaryHex", item.secondaryHex)
            obj.put("surfaceHex", item.surfaceHex)
            arr.put(obj)
        }
        return arr.toString()
    }

    val QuickSwatches = listOf(
        "#6366F1" to "Indigo",
        "#8B5CF6" to "Violet",
        "#A855F7" to "Purple",
        "#EC4899" to "Pink",
        "#F43F5E" to "Rose",
        "#EF4444" to "Red",
        "#DC2626" to "Crimson",
        "#F97316" to "Orange",
        "#F59E0B" to "Amber",
        "#EAB308" to "Gold",
        "#84CC16" to "Lime",
        "#22C55E" to "Emerald",
        "#10B981" to "Teal Mint",
        "#14B8A6" to "Teal",
        "#06B6D4" to "Cyan",
        "#0EA5E9" to "Sky Blue",
        "#3B82F6" to "Electric Blue",
        "#2563EB" to "Cobalt",
        "#64748B" to "Slate",
        "#78716C" to "Stone",
        "#9333EA" to "Neon Purple",
        "#059669" to "Forest Mint",
        "#D97706" to "Warm Ochre",
        "#B91C1C" to "Ruby"
    )

    val BuiltInThemes = listOf(
        CustomPaletteModel(
            id = "preset_indigo",
            name = "Royal Indigo",
            primaryHex = "#6366F1",
            secondaryHex = "#818CF8",
            surfaceHex = "#0F111E"
        ),
        CustomPaletteModel(
            id = "preset_cyber",
            name = "Cyberpunk Aqua",
            primaryHex = "#00E5FF",
            secondaryHex = "#00B0FF",
            surfaceHex = "#0D1117"
        ),
        CustomPaletteModel(
            id = "preset_emerald",
            name = "Emerald Forest",
            primaryHex = "#10B981",
            secondaryHex = "#34D399",
            surfaceHex = "#0C1410"
        ),
        CustomPaletteModel(
            id = "preset_amber",
            name = "Golden Sunset",
            primaryHex = "#F59E0B",
            secondaryHex = "#FBBF24",
            surfaceHex = "#16130D"
        ),
        CustomPaletteModel(
            id = "preset_rose",
            name = "Crimson Rose",
            primaryHex = "#F43F5E",
            secondaryHex = "#FB7185",
            surfaceHex = "#160D10"
        ),
        CustomPaletteModel(
            id = "preset_lavender",
            name = "Lavender Mist",
            primaryHex = "#C084FC",
            secondaryHex = "#E9D5FF",
            surfaceHex = "#130F1A"
        )
    )
}
