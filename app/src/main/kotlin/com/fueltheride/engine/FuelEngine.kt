package com.fueltheride.engine

import android.content.Context
import android.util.Log
import com.fueltheride.data.ConfigRepository
import com.fueltheride.model.FuelAlert
import com.fueltheride.model.FuelConfig
import com.fueltheride.model.FoodItem
import com.fueltheride.util.FuelCalculator
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.OnStreamState
import io.hammerhead.karooext.models.PlayBeepPattern
import io.hammerhead.karooext.models.RideState
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UserProfile

/**
 * FuelEngine — motor energético (sin intervalos fijos).
 *
 * Integra la potencia segundo a segundo para obtener los kJ gastados y, según
 * la intensidad (% FTP), acumula el OBJETIVO de carbohidratos (g) e hidratación
 * (ml). Cuando el déficit (objetivo − consumido) alcanza una ración, dispara la
 * alerta correspondiente con su sonido. Igual que FuelTheGoal.
 */
class FuelEngine(context: Context) {

    companion object {
        private const val TAG = "FuelEngine"
        /** Tamaño de un buche realista (ml). */
        private const val SIP_ML = 150
        const val ALERT_DISPLAY_SECS = 30
    }

    private val configRepo = ConfigRepository(context)
    private var config: FuelConfig = configRepo.load()

    // ── Estado del entreno ───────────────────────────────────────────────────
    @Volatile var rideActive = false; private set
    /** True mientras la actividad está pausada: congela tiempo, energía y avisos. */
    @Volatile var paused = false; private set
    @Volatile private var currentPowerWatts = 0
    @Volatile private var karooFtp = 0
    /** Peso del perfil del Karoo (kg). Fallback 70 si no disponible. */
    @Volatile private var karooWeightKg = 70f
    /** Índice rotatorio de la despensa para la próxima pieza a sugerir. */
    private var nextFoodIndex = 0

    // ── Energía ────────────────────────────────────────────────────────────────
    /** kJ de trabajo acumulados (≈ kcal gastadas). */
    @Volatile var kjBurned = 0.0; private set

    // ── Objetivos acumulados (lo que "deberías" haber tomado a este ritmo) ─────
    private var carbsTargetG = 0.0
    private var hydrationTargetMl = 0.0

    // ── Consumido (raciones disparadas) ────────────────────────────────────────
    @Volatile var totalCarbsG = 0; private set
    @Volatile var totalHydrationMl = 0; private set

    // ── Demanda actual (g/h, ml/h) según la potencia efectiva ──────────────────
    @Volatile private var carbDemandGh = 0.0
    @Volatile private var hydrationDemandMlh = 0.0

    // ── Media de potencia de los últimos 20 min (ring buffer de 1200 muestras a
    //    1 Hz). Suma corrida para O(1). Refleja el esfuerzo RECIENTE, que marca
    //    la tasa de vaciado de glucógeno ahora. ────────────────────────────────
    private val POWER_WINDOW_SECS = 1200            // 20 min
    private val powerWindow = IntArray(POWER_WINDOW_SECS)
    private var powerWindowIdx = 0
    private var powerWindowCount = 0
    private var powerWindowSum = 0L

    // ── Media de potencia de TODA la actividad (contexto del esfuerzo global) ──
    private var totalPowerSum = 0L
    private var elapsedSecs = 0L

    // Peso de lo reciente (20 min) frente al total en la potencia efectiva.
    private val RECENT_WEIGHT = 0.75

    /**
     * True en cuanto el ciclista ha pedaleado de verdad en esta actividad.
     * A partir de ahí la demanda nunca baja del suelo (aunque ahora no haya vatios,
     * p. ej. en una bajada): el cuerpo sigue vaciado. Solo se muestra "--:--" antes
     * de empezar a pedalear o en pausa.
     */
    @Volatile private var hasStartedEffort = false

    /** Crédito de carbos previos (g): retrasa la primera comida y reduce el total. */
    private var preRideCreditG = 0.0
    /** Crédito en kJ del glucógeno extra previo: retrasa el uplift de vaciado. */
    private var preRideKjCredit = 0.0
    /** ml de mezcla con carbos que quedan en el bidón (volumen × nº de cargas). */
    private var carbyMlRemaining = 0

    // ── Alerta activa ──────────────────────────────────────────────────────────
    @Volatile var activeAlert: FuelAlert? = null; private set
    @Volatile var alertRemainingSecs = 0; private set

    private val consumerIds = mutableListOf<String>()

    /** Referencia al sistema Karoo, para suscribir/soltar la potencia bajo demanda. */
    private var karoo: KarooSystemService? = null
    /** ID del consumidor de potencia (solo activo durante la grabación). */
    private var powerConsumerId: String? = null
    /** ID del consumidor de temperatura ambiente (solo activo al grabar). */
    private var tempConsumerId: String? = null
    /** Temperatura ambiente (°C). Por defecto 20 (factor 1.0) si no hay sensor. */
    @Volatile private var karooTempC = 20.0

    // =========================================================================
    // Conexión a Karoo
    // =========================================================================

    fun connect(karooSystem: KarooSystemService) {
        karoo = karooSystem
        // Estos consumidores son baratos (basados en eventos) y siempre activos.
        consumerIds += karooSystem.addConsumer { state: RideState ->
            when (state) {
                is RideState.Recording -> {
                    paused = false
                    if (!rideActive) { rideActive = true; onRideStarted() }
                    subscribePower()      // solo se escucha potencia al grabar
                    subscribeTemp()
                }
                is RideState.Paused -> paused = true   // congela cuentas atrás y energía
                is RideState.Idle -> {
                    rideActive = false; paused = false
                    unsubscribePower()    // sin actividad, se suelta la potencia
                    unsubscribeTemp()
                }
            }
        }
        consumerIds += karooSystem.addConsumer { profile: UserProfile ->
            karooFtp = profile.ftp
            // Peso del perfil. Se asume kg (lo más habitual en karoo-ext).
            // Guarda defensiva por si llegara en gramos o libras.
            var w = profile.weight
            if (w > 300f) w /= 1000f          // venía en gramos
            else if (w in 150f..300f) w *= 0.4536f  // venía en libras
            karooWeightKg = w.coerceIn(40f, 150f)
        }
    }

    /** Suscribe el streaming de potencia (solo mientras se graba). */
    private fun subscribePower() {
        if (powerConsumerId != null) return
        val ks = karoo ?: return
        powerConsumerId = ks.addConsumer(
            OnStreamState.StartStreaming(DataType.Type.POWER)
        ) { event: OnStreamState ->
            val s = event.state
            if (s is StreamState.Streaming) {
                currentPowerWatts = (s.dataPoint.singleValue ?: 0.0).toInt().coerceAtLeast(0)
            }
        }
    }

    /** Suelta el streaming de potencia al terminar la actividad. */
    private fun unsubscribePower() {
        powerConsumerId?.let { karoo?.removeConsumer(it) }
        powerConsumerId = null
        currentPowerWatts = 0
    }

    /**
     * Suscribe la temperatura ambiente del Karoo (solo al grabar).
     *
     * OJO: la constante DataType.Type.TEMPERATURE es mi mejor apuesta del nombre
     * en el SDK. Si Android Studio la marca como no resuelta al compilar, mándame
     * las opciones que te ofrezca el autocompletado (DataType.Type.) y la ajusto.
     * Si el sensor no emite, se queda en 20 °C (factor 1.0), sin romper nada.
     */
    private fun subscribeTemp() {
        if (tempConsumerId != null) return
        val ks = karoo ?: return
        tempConsumerId = ks.addConsumer(
            OnStreamState.StartStreaming(DataType.Type.TEMPERATURE)
        ) { event: OnStreamState ->
            val s = event.state
            if (s is StreamState.Streaming) {
                s.dataPoint.singleValue?.let { karooTempC = it }
            }
        }
    }

    /** Suelta el streaming de temperatura al terminar la actividad. */
    private fun unsubscribeTemp() {
        tempConsumerId?.let { karoo?.removeConsumer(it) }
        tempConsumerId = null
        karooTempC = 20.0
    }

    fun disconnect(karooSystem: KarooSystemService) {
        unsubscribePower()
        unsubscribeTemp()
        consumerIds.forEach { karooSystem.removeConsumer(it) }
        consumerIds.clear()
        karoo = null
    }

    private fun onRideStarted() {
        config = configRepo.load()
        kjBurned = 0.0
        // Arranque proactivo: se siembra media ración en el objetivo para que la
        // PRIMERA comida llegue antes (no esperar a acumular un gel entero de
        // déficit desde cero). Solo adelanta la 1ª: la cadencia estable no cambia.
        val firstFoodG = config.foodItems.firstOrNull()?.carbsG ?: 30
        carbsTargetG = firstFoodG * 0.5
        hydrationTargetMl = 0.0
        totalCarbsG = 0; totalHydrationMl = 0
        activeAlert = null; alertRemainingSecs = 0
        nextFoodIndex = 0
        powerWindowIdx = 0; powerWindowCount = 0; powerWindowSum = 0L
        totalPowerSum = 0L; elapsedSecs = 0L
        carbDemandGh = 0.0; hydrationDemandMlh = 0.0
        hasStartedEffort = false
        // Carbos ingeridos antes: disponibles efectivos según horas transcurridas.
        val avail = FuelCalculator.preRideAvailableCarbsG(
            config.preRideCarbsG, config.preRideHoursBefore
        )
        preRideCreditG = avail
        // Crédito en kJ de TRABAJO (mismo sistema que kjBurned), no metabólico.
        preRideKjCredit = avail * FuelCalculator.WORK_KJ_PER_CARB_G
        // Presupuesto de mezcla con carbos: volumen del bidón × nº de cargas.
        carbyMlRemaining = config.bottle.volumeMl * config.bottle.carbBottleRefills.coerceAtLeast(1)
        Log.d(TAG, "Ride started, energy counters reset")
    }

    // =========================================================================
    // Tick energético (1 Hz, llamado por el campo de datos)
    // =========================================================================

    fun tick() {
        if (!rideActive || paused) return   // pausa: no avanza nada

        val ftp = if (config.useKarooFtp && karooFtp > 0) karooFtp else config.ftpWatts
        val power = currentPowerWatts

        // 1) Integrar energía total (kJ) con la potencia real
        kjBurned += FuelCalculator.kjFromPower(power, 1)

        // 2) Media de potencia de los últimos 20 min (ring buffer, suma corrida O(1))
        if (powerWindowCount == powerWindow.size) {
            powerWindowSum -= powerWindow[powerWindowIdx]   // sale la muestra más vieja
        } else {
            powerWindowCount++
        }
        powerWindow[powerWindowIdx] = power
        powerWindowSum += power
        powerWindowIdx = (powerWindowIdx + 1) % powerWindow.size
        val power20min = if (powerWindowCount > 0) powerWindowSum.toDouble() / powerWindowCount else 0.0

        // 2b) Media de potencia de TODA la actividad (contexto del esfuerzo global)
        totalPowerSum += power
        elapsedSecs += 1
        val powerCum = if (elapsedSecs > 0) totalPowerSum.toDouble() / elapsedSecs else 0.0

        // 2c) Potencia efectiva: lo reciente (20 min) domina, el total aporta contexto
        val effectivePower = RECENT_WEIGHT * power20min + (1 - RECENT_WEIGHT) * powerCum

        // En cuanto se pedalea de verdad, queda "actividad empezada".
        if (power > 0) hasStartedEffort = true

        // 3) Demanda según intensidad efectiva + vaciado de glucógeno. El crédito
        //    de glucógeno previo retrasa el uplift (depósito más lleno al empezar).
        val upliftKj = (kjBurned - preRideKjCredit).coerceAtLeast(0.0)
        carbDemandGh = FuelCalculator.carbDemandGh(effectivePower, ftp, upliftKj)
        hydrationDemandMlh = FuelCalculator.hydrationDemandMlh(effectivePower, ftp, karooWeightKg, karooTempC)

        // Tras empezar a pedalear, la demanda nunca baja del suelo aunque ahora no
        // haya vatios (bajando un puerto tras subirlo): el cuerpo sigue vaciado.
        // Solo queda en 0 (→ "--:--") antes del primer pedaleo o en pausa/parado.
        if (hasStartedEffort && ftp > 0) {
            carbDemandGh = carbDemandGh.coerceAtLeast(FuelCalculator.carbFloorGh(upliftKj))
            hydrationDemandMlh = hydrationDemandMlh.coerceAtLeast(
                FuelCalculator.hydrationFloorMlh(karooWeightKg)
            )
        }

        carbsTargetG += carbDemandGh / 3600.0
        hydrationTargetMl += hydrationDemandMlh / 3600.0

        // 3) ¿Toca beber/comer? Raciones según la despensa y el gasto
        if (ftp > 0) {
            if (hydrationTargetMl - totalHydrationMl >= drinkServingMl()) {
                fireDrink()
            }
            if (kjBurned > 0 && carbDeficitG() >= nextFood().carbsG) {
                fireEat()
            }
        }

        // 4) Caducidad de la alerta a los 30 s
        if (alertRemainingSecs > 0) {
            alertRemainingSecs--
            if (alertRemainingSecs == 0) activeAlert = null
        }
    }

    private fun fireDrink() {
        val ml = drinkServingMl()
        // Los carbos solo salen mientras quede mezcla en el bidón. Al agotar el
        // volumen con carbos, los siguientes tragos (agua) aportan 0 g.
        val carbyMl = minOf(ml, carbyMlRemaining)
        val carbsFromDrink = Math.round(carbyMl * config.bottle.carbConcentration)
        carbyMlRemaining -= carbyMl
        totalHydrationMl += ml
        totalCarbsG += carbsFromDrink
        setAlert(
            if (activeAlert?.type == FuelAlert.AlertType.EAT)
                FuelAlert(FuelAlert.AlertType.DRINK_AND_EAT, ml, activeAlert!!.carbsToEatG)
            else
                FuelAlert(FuelAlert.AlertType.DRINK, mlToDrink = ml)
        )
    }

    private fun fireEat() {
        val food = nextFood()
        val g = food.carbsG
        totalCarbsG += g
        nextFoodIndex++   // rota a la siguiente pieza
        setAlert(
            if (activeAlert?.type == FuelAlert.AlertType.DRINK)
                FuelAlert(FuelAlert.AlertType.DRINK_AND_EAT, activeAlert!!.mlToDrink, g, food.name)
            else
                FuelAlert(FuelAlert.AlertType.EAT, carbsToEatG = g, foodName = food.name)
        )
    }

    private fun setAlert(alert: FuelAlert) {
        activeAlert = alert
        alertRemainingSecs = ALERT_DISPLAY_SECS
        playBeeps(alert.type)
    }

    // =========================================================================
    // Estimaciones para el campo (segundos hasta la próxima alerta)
    // =========================================================================

    /**
     * Segundos estimados hasta la próxima alerta de bebida (-1 = sin datos).
     * Usa la demanda ACTUAL (potencia de los últimos 30 s): si subes el ritmo de
     * verdad, la cuenta atrás se acorta; si aflojas, se alarga. Sin bandazos de 1 s.
     */
    fun secsToNextDrink(): Int {
        val ratePerSec = hydrationDemandMlh / 3600.0
        if (ratePerSec <= 0.0) return -1
        val remaining = drinkServingMl() - (hydrationTargetMl - totalHydrationMl)
        return (remaining / ratePerSec).coerceAtLeast(0.0).toInt()
    }

    /**
     * Segundos estimados hasta la próxima alerta de comida (-1 = sin datos).
     * Usa la demanda ACTUAL de carbos (potencia de los últimos 30 s).
     */
    fun secsToNextEat(): Int {
        val ratePerSec = carbDemandGh / 3600.0
        if (ratePerSec <= 0.0) return -1
        val remaining = nextFood().carbsG - carbDeficitG()
        return (remaining / ratePerSec).coerceAtLeast(0.0).toInt()
    }

    // =========================================================================
    // Raciones según la despensa y el gasto (NO configurables como números)
    // =========================================================================

    /** Próxima pieza de comida a sugerir (rota por la despensa). */
    private fun nextFood(): FoodItem {
        val items = config.foodItems
        return if (items.isEmpty()) FoodItem("", 30)
        else items[nextFoodIndex % items.size]
    }

    /**
     * Ración de bebida (ml) por toma: buche FIJO (~150 ml). Al ser fijo, el
     * intervalo entre tragos varía con la demanda: a más sudor (intensidad,
     * calor), más pronto el aviso. Antes era demanda/4, que daba siempre ~15 min.
     */
    fun drinkServingMl(): Int = SIP_ML

    /** Etiqueta de la alerta de bebida activa (ej. "190 ml"). */
    fun drinkAlertLabel(): String {
        val a = activeAlert ?: return ""
        return "${a.mlToDrink} ml"
    }

    /** Etiqueta de la alerta de comida activa (ej. "Gel 45 g" o "45 g"). */
    fun eatAlertLabel(): String {
        val a = activeAlert ?: return ""
        return if (a.foodName.isNotBlank()) "${a.foodName} ${a.carbsToEatG}g"
        else "${a.carbsToEatG} g"
    }

    // =========================================================================
    // Relleno del "depósito" (1.0 = lleno tras ingerir, 0.0 = toca alerta)
    // =========================================================================

    /** Fracción 0..1 del depósito de bebida (lleno tras beber, se vacía hacia 0). */
    fun drinkFillFraction(): Float {
        val serving = drinkServingMl().toDouble()
        val deficit = hydrationTargetMl - totalHydrationMl
        return ((serving - deficit) / serving).coerceIn(0.0, 1.0).toFloat()
    }

    /**
     * Déficit de carbos pendiente de reponer (g), descontando el crédito de los
     * carbos ingeridos antes de la actividad (que dan un margen inicial).
     */
    private fun carbDeficitG(): Double =
        carbsTargetG - totalCarbsG - preRideCreditG

    /** Fracción 0..1 del depósito de comida (lleno tras comer, se vacía hacia 0). */
    fun eatFillFraction(): Float {
        val serving = nextFood().carbsG.toDouble()
        val deficit = carbDeficitG()
        return ((serving - deficit) / serving).coerceIn(0.0, 1.0).toFloat()
    }

    /** ¿Hay alerta de bebida activa (parpadeo) ahora mismo? */
    fun isDrinkAlerting(): Boolean =
        alertRemainingSecs > 0 && (activeAlert?.type == FuelAlert.AlertType.DRINK ||
            activeAlert?.type == FuelAlert.AlertType.DRINK_AND_EAT)

    /** ¿Hay alerta de comida activa (parpadeo) ahora mismo? */
    fun isEatAlerting(): Boolean =
        alertRemainingSecs > 0 && (activeAlert?.type == FuelAlert.AlertType.EAT ||
            activeAlert?.type == FuelAlert.AlertType.DRINK_AND_EAT)

    // =========================================================================
    // Audio
    // =========================================================================

    /**
     * Pitido por el ALTAVOZ INTERNO del Karoo (PlayBeepPattern del SDK).
     * Estilo "Arcade 8-bit" (tipo moneda de videojuego), tonos que suben:
     *   - Bebida: 1976 → 2637 Hz (corto + largo)
     *   - Comida: 1568 → 2093 → 2637 Hz (dos cortos + largo)
     *   - Ambos: la de bebida, pausa, y la de comida
     */
    private fun playBeeps(type: FuelAlert.AlertType) {
        // Cada tono = (frecuencia Hz, duración ms)
        val drink = listOf(1976 to 110, 2637 to 280)
        val eat = listOf(1568 to 110, 2093 to 110, 2637 to 320)
        val notes: List<Pair<Int, Int>> = when (type) {
            FuelAlert.AlertType.DRINK -> drink
            FuelAlert.AlertType.EAT -> eat
            FuelAlert.AlertType.DRINK_AND_EAT -> drink + listOf(0 to 210) + eat  // 0 = pausa
        }
        val tones = mutableListOf<PlayBeepPattern.Tone>()
        notes.forEachIndexed { i, (freq, dur) ->
            if (i > 0) tones.add(PlayBeepPattern.Tone(null, 90))   // hueco entre tonos
            if (freq == 0) tones.add(PlayBeepPattern.Tone(null, dur))  // pausa separadora (ambos)
            else tones.add(PlayBeepPattern.Tone(freq, dur))
        }
        karoo?.dispatch(PlayBeepPattern(tones))
    }
}
