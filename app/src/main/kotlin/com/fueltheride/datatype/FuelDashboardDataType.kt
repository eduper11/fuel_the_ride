package com.fueltheride.datatype

import android.content.Context
import android.widget.RemoteViews
import com.fueltheride.R
import com.fueltheride.engine.FuelEngine
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UpdateGraphicConfig
import io.hammerhead.karooext.models.ViewConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * FuelDashboardDataType — campo de datos gráfico.
 *
 * Dos mitades (💧 bebida azul / 🍌 comida verde). Cada una:
 *  - "Depósito" de fondo que se vacía de arriba a abajo según se acerca la
 *    siguiente ingesta (ClipDrawable + setImageLevel, 0..10000).
 *  - Cuenta atrás grande encima.
 *
 * Durante la alerta de 30s, la mitad que toca PARPADEA alternando (cada 500 ms)
 * la cuenta atrás siguiente y la cantidad a ingerir ("150 ml" / "40 g").
 *
 * Render cada 500 ms (para el parpadeo); el motor integra energía 1 vez/segundo.
 */
class FuelDashboardDataType(
    private val applicationContext: Context,
    private val engine: FuelEngine
) : DataTypeImpl("fueltheride", "fuel_dashboard") {

    private val scope = CoroutineScope(Dispatchers.Default)

    override fun startView(
        context: Context,
        config: ViewConfig,
        emitter: ViewEmitter
    ) {
        emitter.onNext(UpdateGraphicConfig(showHeader = false))

        // Editor de pantallas: ciclo de demostración animado en bucle.
        if (config.preview) {
            val job: Job = scope.launch { runPreviewCycle(emitter) }
            emitter.setCancellable { job.cancel() }
            return
        }

        // En vivo: cadencia adaptativa para ahorrar batería.
        //   - Alerta activa (parpadeo): 500 ms
        //   - Grabando sin alerta: 1 s (la cuenta atrás cambia cada segundo)
        //   - Parado/sin actividad: 2 s (el campo solo muestra "--:--")
        // El motor avanza exactamente 1 vez/segundo, medido por reloj real,
        // independientemente de cada cuánto se repinte.
        val job: Job = scope.launch {
            var lastTickMs = 0L
            while (isActive) {
                val now = android.os.SystemClock.elapsedRealtime()
                if (now - lastTickMs >= 1000L) {
                    engine.tick()
                    lastTickMs = now
                }
                val blinkOn = (now / 500L) % 2L == 0L
                emitter.updateView(buildLiveView(blinkOn))

                val nextDelay = when {
                    engine.alertRemainingSecs > 0 -> 500L                 // parpadeo
                    engine.rideActive && !engine.paused -> 1000L          // grabando
                    else -> 2000L                                         // parado
                }
                delay(nextDelay)
            }
        }
        emitter.setCancellable { job.cancel() }
    }

    override fun startStream(emitter: Emitter<StreamState>) {
        val job = scope.launch {
            while (isActive) {
                emitter.onNext(
                    StreamState.Streaming(
                        DataPoint(
                            dataTypeId = dataTypeId,
                            values = mapOf(DataType.Field.SINGLE to engine.totalCarbsG.toDouble())
                        )
                    )
                )
                // Grabando: 1 s (para el registro FIT). Parado: 5 s (apenas cambia).
                delay(if (engine.rideActive && !engine.paused) 1_000L else 5_000L)
            }
        }
        emitter.setCancellable { job.cancel() }
    }

    // =========================================================================
    // Render
    // =========================================================================

    private fun buildLiveView(blinkOn: Boolean): RemoteViews {
        val v = RemoteViews(applicationContext.packageName, R.layout.view_field_normal)

        // Rellenos (0..10000)
        v.setInt(R.id.iv_drink_fill, "setImageLevel", (engine.drinkFillFraction() * 10000).toInt())
        v.setInt(R.id.iv_eat_fill, "setImageLevel", (engine.eatFillFraction() * 10000).toInt())

        // Texto BEBIDA
        v.setTextViewText(R.id.tv_drink_main, when {
            engine.isDrinkAlerting() && blinkOn -> engine.drinkAlertLabel()
            engine.rideActive -> estToMmss(engine.secsToNextDrink())
            else -> "--:--"
        })

        // Texto COMIDA
        v.setTextViewText(R.id.tv_eat_main, when {
            engine.isEatAlerting() && blinkOn -> engine.eatAlertLabel()
            engine.rideActive -> estToMmss(engine.secsToNextEat())
            else -> "--:--"
        })

        return v
    }

    /**
     * Ciclo de demostración para el editor (config.preview = true).
     *
     * Muestra un ciclo completo en bucle para que el usuario vea cómo funciona:
     *  1) La bebida cuenta atrás desde 0:08 hacia 0, su depósito se vacía.
     *  2) La comida va más lenta (depósito más lleno), de fondo.
     *  3) Al llegar la bebida a 0 → alerta 4 s parpadeando "150 ml".
     *  4) El depósito de bebida se rellena y vuelve a empezar.
     *
     * Tiempos comprimidos (no son los reales) solo para la demo visual.
     */
    private suspend fun runPreviewCycle(emitter: ViewEmitter) {
        val drinkCountFrom = 8      // segundos de cuenta atrás (demo)
        val alertSecs = 4          // duración de la alerta en la demo

        // delay() es cancelable: al cerrar la preview, lanza y corta el bucle.
        while (true) {
            // ── Fase 1: cuenta atrás de bebida bajando ──
            for (sec in drinkCountFrom downTo 1) {
                // Comida: más lenta, va de 0:18 bajando despacio
                val eatSec = 18 - (drinkCountFrom - sec) / 2
                val drinkFrac = sec.toFloat() / drinkCountFrom
                val eatFrac = (eatSec.toFloat() / 18f).coerceIn(0f, 1f)
                repeat(2) { // dos medios-ticks (500 ms) por segundo
                    emitter.updateView(
                        buildPreviewFrame(
                            drinkText = mmss(sec),
                            eatText = mmss(eatSec),
                            drinkFrac = drinkFrac,
                            eatFrac = eatFrac
                        )
                    )
                    delay(500L)
                }
            }

            // ── Fase 2: alerta de bebida parpadeando "150 ml" ──
            for (sec in 0 until alertSecs) {
                val blinkOn = sec % 2 == 0
                emitter.updateView(
                    buildPreviewFrame(
                        // tras disparar, la cuenta atrás siguiente ya reiniciada
                        drinkText = if (blinkOn) "150 ml" else mmss(drinkCountFrom),
                        eatText = "0:10",
                        drinkFrac = 1f,        // depósito recién rellenado
                        eatFrac = 0.55f
                    )
                )
                delay(500L)
            }
        }
    }

    /** Construye un fotograma de la demo con valores dados. */
    private fun buildPreviewFrame(
        drinkText: String,
        eatText: String,
        drinkFrac: Float,
        eatFrac: Float
    ): RemoteViews {
        val v = RemoteViews(applicationContext.packageName, R.layout.view_field_normal)
        v.setInt(R.id.iv_drink_fill, "setImageLevel", (drinkFrac * 10000).toInt())
        v.setInt(R.id.iv_eat_fill, "setImageLevel", (eatFrac * 10000).toInt())
        v.setTextViewText(R.id.tv_drink_main, drinkText)
        v.setTextViewText(R.id.tv_eat_main, eatText)
        return v
    }

    /** MM:SS para la demo. */
    private fun mmss(totalSecs: Int): String {
        val m = totalSecs / 60
        val s = totalSecs % 60
        return "%d:%02d".format(m, s)
    }

    /** Formatea segundos como "MM:SS"; -1 (sin ritmo) → "--:--". */
    private fun estToMmss(totalSecs: Int): String {
        if (totalSecs < 0) return "--:--"
        val m = totalSecs / 60
        val s = totalSecs % 60
        return "%d:%02d".format(m, s)
    }
}
