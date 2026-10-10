package mg.radarreseau

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.graphics.Color
import android.graphics.Typeface
import android.content.Intent
import android.provider.Settings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var signal: TextView
    private lateinit var wifi: TextView
    private lateinit var network: TextView
    private lateinit var locationText: TextView
    private lateinit var quality: TextView
    private lateinit var spots: TextView
    private lateinit var telephony: TelephonyManager
    private lateinit var locationManager: LocationManager
    private var lastLocation: Location? = null
    private val handler = Handler(Looper.getMainLooper())
    private val prefs by lazy { getSharedPreferences("spots", Context.MODE_PRIVATE) }
    private val permissionRequest = 41

    private val refresh = object : Runnable {
        override fun run() {
            refreshSignal()
            handler.postDelayed(this, 2500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        telephony = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        buildUi()
        askPermissionsIfNeeded()
        handler.post(refresh)
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 26, 22, 18)
            setBackgroundColor(Color.rgb(10, 17, 31))
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val title = TextView(this).apply {
            text = "◉ RADAR RÉSEAU"
            textSize = 25f; setTextColor(Color.WHITE); typeface = Typeface.DEFAULT_BOLD
        }
        status = label("Analyse locale • actualisation toutes les 2,5 s", 13f, 0xFF9BAAC4.toInt())
        signal = label("Signal mobile : mesure en attente…", 21f, Color.WHITE)
        wifi = label("Wi‑Fi : mesure en attente…", 17f, Color.WHITE)
        network = label("Réseau : —", 15f, 0xFFB8C7E0.toInt())
        quality = label("QUALITÉ : —", 24f, 0xFF62E6A7.toInt())
        locationText = label("Position : autorisation nécessaire", 14f, 0xFFB8C7E0.toInt())
        val note = label("Marche lentement dans la zone : observe le signal, puis enregistre les endroits où il est meilleur.", 14f, 0xFF9BAAC4.toInt())
        val save = Button(this).apply { text = "＋ Enregistrer cet endroit" }
        save.setOnClickListener { saveSpot() }
        val clear = Button(this).apply { text = "Effacer les endroits enregistrés" }
        clear.setOnClickListener {
            prefs.edit().remove("list").apply()
            showSpots()
        }
        val settings = Button(this).apply { text = "Ouvrir les réglages réseau" }
        settings.setOnClickListener {
            try { startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS)) } catch (_: Exception) {}
        }
        spots = label("MES MEILLEURS ENDROITS\nAucun endroit enregistré pour le moment.", 14f, Color.WHITE)
        listOf(title, status, signal, wifi, network, quality, locationText, note, save, clear, settings, spots).forEach {
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            params.bottomMargin = 16
            content.addView(it, params)
        }
        scroll.addView(content)
        root.addView(scroll)
        setContentView(root)
    }

    private fun label(textValue: String, size: Float, color: Int) = TextView(this).apply {
        text = textValue; textSize = size; setTextColor(color)
    }

    private fun askPermissionsIfNeeded() {
        val needed = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_PHONE_STATE
        ).filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (needed.isNotEmpty()) requestPermissions(needed.toTypedArray(), permissionRequest)
        else startLocation()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (requestCode == permissionRequest) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) startLocation()
            refreshSignal()
        }
    }

    private fun startLocation() {
        try {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
            val providers = locationManager.getProviders(true)
            for (provider in providers) {
                try {
                    locationManager.requestLocationUpdates(provider, 2000L, 1f, object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            lastLocation = location
                            locationText.text = "Position : %.5f, %.5f (précision ~%dm)".format(
                                Locale.US, location.latitude, location.longitude, location.accuracy.toInt())
                        }
                    })
                    val last = locationManager.getLastKnownLocation(provider)
                    if (last != null && (lastLocation == null || last.time > lastLocation!!.time)) lastLocation = last
                } catch (_: SecurityException) {}
            }
        } catch (_: Exception) {}
    }

    private fun refreshSignal() {
        var dbm: Int? = null
        var type = "Mobile"
        try {
            if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                val ss = telephony.signalStrength
                if (ss != null) {
                    val cells = ss.cellSignalStrengths
                    val usable = cells.map { it.dbm }.filter { it in -150..-1 }
                    if (usable.isNotEmpty()) dbm = usable.maxOrNull()
                }
                type = when (telephony.dataNetworkType) {
                    20 -> "5G"
                    13 -> "4G / LTE"
                    19 -> "4G"
                    3, 8, 9, 10, 15 -> "3G"
                    1, 2, 4, 7, 11, 16 -> "2G"
                    else -> "Mobile"
                }
            }
        } catch (_: Exception) {}
        val wifiDbm = try {
            val wm = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                val info = wm.connectionInfo
                if (info != null && info.networkId != -1) info.rssi else null
            } else null
        } catch (_: Exception) { null }

        signal.text = if (dbm != null) "Signal mobile : $dbm dBm" else "Signal mobile : indisponible (SIM/autorisation)"
        wifi.text = if (wifiDbm != null) "Wi‑Fi connecté : $wifiDbm dBm" else "Wi‑Fi : non connecté ou mesure bloquée"
        network.text = "Réseau mobile : $type"
        val strength = dbm ?: wifiDbm
        val score = when {
            strength == null -> null
            strength >= -75 -> "EXCELLENT"
            strength >= -90 -> "BON"
            strength >= -105 -> "MOYEN"
            else -> "FAIBLE"
        }
        quality.text = "QUALITÉ : ${score ?: "INCONNUE"}"
        quality.setTextColor(when(score) {
            "EXCELLENT" -> 0xFF62E6A7.toInt()
            "BON" -> 0xFFB8F06A.toInt()
            "MOYEN" -> 0xFFFFC857.toInt()
            "FAIBLE" -> 0xFFFF7474.toInt()
            else -> Color.WHITE
        })
        status.text = "Mesure en direct • ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}"
    }

    private fun saveSpot() {
        val location = lastLocation
        val line = if (location != null) {
            val signalText = signal.text.toString().substringAfter(": ").trim()
            val wifiText = wifi.text.toString().substringAfter(": ").trim()
            val stamp = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date())
            "$stamp • GPS %.5f, %.5f • Mobile $signalText • Wi‑Fi $wifiText".format(Locale.US, location.latitude, location.longitude)
        } else {
            "Position GPS indisponible • ${SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date())} • ${signal.text} • ${wifi.text}"
        }
        val old = prefs.getString("list", "") ?: ""
        prefs.edit().putString("list", (line + "\n" + old).trim()).apply()
        showSpots()
    }

    private fun showSpots() {
        val saved = prefs.getString("list", "") ?: ""
        spots.text = "MES MEILLEURS ENDROITS\n" + if (saved.isBlank()) "Aucun endroit enregistré pour le moment." else saved
    }

    override fun onDestroy() {
        handler.removeCallbacks(refresh)
        try { locationManager.removeUpdates { } } catch (_: Exception) {}
        super.onDestroy()
    }
}
