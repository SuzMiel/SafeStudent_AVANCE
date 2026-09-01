package com.example.safestudent

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class AlertaActivity : AppCompatActivity() {

    private val CHANNEL_ID = "alertas_channel"
    private val PERMISSION_REQUEST_CODE = 1001
    private var numeroSeleccionado = "116"

    private lateinit var map: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val ctx = applicationContext
        Configuration.getInstance().userAgentValue = "SafeStudentTacnaApp/1.0"
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))

        setContentView(R.layout.activity_alerta)

        val toolbar = findViewById<Toolbar>(R.id.toolbarAlerta)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        map = findViewById(R.id.mapView)

        // Fuente de tiles gratuita de alta disponibilidad
        val osmTileSource = XYTileSource(
            "OSMFR",
            0, 19, 256, ".png",
            arrayOf(
                "https://a.tile.openstreetmap.fr/osmfr/",
                "https://b.tile.openstreetmap.fr/osmfr/",
                "https://c.tile.openstreetmap.fr/osmfr/"
            )
        )

        map.setTileSource(osmTileSource)
        map.setMultiTouchControls(true)

        val mapController = map.controller
        mapController.setZoom(18.5)

        // Punto de referencia principal (SENATI / Tacna)
        val puntoCercaSenati = GeoPoint(-18.03760570289263, -70.25071864765032)

        mapController.setCenter(puntoCercaSenati)

        val puntoPerezGamboa = GeoPoint(-18.038576240779836, -70.24918121126326)
        val markerPerezGamboa = Marker(map).apply {
            position = puntoPerezGamboa
            //icon = ContextCompat.getDrawable(this@AlertaActivity, R.drawable.ic_marcador_custom)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Plaza Eduardo Pérez Gamboa"
            snippet = "Zona de encuentro / Gregorio Albarracín"
        }
        map.overlays.add(markerPerezGamboa)

        crearCanalNotificaciones()
        verificarYSolicitarPermiso()

        // Acción directa al pulsar AVISOS
        findViewById<Button>(R.id.btnDemoAvisos).setOnClickListener {
            lanzarAvisoAleatorio()
        }

        // Acción directa al pulsar ALERTA
        findViewById<Button>(R.id.btnDemoAlerta).setOnClickListener {
            lanzarAlertaAleatoria()
        }

        val spCategoria = findViewById<Spinner>(R.id.spCategoria)
        val spContacto = findViewById<Spinner>(R.id.spContacto)
        val btnLlamar = findViewById<Button>(R.id.btnLlamarSeleccionado)

        val categorias = arrayOf("Emergencias", "Apoyo Estudiantil")
        val adapterCat = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categorias)
        spCategoria.adapter = adapterCat

        val emergenciasNombres = arrayOf("Bomberos (116)", "Policía (105)", "Seguridad Campus")
        val emergenciasNumeros = arrayOf("116", "105", "952000111")

        val apoyoNombres = arrayOf("Tutoría", "Secretaría", "Fondo Mi Futuro")
        val apoyoNumeros = arrayOf("952333444", "952555666", "952999000")

        spCategoria.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val subContactos = if (position == 0) emergenciasNombres else apoyoNombres
                val adapterSub = ArrayAdapter(this@AlertaActivity, android.R.layout.simple_spinner_dropdown_item, subContactos)
                spContacto.adapter = adapterSub
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spContacto.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val categoriaActual = spCategoria.selectedItemPosition
                numeroSeleccionado = if (categoriaActual == 0) {
                    emergenciasNumeros[position]
                } else {
                    apoyoNumeros[position]
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnLlamar.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$numeroSeleccionado")
            }
            startActivity(intent)
        }
    }

    private fun lanzarAvisoAleatorio() {
        val avisos = listOf(
            Pair(" Aniversario SENATI Tacna", "¡Te invitamos a las actividades académicas y deportivas este viernes!"),
            Pair(" Paro de Transportistas Tacna", "Comunicado: Clases virtuales del 7 al 9 de Septiembre por prevención."),
            Pair(" Prácticas Pre-Profesionales", "Recordatorio: Fecha límite para presentar la carta de presentación de empresa.")
        )
        val aviso = avisos.random()
        lanzarNotificacionHeadsUp(aviso.first, aviso.second)
    }

    private fun lanzarAlertaAleatoria() {
        val alertas = listOf(
            Pair(" ALERTA SÍSMICA DE EMERGENCIA", "Sismo detectado en Tacna. Mantenga la calma y diríjase a las zonas de seguridad mas cercanas."),
            Pair(" ALERTA METEOROLÓGICA", "Lluvias intensas detectadas. Resguarde equipos electrónicos y evite áreas descubiertas."),
            Pair(" ALERTA DE VIENTOS EXTREMOS", "Vientos fuertes en la zona. Evite estructuras frágiles y asegure ventanas en las aulas.")
        )
        val alerta = alertas.random()
        mostrarAlertaModalCentral(alerta.first, alerta.second)
    }

    private fun mostrarAlertaModalCentral(titulo: String, mensaje: String) {
        AlertDialog.Builder(this)
            .setTitle(titulo)
            .setMessage(mensaje)
            .setCancelable(false)
            .setPositiveButton("ENTENDIDO") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun verificarYSolicitarPermiso() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), PERMISSION_REQUEST_CODE)
            }
        }
    }

    private fun crearCanalNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Avisos e Informes Estudiantiles"
            val descriptionText = "Notificaciones informativas prioritarias arriba"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun lanzarNotificacionHeadsUp(titulo: String, mensaje: String) {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)

        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}