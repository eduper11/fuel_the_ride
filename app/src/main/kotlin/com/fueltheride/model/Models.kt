package com.fueltheride.model

/**
 * Una pieza de comida que el ciclista lleva (gel, barrita, guayaba…).
 * Define su nombre y los gramos de carbohidratos que aporta.
 */
data class FoodItem(
    val name: String,
    val carbsG: Int
)

/**
 * Bottle configuration: size, carb content, salts flag.
 */
data class BottleConfig(
    /** Total volume in ml */
    val volumeMl: Int,
    /** Total carbohydrates in the full bottle, in grams (0 = plain water) */
    val carbsGrams: Int = 0,
    /** Whether the bottle contains electrolytes/salts */
    val hasSalts: Boolean = false,
    /**
     * Nº de "cargas" del bidón con mezcla de carbos que se llevan en la ruta.
     * 1 = un bidón; 2 = se recarga una vez con la misma mezcla, etc. Al agotar
     * ese volumen total, los siguientes tragos cuentan 0 g (bidón con agua).
     */
    val carbBottleRefills: Int = 1
) {
    companion object {
        val BOTTLE_500 = BottleConfig(volumeMl = 500)
        val BOTTLE_750 = BottleConfig(volumeMl = 750)
    }

    /** Carb concentration in g/ml */
    val carbConcentration: Float
        get() = if (volumeMl > 0) carbsGrams.toFloat() / volumeMl else 0f

    /** Presupuesto TOTAL de carbos de bebida en la ruta (g): carbos × nº de cargas. */
    val totalCarbBudgetG: Int
        get() = carbsGrams * carbBottleRefills.coerceAtLeast(1)
}

/**
 * User-configured fueling strategy.
 */
data class FuelConfig(
    /** FTP in watts. If 0, use FTP from Karoo user profile. */
    val ftpWatts: Int = 0,
    /** Whether to read FTP from the Karoo UserProfile instead */
    val useKarooFtp: Boolean = true,
    /** Bottle setup (size + carb content + salts) */
    val bottle: BottleConfig = BottleConfig.BOTTLE_750,
    /**
     * Comida que lleva el ciclista. La app rota entre estas piezas y, cuando el
     * déficit energético de carbos alcanza el tamaño de la siguiente pieza, avisa
     * de comerla concretamente (ej. "Gel 45 g").
     */
    val foodItems: List<FoodItem> = listOf(FoodItem("Gel", 30)),
    /**
     * Carbohidratos ingeridos ANTES de la actividad (g). Llenan el glucógeno y dan
     * un margen inicial antes de depender de lo que comes durante la marcha.
     */
    val preRideCarbsG: Int = 0,
    /**
     * Horas antes de la actividad en que se ingirieron esos carbos. Cuanto más
     * tiempo ha pasado, menos quedan disponibles (se han ido usando/almacenando).
     */
    val preRideHoursBefore: Float = 1f
) {
    companion object {
        /** SharedPreferences key prefix */
        const val PREFS_NAME = "fuel_config"
    }
}

/**
 * A single alert that the extension shows on screen + plays a sound for.
 */
data class FuelAlert(
    val type: AlertType,
    /** ml to drink (hydration alerts) */
    val mlToDrink: Int = 0,
    /** g of carbs to eat (solid alerts) */
    val carbsToEatG: Int = 0,
    /** Nombre de la pieza de comida sugerida (ej. "Gel", "Guayaba"). */
    val foodName: String = "",
    /** Timestamp when this alert was triggered (system millis) */
    val triggeredAtMs: Long = System.currentTimeMillis()
) {
    enum class AlertType { DRINK, EAT, DRINK_AND_EAT }
}

/**
 * Real-time accumulator of what the rider has consumed and burned.
 */
data class RideNutritionState(
    val totalCarbsConsumedG: Int = 0,
    val totalHydrationConsumedMl: Int = 0,
    val estimatedKcalBurned: Int = 0,
    /** Active alert being displayed; null when no alert active */
    val activeAlert: FuelAlert? = null,
    /** Millis until next drink alert */
    val msUntilNextDrink: Long = 0L,
    /** Millis until next eat alert */
    val msUntilNextEat: Long = 0L
)
