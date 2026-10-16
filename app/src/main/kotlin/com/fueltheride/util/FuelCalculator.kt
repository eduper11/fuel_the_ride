package com.fueltheride.util

import com.fueltheride.model.FuelConfig
import kotlin.math.roundToInt

/**
 * FuelCalculator — motor de nutrición basado en fisiología real.
 *
 * BASE CIENTÍFICA
 * ---------------
 * 1) CONCEPTO DE CROSSOVER (Brooks & Mercier): cuanto mayor es la intensidad,
 *    mayor es la cantidad absoluta de energía que viene de carbohidratos. Por
 *    encima de ~85% del umbral el cuerpo casi no oxida grasa. → La TASA de gasto
 *    de carbos la marca la intensidad reciente.
 *
 * 2) VACIADO PROGRESIVO DEL GLUCÓGENO: a medida que avanza la actividad el
 *    glucógeno muscular se agota (60-80% vaciado pasadas ~2 h) y el cuerpo pasa
 *    a depender cada vez más de la glucosa de la sangre (la que ingieres). Por
 *    eso la guía sube de 60 a 90 g/h cuando el ejercicio supera ~2,5 h. → El
 *    GASTO ACUMULADO empuja la demanda hacia arriba.
 *
 * 3) TECHO DE ABSORCIÓN INTESTINAL (Jeukendrup): por mucho que quemes, el
 *    intestino solo absorbe ~60 g/h de un carbohidrato simple, o ~90 g/h con
 *    mezcla glucosa+fructosa 2:1. La INGESTA se topa ahí.
 *
 * MODELO DE INTENSIDAD
 * --------------------
 * La intensidad que alimenta la demanda es una "potencia efectiva" que pondera:
 *   - Media de los últimos 20 min (peso 75%) → el esfuerzo reciente domina, que
 *     es lo que de verdad marca la tasa de vaciado de glucógeno AHORA.
 *   - Media de toda la actividad (peso 25%) → contexto del esfuerzo global.
 * Esa mezcla la calcula el motor; aquí se recibe ya como effectivePower.
 *
 * Ni el vatio instantáneo (ruidoso) ni la media de todo el entreno a secas
 * (diluye lo reciente): una mezcla ponderada hacia los últimos 20 min.
 */
object FuelCalculator {

    /** Techo de absorción con mezcla glucosa+fructosa (g/h). */
    const val CARB_INTAKE_CAP_GH = 90
    /** Suelo de demanda de carbos pedaleando suave (g/h). */
    const val CARB_INTAKE_FLOOR_GH = 30

    /**
     * kJ de TRABAJO de referencia para "depósito de glucógeno vacío".
     *
     * IMPORTANTE: kjBurned son kJ de trabajo mecánico (vatios × tiempo), no la
     * energía metabólica. El glucógeno de un ciclista (~450 g ≈ 1800 kcal ≈ 7500 kJ
     * metabólicos) equivale, al rendimiento mecánico típico (~24%), a unos
     * ~1800 kJ de TRABAJO. Por eso la referencia es 1800 (no 7000): si no, el
     * multiplicador apenas crecía y la demanda salía demasiado baja.
     */
    const val GLYCOGEN_REFERENCE_KJ = 1800.0
    /**
     * Uplift máximo de demanda por vaciado de glucógeno (+50%). Refleja la guía
     * de subir de ~60 a ~90 g/h en esfuerzos largos (Jeukendrup): +50%.
     */
    const val MAX_DEPLETION_UPLIFT = 0.50
    /** kJ de trabajo equivalentes por gramo de carbohidrato (≈16,7 kJ metab. × 24%). */
    const val WORK_KJ_PER_CARB_G = 4.0

    /** Trabajo en kJ producido en `seconds` segundos a `watts` vatios. */
    fun kjFromPower(watts: Int, seconds: Int = 1): Double {
        if (watts <= 0) return 0.0
        return watts.toDouble() * seconds / 1000.0
    }

    /**
     * Multiplicador por vaciado de glucógeno: 1.0 al empezar → 1.30 cuando el
     * gasto acumulado se acerca a vaciar el depósito. Refleja que, a igualdad de
     * intensidad, cuanto más llevas gastado más dependes de los carbos ingeridos.
     */
    fun depletionUplift(kjBurned: Double): Double {
        val frac = (kjBurned / GLYCOGEN_REFERENCE_KJ).coerceIn(0.0, 1.0)
        return 1.0 + MAX_DEPLETION_UPLIFT * frac
    }

    /**
     * Demanda de carbohidratos (g/h) = curva de intensidad (crossover) × uplift
     * por vaciado de glucógeno, topada al límite intestinal.
     *
     * @param effectivePower potencia efectiva (mezcla 20 min + total) en W
     * @param ftp FTP en W
     * @param kjBurned energía total gastada en la actividad (kJ)
     */
    fun carbDemandGh(effectivePower: Double, ftp: Int, kjBurned: Double): Double {
        // Sin FTP o sin esfuerzo reciente → sin demanda (no se acumula nada).
        if (ftp <= 0 || effectivePower <= 0.0) return 0.0
        val i = effectivePower / ftp                 // fracción del FTP
        val lo = 0.55; val hi = 1.05                 // rampa de intensidad
        val frac = ((i - lo) / (hi - lo)).coerceIn(0.0, 1.0)
        val base = CARB_INTAKE_FLOOR_GH + frac * (CARB_INTAKE_CAP_GH - CARB_INTAKE_FLOOR_GH)
        val demand = base * depletionUplift(kjBurned)
        // Nunca por encima del techo de absorción intestinal.
        return demand.coerceAtMost(CARB_INTAKE_CAP_GH.toDouble())
    }

    /**
     * Factor de temperatura sobre la hidratación. La sudoración sube mucho con el
     * calor: ~1,0 a 20 °C, subiendo con la temperatura ambiente. Acotado para no
     * disparar (ni penalizar de más en frío).
     *   20 °C → 1,00 · 25 °C → 1,23 · 30 °C → 1,45 · 35 °C → 1,68
     */
    fun temperatureFactor(tempC: Double): Double =
        (1.0 + 0.045 * (tempC - 20.0)).coerceIn(0.85, 1.75)

    /**
     * Demanda de hidratación (ml/h) según intensidad efectiva, peso y temperatura.
     * La sudoración sube con la intensidad, con la masa corporal y con el calor.
     * No lleva uplift de glucógeno (eso es específico de los carbos).
     */
    fun hydrationDemandMlh(
        effectivePower: Double, ftp: Int, weightKg: Float = 70f, tempC: Double = 20.0
    ): Double {
        if (ftp <= 0 || effectivePower <= 0.0) return 0.0
        val i = effectivePower / ftp
        val frac = ((i - 0.5) / (1.0 - 0.5)).coerceIn(0.0, 1.0)
        val base = 450.0 + frac * (850.0 - 450.0)    // 450 → 850 ml/h pedaleando
        val weightFactor = (weightKg / 70f).coerceIn(0.7f, 1.4f)
        return base * weightFactor * temperatureFactor(tempC)
    }

    /** Carbohidratos (g) aportados al beber `mlConsumed` del bidón configurado. */
    fun carbsFromDrinking(mlConsumed: Int, config: FuelConfig): Int {
        return (mlConsumed * config.bottle.carbConcentration).roundToInt()
    }

    /**
     * Suelo de demanda de carbos (g/h) mientras se rueda, aunque ahora no haya
     * vatios (p. ej. bajando un puerto tras subirlo). Lleva el uplift de glucógeno,
     * porque vienes de gastar: el cuerpo sigue vaciado aunque ahora no pedalees.
     */
    fun carbFloorGh(kjBurned: Double): Double =
        CARB_INTAKE_FLOOR_GH * depletionUplift(kjBurned)

    /** Suelo de demanda de hidratación (ml/h) mientras se rueda, escalado por peso. */
    fun hydrationFloorMlh(weightKg: Float = 70f): Double {
        val factor = (weightKg / 70f).coerceIn(0.7f, 1.4f)
        return 450.0 * factor
    }

    /**
     * Carbohidratos previos EFECTIVOS (g) que siguen disponibles al empezar.
     *
     * No todo lo que comiste antes sigue "en reserva": parte ya se ha oxidado o
     * almacenado. Modelamos una disponibilidad que decae con las horas:
     *   - ~1 h antes  → casi todo disponible (~90%)
     *   - ~3 h antes  → la mitad (~50%)
     *   - ~4 h o más  → poco aprovechable directamente (~35%)
     * Decaimiento exponencial suave (vida media ~3 h).
     */
    fun preRideAvailableCarbsG(grams: Int, hoursBefore: Float): Double {
        if (grams <= 0) return 0.0
        val h = hoursBefore.coerceIn(0f, 6f)
        val availability = Math.pow(0.5, (h / 3.0))   // vida media 3 h
        return grams * availability
    }
}
