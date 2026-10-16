package com.fueltheride.service

import android.util.Log
import com.fueltheride.datatype.FuelDashboardDataType
import com.fueltheride.engine.FuelEngine
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.extension.KarooExtension

/**
 * FuelExtensionService
 *
 * Servicio KarooExtension. Karoo OS se enlaza a él durante el entreno.
 *
 * ⚠️ El id de la extensión ("fueltheride") NO puede contener puntos.
 *
 * Responsabilidades:
 *  - Conectar KarooSystemService y el FuelEngine (que se suscribe a potencia,
 *    tiempo, estado del entreno y FTP del perfil).
 *  - Exponer el campo de datos en la propiedad `types`.
 *
 * El campo (FuelDashboardDataType) hace el resto: bucle 1Hz, render y sonido.
 */
class FuelExtensionService : KarooExtension("fueltheride", "1.0") {

    companion object {
        private const val TAG = "FuelExtensionService"
    }

    private lateinit var karooSystem: KarooSystemService
    private lateinit var engine: FuelEngine

    /**
     * Campos de datos que ofrece la extensión. El typeId debe coincidir
     * con el declarado en extension_info.xml.
     */
    override val types: List<DataTypeImpl> by lazy {
        listOf(FuelDashboardDataType(applicationContext, engine))
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "FuelExtensionService onCreate")
        engine = FuelEngine(applicationContext)
        karooSystem = KarooSystemService(applicationContext)
        karooSystem.connect { connected ->
            Log.d(TAG, "Karoo system connected=$connected")
            engine.connect(karooSystem)
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "FuelExtensionService onDestroy")
        engine.disconnect(karooSystem)
        karooSystem.disconnect()
        super.onDestroy()
    }
}
