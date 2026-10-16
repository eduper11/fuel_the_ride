package com.fueltheride.ui

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.fueltheride.R
import com.fueltheride.data.ConfigRepository
import com.fueltheride.databinding.ActivityConfigBinding
import com.fueltheride.model.BottleConfig
import com.fueltheride.model.FoodItem
import com.fueltheride.model.FuelConfig
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.models.UserProfile

/**
 * ConfigActivity
 *
 * Shown when the user opens FuelTheRide from the Karoo app drawer
 * (outside of a ride). Here they configure:
 *   - FTP (manual or read from Karoo profile)
 *   - Weight
 *   - Bottle size (500 / 750 / custom)
 *   - Carbs in bottle
 *   - Salts flag
 *   - Carbs per solid intake
 *   - Drink interval (min)
 *   - Eat interval (min)
 */
class ConfigActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfigBinding
    private lateinit var repo: ConfigRepository
    private lateinit var karooSystem: KarooSystemService

    private var karooFtp: Int = 0
    private var profileConsumerId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repo = ConfigRepository(this)
        karooSystem = KarooSystemService(this)

        setupBottleSpinner()
        loadSavedValues()
        setupListeners()

        // Muestra FTP y peso del perfil del Karoo como referencia
        karooSystem.connect {
            profileConsumerId = karooSystem.addConsumer { profile: UserProfile ->
                karooFtp = profile.ftp
                runOnUiThread {
                    binding.tvKarooFtpHint.text =
                        "Perfil Karoo · FTP: ${karooFtp} W · Peso: ${profile.weight.toInt()} kg"
                    binding.tvKarooFtpHint.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onStop() {
        profileConsumerId?.let { karooSystem.removeConsumer(it) }
        karooSystem.disconnect()
        super.onStop()
    }

    // =========================================================================
    // Setup
    // =========================================================================

    private fun setupBottleSpinner() {
        val options = listOf("500 ml", "750 ml", "Custom")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerBottleSize.adapter = adapter

        binding.spinnerBottleSize.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?, view: View?, position: Int, id: Long
                ) {
                    binding.tilCustomBottle.visibility =
                        if (position == 2) View.VISIBLE else View.GONE
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
    }

    private fun loadSavedValues() {
        val config = repo.load()

        binding.etFtp.setText(if (config.ftpWatts > 0) config.ftpWatts.toString() else "")
        binding.switchUseKarooFtp.isChecked = config.useKarooFtp
        updateFtpFieldVisibility(config.useKarooFtp)

        // Bottle size spinner
        val spinnerPos = when (config.bottle.volumeMl) {
            500 -> 0
            750 -> 1
            else -> 2
        }
        binding.spinnerBottleSize.setSelection(spinnerPos)
        if (spinnerPos == 2) {
            binding.etCustomBottle.setText(config.bottle.volumeMl.toString())
        }

        binding.etBottleCarbs.setText(config.bottle.carbsGrams.toString())
        binding.etBottleRefills.setText(config.bottle.carbBottleRefills.toString())
        binding.switchBottleSalts.isChecked = config.bottle.hasSalts

        // Antes de la actividad
        if (config.preRideCarbsG > 0) binding.etPrerideCarbs.setText(config.preRideCarbsG.toString())
        binding.etPrerideHours.setText(config.preRideHoursBefore.toString())

        // Despensa: rellena hasta 4 filas con lo guardado
        val nameFields = listOf(
            binding.etFoodName1, binding.etFoodName2, binding.etFoodName3, binding.etFoodName4
        )
        val carbFields = listOf(
            binding.etFoodCarbs1, binding.etFoodCarbs2, binding.etFoodCarbs3, binding.etFoodCarbs4
        )
        config.foodItems.take(4).forEachIndexed { i, item ->
            nameFields[i].setText(item.name)
            carbFields[i].setText(item.carbsG.toString())
        }
    }

    private fun setupListeners() {
        binding.switchUseKarooFtp.setOnCheckedChangeListener { _, isChecked ->
            updateFtpFieldVisibility(isChecked)
        }

        binding.btnSave.setOnClickListener {
            saveConfig()
        }
    }

    private fun updateFtpFieldVisibility(useKaroo: Boolean) {
        binding.tilFtp.visibility = if (useKaroo) View.GONE else View.VISIBLE
    }

    // =========================================================================
    // Save
    // =========================================================================

    private fun saveConfig() {
        val useKarooFtp = binding.switchUseKarooFtp.isChecked

        val ftpWatts = if (useKarooFtp) {
            0
        } else {
            binding.etFtp.text.toString().toIntOrNull() ?: run {
                binding.etFtp.error = "Enter a valid FTP"
                return
            }
        }

        val bottleVolumeMl = when (binding.spinnerBottleSize.selectedItemPosition) {
            0 -> 500
            1 -> 750
            else -> binding.etCustomBottle.text.toString().toIntOrNull() ?: run {
                binding.etCustomBottle.error = "Enter bottle size"
                return
            }
        }

        val bottleCarbs = binding.etBottleCarbs.text.toString().toIntOrNull() ?: 0
        val bottleSalts = binding.switchBottleSalts.isChecked
        val bottleRefills = binding.etBottleRefills.text.toString().toIntOrNull()?.coerceAtLeast(1) ?: 1

        // Despensa: lee las 4 filas, ignora las vacías
        val nameFields = listOf(
            binding.etFoodName1, binding.etFoodName2, binding.etFoodName3, binding.etFoodName4
        )
        val carbFields = listOf(
            binding.etFoodCarbs1, binding.etFoodCarbs2, binding.etFoodCarbs3, binding.etFoodCarbs4
        )
        val foodItems = nameFields.indices.mapNotNull { i ->
            val name = nameFields[i].text.toString().trim()
            val carbs = carbFields[i].text.toString().toIntOrNull()
            if (name.isNotEmpty() && carbs != null && carbs > 0) FoodItem(name, carbs) else null
        }
        if (foodItems.isEmpty()) {
            binding.etFoodName1.error = "Define al menos una comida"
            return
        }

        val preRideCarbs = binding.etPrerideCarbs.text.toString().toIntOrNull() ?: 0
        val preRideHours = binding.etPrerideHours.text.toString().toFloatOrNull() ?: 1f

        val config = FuelConfig(
            ftpWatts = ftpWatts,
            useKarooFtp = useKarooFtp,
            bottle = BottleConfig(
                volumeMl = bottleVolumeMl,
                carbsGrams = bottleCarbs,
                hasSalts = bottleSalts,
                carbBottleRefills = bottleRefills
            ),
            foodItems = foodItems,
            preRideCarbsG = preRideCarbs,
            preRideHoursBefore = preRideHours
        )

        repo.save(config)
        Toast.makeText(this, getString(R.string.config_saved), Toast.LENGTH_SHORT).show()
        finish()
    }
}
