package com.fueltheride.data

import android.content.Context
import android.content.SharedPreferences
import com.fueltheride.model.BottleConfig
import com.fueltheride.model.FoodItem
import com.fueltheride.model.FuelConfig

/**
 * Persiste y recupera [FuelConfig] en SharedPreferences.
 *
 * Solo se guarda lo que el usuario configura manualmente: FTP (opcional, si no
 * usa el del perfil del Karoo) y el bidón. El peso y las raciones de comida/bebida
 * NO se guardan: el peso viene del perfil del Karoo y las raciones se calculan
 * automáticamente según el gasto energético.
 */
class ConfigRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(FuelConfig.PREFS_NAME, Context.MODE_PRIVATE)

    fun save(config: FuelConfig) {
        prefs.edit().apply {
            putInt(KEY_FTP, config.ftpWatts)
            putBoolean(KEY_USE_KAROO_FTP, config.useKarooFtp)
            putInt(KEY_BOTTLE_VOLUME, config.bottle.volumeMl)
            putInt(KEY_BOTTLE_CARBS, config.bottle.carbsGrams)
            putBoolean(KEY_BOTTLE_SALTS, config.bottle.hasSalts)
            putInt(KEY_BOTTLE_REFILLS, config.bottle.carbBottleRefills)
            putString(KEY_FOOD_ITEMS, serializeFood(config.foodItems))
            putInt(KEY_PRERIDE_CARBS, config.preRideCarbsG)
            putFloat(KEY_PRERIDE_HOURS, config.preRideHoursBefore)
            apply()
        }
    }

    fun load(): FuelConfig {
        return FuelConfig(
            ftpWatts = prefs.getInt(KEY_FTP, 0),
            useKarooFtp = prefs.getBoolean(KEY_USE_KAROO_FTP, true),
            bottle = BottleConfig(
                volumeMl = prefs.getInt(KEY_BOTTLE_VOLUME, 750),
                carbsGrams = prefs.getInt(KEY_BOTTLE_CARBS, 0),
                hasSalts = prefs.getBoolean(KEY_BOTTLE_SALTS, false),
                carbBottleRefills = prefs.getInt(KEY_BOTTLE_REFILLS, 1)
            ),
            foodItems = deserializeFood(prefs.getString(KEY_FOOD_ITEMS, null)),
            preRideCarbsG = prefs.getInt(KEY_PRERIDE_CARBS, 0),
            preRideHoursBefore = prefs.getFloat(KEY_PRERIDE_HOURS, 1f)
        )
    }

    /** Serializa la despensa como "nombre:carbos;nombre:carbos". */
    private fun serializeFood(items: List<FoodItem>): String =
        items.joinToString(";") { "${it.name.replace(";", "").replace(":", "")}:${it.carbsG}" }

    /** Reconstruye la despensa; si no hay nada guardado, un gel de 30 g por defecto. */
    private fun deserializeFood(raw: String?): List<FoodItem> {
        if (raw.isNullOrBlank()) return listOf(FoodItem("Gel", 30))
        return raw.split(";").mapNotNull { entry ->
            val parts = entry.split(":")
            val name = parts.getOrNull(0)?.trim().orEmpty()
            val carbs = parts.getOrNull(1)?.trim()?.toIntOrNull()
            if (name.isNotEmpty() && carbs != null && carbs > 0) FoodItem(name, carbs) else null
        }.ifEmpty { listOf(FoodItem("Gel", 30)) }
    }

    companion object {
        private const val KEY_FTP = "ftp_watts"
        private const val KEY_USE_KAROO_FTP = "use_karoo_ftp"
        private const val KEY_BOTTLE_VOLUME = "bottle_volume_ml"
        private const val KEY_BOTTLE_CARBS = "bottle_carbs_g"
        private const val KEY_BOTTLE_SALTS = "bottle_salts"
        private const val KEY_BOTTLE_REFILLS = "bottle_carb_refills"
        private const val KEY_FOOD_ITEMS = "food_items"
        private const val KEY_PRERIDE_CARBS = "preride_carbs_g"
        private const val KEY_PRERIDE_HOURS = "preride_hours_before"
    }
}
