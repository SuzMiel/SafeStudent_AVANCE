package com.example.safestudent

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlin.math.abs
import kotlin.math.atan2
import org.osmdroid.config.Configuration
import org.osmdroid.events.*
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.infowindow.BasicInfoWindow
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class AlertaActivity : AppCompatActivity() {

    private companion object {
        const val CHANNEL_ID = "alertas_channel"
        const val PERMISSION_REQUEST_CODE = 1001
        const val GPS_PERMISSION_REQUEST_CODE = 1002

        val PUNTO_SENATI_CONO_SUR = GeoPoint(-18.038986086579403, -70.24926287883561)
        val PUNTO_SENATI_CIUDAD_NUEVA = GeoPoint(-17.988288656479437, -70.23784914151942)
        val PUNTO_INICIAL = GeoPoint(-18.03760570289263, -70.25071864765032)
    }

    // Variables UI Generales
    private lateinit var cardInfoSenati: CardView
    private lateinit var txtTituloInfoSenati: TextView
    private lateinit var txtDetalleInfoSenati: TextView
    private lateinit var btnCerrarInfoSenati: ImageView
    private val viewModel: AlertaViewModel by viewModels()

    // Variables UI Directorio (Sección 1)
    private lateinit var vistaDirectorio: LinearLayout
    private lateinit var btnNavDirectorio: Button
    private lateinit var btnTabEmergencia: Button
    private lateinit var btnTabApoyo: Button
    private lateinit var cardContacto1: CardView
    private lateinit var cardContacto2: CardView
    private lateinit var cardContacto3: CardView
    private lateinit var cardClinicas: CardView
    private lateinit var txtNombre1: TextView
    private lateinit var txtNumero1: TextView
    private lateinit var txtNombre2: TextView
    private lateinit var txtNumero2: TextView
    private lateinit var txtNombre3: TextView
    private lateinit var txtNumero3: TextView
    private var listaContactosActuales = listOf<ContactoEntity>()

    // Variables UI Mapa (Sección 2 y 3)
    private lateinit var vistaMapa: LinearLayout
    private lateinit var btnNavMapa: Button
    private lateinit var map: MapView
    private lateinit var miUbicacionOverlay: MyLocationNewOverlay
    private lateinit var layoutIndicadorConoSur: LinearLayout
    private lateinit var imgFlechaConoSur: ImageView
    private lateinit var layoutIndicadorCiudadNueva: LinearLayout
    private lateinit var imgFlechaCiudadNueva: ImageView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val ctx = applicationContext
        Configuration.getInstance().userAgentValue = "SafeStudentTacnaApp/1.0"
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))

        setContentView(R.layout.activity_alerta)

        enlazarVistas()

        val toolbar: Toolbar = findViewById(R.id.toolbarAlerta)
        toolbar.navigationIcon?.mutate()?.setTint(ContextCompat.getColor(this, android.R.color.white))
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        configurarNavegacionInferior()

        // INICIALIZACIÓN POR SECCIONES

        // Sección 1
        configurarPestañasDirectorio()
        configurarAccionesTarjetas()
        crearCanalNotificaciones()
        verificarPermisoNotificaciones()
        observarViewModel()

        val btnDemoAvisos: Button = findViewById(R.id.btnDemoAvisos)
        val btnDemoAlerta: Button = findViewById(R.id.btnDemoAlerta)
        configurarBotonesDemo(btnDemoAvisos, btnDemoAlerta)

        // Sección 2 y 3
        configurarMapa()
        configurarUbicacionLocal()

        // Carga inicial de datos
        viewModel.cargarContactosPorCategoria("Emergencias")
    }

    private fun enlazarVistas() {
        vistaMapa = findViewById(R.id.vistaMapa)
        vistaDirectorio = findViewById(R.id.vistaDirectorio)
        btnNavMapa = findViewById(R.id.btnNavMapa)
        btnNavDirectorio = findViewById(R.id.btnNavDirectorio)
        btnTabEmergencia = findViewById(R.id.btnTabEmergencia)
        btnTabApoyo = findViewById(R.id.btnTabApoyo)
        cardContacto1 = findViewById(R.id.cardContacto1)
        cardContacto2 = findViewById(R.id.cardContacto2)
        cardContacto3 = findViewById(R.id.cardContacto3)
        cardClinicas = findViewById(R.id.cardClinicas)
        txtNombre1 = findViewById(R.id.txtNombre1)
        txtNumero1 = findViewById(R.id.txtNumero1)
        txtNombre2 = findViewById(R.id.txtNombre2)
        txtNumero2 = findViewById(R.id.txtNumero2)
        txtNombre3 = findViewById(R.id.txtNombre3)
        txtNumero3 = findViewById(R.id.txtNumero3)
        layoutIndicadorConoSur = findViewById(R.id.layoutIndicadorConoSur)
        imgFlechaConoSur = findViewById(R.id.imgFlechaConoSur)
        layoutIndicadorCiudadNueva = findViewById(R.id.layoutIndicadorCiudadNueva)
        imgFlechaCiudadNueva = findViewById(R.id.imgFlechaCiudadNueva)
        cardInfoSenati = findViewById(R.id.cardInfoSenati)
        txtTituloInfoSenati = findViewById(R.id.txtTituloInfoSenati)
        txtDetalleInfoSenati = findViewById(R.id.txtDetalleInfoSenati)
        btnCerrarInfoSenati = findViewById(R.id.btnCerrarInfoSenati)

        btnCerrarInfoSenati.setOnClickListener {
            cardInfoSenati.visibility = View.GONE
        }
    }

    private fun configurarNavegacionInferior() {
        btnNavMapa.setOnClickListener {
            vistaMapa.visibility = View.VISIBLE
            vistaDirectorio.visibility = View.GONE
            btnNavMapa.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            btnNavDirectorio.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        }

        btnNavDirectorio.setOnClickListener {
            vistaMapa.visibility = View.GONE
            vistaDirectorio.visibility = View.VISIBLE
            btnNavDirectorio.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            btnNavMapa.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        }
    }


    // SECCIÓN 1: NOTIFICACIONES, ALERTAS Y DIRECTORIO DE NÚMEROS

    private fun configurarPestañasDirectorio() {
        btnTabEmergencia.setOnClickListener {
            btnTabEmergencia.setBackgroundColor(android.graphics.Color.parseColor("#1B365D"))
            btnTabEmergencia.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            btnTabApoyo.setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"))
            btnTabApoyo.setTextColor(android.graphics.Color.parseColor("#1B365D"))
            cardClinicas.visibility = View.VISIBLE
            viewModel.cargarContactosPorCategoria("Emergencias")
        }

        btnTabApoyo.setOnClickListener {
            btnTabApoyo.setBackgroundColor(android.graphics.Color.parseColor("#1B365D"))
            btnTabApoyo.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            btnTabEmergencia.setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"))
            btnTabEmergencia.setTextColor(android.graphics.Color.parseColor("#1B365D"))
            cardClinicas.visibility = View.GONE
            viewModel.cargarContactosPorCategoria("Apoyo Estudiantil")
        }
    }

    private fun configurarAccionesTarjetas() {
        cardContacto1.setOnClickListener {
            if (listaContactosActuales.isNotEmpty()) mostrarDialogoConfirmacion(listaContactosActuales[0].nombre, listaContactosActuales[0].numero)
        }
        cardContacto2.setOnClickListener {
            if (listaContactosActuales.size > 1) mostrarDialogoConfirmacion(listaContactosActuales[1].nombre, listaContactosActuales[1].numero)
        }
        cardContacto3.setOnClickListener {
            if (listaContactosActuales.size > 2) mostrarDialogoConfirmacion(listaContactosActuales[2].nombre, listaContactosActuales[2].numero)
        }
        cardClinicas.setOnClickListener {
            val nombresClinicas = arrayOf("Clínica La Luz ((052) 638720)", "Clínica Isabel ((052) 242401)", "Clínica Promedic ((052) 427239)")
            val numerosClinicas = arrayOf("052638720", "052242401", "052427239")
            AlertDialog.Builder(this)
                .setTitle("Clínicas Asociadas en Tacna")
                .setItems(nombresClinicas) { _, which -> mostrarDialogoConfirmacion(nombresClinicas[which], numerosClinicas[which]) }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun mostrarDialogoConfirmacion(nombre: String, numero: String) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar Llamada")
            .setMessage("¿Deseas realizar una llamada a $nombre ($numero)?")
            .setPositiveButton("LLAMAR") { _, _ ->
                startActivity(Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:$numero") })
            }
            .setNegativeButton("CANCELAR", null)
            .show()
    }

    private fun observarViewModel() {
        viewModel.aviso.observe(this) { aviso -> lanzarNotificacionHeadsUp(aviso.titulo, aviso.detalle) }
        viewModel.alerta.observe(this) { alerta ->
            AlertDialog.Builder(this).setTitle(alerta.titulo).setMessage(alerta.detalle)
                .setPositiveButton("ENTENDIDO") { dialog, _ -> dialog.dismiss() }.show()
        }
        viewModel.contactosActuales.observe(this) { contactos ->
            listaContactosActuales = contactos
            if (contactos.isNotEmpty()) {
                txtNombre1.text = limpiarNombre(contactos[0].nombre)
                txtNumero1.text = contactos[0].numero
                cardContacto1.visibility = View.VISIBLE
            } else cardContacto1.visibility = View.GONE

            if (contactos.size > 1) {
                txtNombre2.text = limpiarNombre(contactos[1].nombre)
                txtNumero2.text = contactos[1].numero
                cardContacto2.visibility = View.VISIBLE
            } else cardContacto2.visibility = View.GONE

            if (contactos.size > 2) {
                txtNombre3.text = limpiarNombre(contactos[2].nombre)
                txtNumero3.text = contactos[2].numero
                cardContacto3.visibility = View.VISIBLE
            } else cardContacto3.visibility = View.GONE
        }
    }

    private fun limpiarNombre(nombreCompleto: String): String {
        val index = nombreCompleto.indexOf("(")
        return if (index != -1) nombreCompleto.substring(0, index).trim() else nombreCompleto
    }

    private fun configurarBotonesDemo(btnAvisos: Button, btnAlerta: Button) {
        btnAvisos.setOnClickListener { viewModel.generarAvisoAleatorio() }
        btnAlerta.setOnClickListener { viewModel.generarAlertaAleatoria() }
    }

    private fun crearCanalNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Avisos e Informes Estudiantiles", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Notificaciones informativas prioritarias"
                enableVibration(true)
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    private fun verificarPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), PERMISSION_REQUEST_CODE)
        }
    }

    private fun lanzarNotificacionHeadsUp(titulo: String, mensaje: String) {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            // AGREGA ESTA LÍNEA (Usa directo el icono que creaste):
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)

        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(
            System.currentTimeMillis().toInt(),
            builder.build()
        )
    }


    // SECCIÓN 2: MAPA, PUNTOS SENATI, UBICACIÓN DEL USUARIO Y SEGUIMIENTO (INDICADORES)

    private fun configurarMapa() {
        map = findViewById(R.id.mapView)
        val osmTileSource = XYTileSource("OSMFR", 0, 19, 256, ".png", arrayOf("https://a.tile.openstreetmap.fr/osmfr/", "https://b.tile.openstreetmap.fr/osmfr/", "https://c.tile.openstreetmap.fr/osmfr/"))
        map.setTileSource(osmTileSource)
        map.setMultiTouchControls(true)
        map.controller.setZoom(17.5)
        map.controller.setCenter(PUNTO_INICIAL)

        agregarMarcadoresSenati()
        configurarIndicadoresSeguimiento()
        agregarZonasRojasDeRiesgo()
        map.invalidate()
    }

    private fun agregarMarcadoresSenati() {
        val originalIcon = ContextCompat.getDrawable(this, R.drawable.icono_senati)

        val markerCs = Marker(map).apply {
            position = PUNTO_SENATI_CONO_SUR
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = redimensionarDrawable(originalIcon, 40, 24)
            setOnMarkerClickListener { _, _ ->
                txtTituloInfoSenati.text = "SENATI - Sede Cono Sur"
                txtDetalleInfoSenati.text = "Buses con cobertura a la Sede:\nLinea 10-B\nLinea 14\nLinea 1\nLinea 15"
                cardInfoSenati.visibility = View.VISIBLE
                map.controller.animateTo(PUNTO_SENATI_CONO_SUR)
                true
            }
        }
        map.overlays.add(markerCs)

        val markerCn = Marker(map).apply {
            position = PUNTO_SENATI_CIUDAD_NUEVA
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = redimensionarDrawable(originalIcon, 40, 24)
            setOnMarkerClickListener { _, _ ->
                txtTituloInfoSenati.text = "SENATI - Sede Ciudad Nueva"
                txtDetalleInfoSenati.text = "Buses con cobertura a la Sede:\nLinea 15\nLinea 202\nLinea 22\nLinea 1"
                cardInfoSenati.visibility = View.VISIBLE
                map.controller.animateTo(PUNTO_SENATI_CIUDAD_NUEVA)
                true
            }
        }
        map.overlays.add(markerCn)
    }

    private fun configurarIndicadoresSeguimiento() {
        layoutIndicadorConoSur.setOnClickListener { map.controller.animateTo(PUNTO_SENATI_CONO_SUR) }
        layoutIndicadorCiudadNueva.setOnClickListener { map.controller.animateTo(PUNTO_SENATI_CIUDAD_NUEVA) }

        map.addMapListener(object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean { actualizarAmbosIndicadores(); return true }
            override fun onZoom(event: ZoomEvent?): Boolean { actualizarAmbosIndicadores(); return true }
        })
    }

    private fun actualizarAmbosIndicadores() {
        posicionarCartelEnBorde(PUNTO_SENATI_CONO_SUR, layoutIndicadorConoSur, imgFlechaConoSur)
        posicionarCartelEnBorde(PUNTO_SENATI_CIUDAD_NUEVA, layoutIndicadorCiudadNueva, imgFlechaCiudadNueva)
    }

    private fun posicionarCartelEnBorde(puntoDestino: GeoPoint, cartel: LinearLayout, flecha: ImageView) {
        val projection = map.projection ?: return
        val mapWidth = map.width
        val mapHeight = map.height
        if (mapWidth == 0 || mapHeight == 0) return

        val screenPoint = projection.toPixels(puntoDestino, null)
        val estaVisible = screenPoint.x in 0..mapWidth && screenPoint.y in 0..mapHeight

        if (estaVisible) {
            cartel.visibility = View.GONE
        } else {
            cartel.visibility = View.VISIBLE
            val padding = 24f
            val viewW = if (cartel.width > 0) cartel.width.toFloat() else 220f
            val viewH = if (cartel.height > 0) cartel.height.toFloat() else 70f
            val centroX = mapWidth / 2f
            val centroY = mapHeight / 2f
            val dx = screenPoint.x - centroX
            val dy = screenPoint.y - centroY
            val halfW = (mapWidth / 2f) - (viewW / 2f) - padding
            val halfH = (mapHeight / 2f) - (viewH / 2f) - padding
            val scaleX = if (dx != 0f) halfW / abs(dx) else Float.MAX_VALUE
            val scaleY = if (dy != 0f) halfH / abs(dy) else Float.MAX_VALUE
            val scale = minOf(scaleX, scaleY)

            cartel.x = centroX + (dx * scale) - (viewW / 2f)
            cartel.y = centroY + (dy * scale) - (viewH / 2f)
            val anguloRad = atan2(dy.toDouble(), dx.toDouble())
            flecha.rotation = Math.toDegrees(anguloRad).toFloat() + 90f
        }
    }

    private fun configurarUbicacionLocal() {
        val btnMiUbicacion: FloatingActionButton = findViewById(R.id.btnMiUbicacion)
        val proveedorGps = GpsMyLocationProvider(this)
        val iconoPersona = BitmapFactory.decodeResource(resources, org.osmdroid.library.R.drawable.person)

        miUbicacionOverlay = MyLocationNewOverlay(proveedorGps, map).apply {
            enableMyLocation()
            enableFollowLocation()
            setPersonIcon(iconoPersona)
            setDirectionIcon(iconoPersona)
        }
        map.overlays.add(miUbicacionOverlay)

        btnMiUbicacion.setOnClickListener {
            val miPosicion = miUbicacionOverlay.myLocation
            if (miPosicion != null) map.controller.animateTo(miPosicion)
            else {
                Toast.makeText(this, "Obteniendo señal GPS...", Toast.LENGTH_SHORT).show()
                miUbicacionOverlay.enableFollowLocation()
            }
        }
        verificarPermisosUbicacion()
    }

    private fun verificarPermisosUbicacion() {
        val fineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fineLocation != PackageManager.PERMISSION_GRANTED || coarseLocation != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), GPS_PERMISSION_REQUEST_CODE)
        } else miUbicacionOverlay.enableMyLocation()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == GPS_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) miUbicacionOverlay.enableMyLocation()
            else Toast.makeText(this, "Permiso de GPS no otorgado", Toast.LENGTH_SHORT).show()
        }
    }



    // SECCIÓN 3: RUTAS Y ZONAS DE RIESGO (CÍRCULOS ROJOS)

    private fun agregarZonasRojasDeRiesgo() {
        val zonasPeligro = listOf(
            Triple("Zona Peligro - Av. Municipal", "Reporte: Hurtos al paso y robos de celulares en horarios nocturnos.", GeoPoint(-18.03810, -70.25140)),
            Triple("Zona de Riesgo - Mariano Melgar", "Reporte: Baja iluminación pública y reportes de arrebatos de mochilas.", GeoPoint(-18.03980, -70.24830)),
            Triple("Punto Crítico - Calle Zela", "Reporte: Asaltos reportados en moto lineal durante fines de semana.", GeoPoint(-18.03600, -70.24980)),
            Triple("Zona Roja - Av. Internacional", "Reporte: Robos al paso y presencia de personas sospechosas cerca a paraderos.", GeoPoint(-17.98950, -70.23690)),
            Triple("Punto Crítico - Plaza José Olaya", "Reporte: Arrebatos de pertenencias y carteristas en horas punta.", GeoPoint(-17.98680, -70.23880)),
            Triple("Zona de Riesgo - Calle Mariano Necochea", "Reporte: Poca visibilidad nocturna y calles desoladas al salir de clases.", GeoPoint(-17.98740, -70.23560))
        )

        for (zona in zonasPeligro) {
            val markerCentro = Marker(map).apply {
                position = zona.third
                title = zona.first
                snippet = zona.second
                icon = ColorDrawable(Color.TRANSPARENT)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                infoWindow = BasicInfoWindow(org.osmdroid.library.R.layout.bonuspack_bubble, map)
            }
            map.overlays.add(markerCentro)

            // Círculo rojo semitransparente
            val circuloRojo = Polygon(map).apply {
                points = Polygon.pointsAsCircle(zona.third, 85.0)
                fillPaint.color = Color.argb(90, 244, 67, 54)   // Fondo rojo translúcido
                outlinePaint.color = Color.argb(220, 183, 28, 28) // Borde rojo oscuro
                outlinePaint.strokeWidth = 3.5f

                setOnClickListener { _, _, _ ->
                    cardInfoSenati.visibility = View.GONE
                    markerCentro.showInfoWindow()
                    map.controller.animateTo(zona.third)
                    true
                }
            }
            map.overlays.add(circuloRojo)
        }
    }

    // MÉTODOS DE CICLO DE VIDA Y UTILIDADES

    private fun redimensionarDrawable(drawable: Drawable?, anchoDp: Int, altoDp: Int): Drawable? {
        if (drawable !is BitmapDrawable) return null
        val densidad = resources.displayMetrics.density
        val anchoPx = (anchoDp * densidad).toInt()
        val altoPx = (altoDp * densidad).toInt()

        val bitmapEscalado = Bitmap.createScaledBitmap(drawable.bitmap, anchoPx, altoPx, true)
        return BitmapDrawable(resources, bitmapEscalado)
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
        if (::miUbicacionOverlay.isInitialized) miUbicacionOverlay.enableMyLocation()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
        if (::miUbicacionOverlay.isInitialized) miUbicacionOverlay.disableMyLocation()
    }
}