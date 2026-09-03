package com.example.safestudent

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
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
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import org.osmdroid.config.Configuration
import org.osmdroid.events.*
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import kotlin.math.abs
import kotlin.math.atan2
import com.google.android.material.floatingactionbutton.FloatingActionButton

class AlertaActivity : AppCompatActivity() {

    private companion object {
        const val CHANNEL_ID = "alertas_channel"
        const val PERMISSION_REQUEST_CODE = 1001
        const val GPS_PERMISSION_REQUEST_CODE = 1002

        // Coordenadas reales de ambas sedes en Tacna
        val PUNTO_SENATI_CONO_SUR = GeoPoint(-18.038986086579403, -70.24926287883561)
        val PUNTO_SENATI_CIUDAD_NUEVA = GeoPoint(-17.988288656479437, -70.23784914151942)

        val PUNTO_INICIAL = GeoPoint(-18.03760570289263, -70.25071864765032)
    }

    private val viewModel: AlertaViewModel by viewModels()

    private lateinit var map: MapView
    private lateinit var spContacto: Spinner
    private lateinit var miUbicacionOverlay: MyLocationNewOverlay

    // Indicadores para Cono Sur
    private lateinit var layoutIndicadorConoSur: LinearLayout
    private lateinit var imgFlechaConoSur: ImageView

    // Indicadores para Ciudad Nueva
    private lateinit var layoutIndicadorCiudadNueva: LinearLayout
    private lateinit var imgFlechaCiudadNueva: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val ctx = applicationContext
        Configuration.getInstance().userAgentValue = "SafeStudentTacnaApp/1.0"
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))

        setContentView(R.layout.activity_alerta)

        val toolbar: Toolbar = findViewById(R.id.toolbarAlerta)
        map = findViewById(R.id.mapView)

        layoutIndicadorConoSur = findViewById(R.id.layoutIndicadorConoSur)
        imgFlechaConoSur = findViewById(R.id.imgFlechaConoSur)

        layoutIndicadorCiudadNueva = findViewById(R.id.layoutIndicadorCiudadNueva)
        imgFlechaCiudadNueva = findViewById(R.id.imgFlechaCiudadNueva)

        val btnDemoAvisos: Button = findViewById(R.id.btnDemoAvisos)
        val btnDemoAlerta: Button = findViewById(R.id.btnDemoAlerta)
        val btnLlamar: Button = findViewById(R.id.btnLlamarSeleccionado)
        val spCategoria: Spinner = findViewById(R.id.spCategoria)
        spContacto = findViewById(R.id.spContacto)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        configurarMapa()
        configurarUbicacionLocal()
        configurarSpinners(spCategoria)
        configurarBotones(btnDemoAvisos, btnDemoAlerta, btnLlamar)
        observarViewModel()

        crearCanalNotificaciones()
        verificarPermisoNotificaciones()
    }

    private fun configurarMapa() {
        val osmTileSource = XYTileSource(
            "OSMFR", 0, 19, 256, ".png",
            arrayOf(
                "https://a.tile.openstreetmap.fr/osmfr/",
                "https://b.tile.openstreetmap.fr/osmfr/",
                "https://c.tile.openstreetmap.fr/osmfr/"
            )
        )
        map.setTileSource(osmTileSource)
        map.setMultiTouchControls(true)

        val mapController = map.controller
        mapController.setZoom(17.5)
        mapController.setCenter(PUNTO_INICIAL)

        val originalIcon = ContextCompat.getDrawable(this, R.drawable.icono_senati)

        // 1. Marcador Sede Cono Sur
        val markerCs = Marker(map).apply {
            position = PUNTO_SENATI_CONO_SUR
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "SENATI Cono Sur"
            snippet = "Campus Cono Sur"
            icon = redimensionarDrawable(originalIcon, 40, 24)
        }
        map.overlays.add(markerCs)

        // 2. Marcador Sede Ciudad Nueva
        val markerCn = Marker(map).apply {
            position = PUNTO_SENATI_CIUDAD_NUEVA
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "SENATI Ciudad Nueva"
            snippet = "Campus Principal Ciudad Nueva"
            icon = redimensionarDrawable(originalIcon, 40, 24)
        }
        map.overlays.add(markerCn)

        // Al tocar cada cartel, viaja hacia la sede correspondiente
        layoutIndicadorConoSur.setOnClickListener {
            map.controller.animateTo(PUNTO_SENATI_CONO_SUR)
        }

        layoutIndicadorCiudadNueva.setOnClickListener {
            map.controller.animateTo(PUNTO_SENATI_CIUDAD_NUEVA)
        }

        map.addMapListener(object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                actualizarAmbosIndicadores()
                return true
            }

            override fun onZoom(event: ZoomEvent?): Boolean {
                actualizarAmbosIndicadores()
                return true
            }
        })
    }

    private fun actualizarAmbosIndicadores() {
        // Actualiza individualmente el cartel de cada sede
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

            // Rota la flecha hacia el punto correspondiente
            val anguloRad = atan2(dy.toDouble(), dx.toDouble())
            flecha.rotation = Math.toDegrees(anguloRad).toFloat() + 90f
        }
    }

    private fun configurarUbicacionLocal() {
        val btnMiUbicacion: FloatingActionButton = findViewById(R.id.btnMiUbicacion)

        val proveedorGps = GpsMyLocationProvider(this)
        miUbicacionOverlay = MyLocationNewOverlay(proveedorGps, map).apply {
            enableMyLocation()
            setDrawAccuracyEnabled(true)
            setDirectionIcon(ContextCompat.getDrawable(this@AlertaActivity, org.osmdroid.library.R.drawable.person)!!.mutate().apply { setTint(android.graphics.Color.parseColor("#007AFF")) }.let { (redimensionarDrawable(it, 48, 48) as BitmapDrawable).bitmap })
        }
        map.overlays.add(miUbicacionOverlay)

        btnMiUbicacion.setOnClickListener {
            val miPosicion = miUbicacionOverlay.myLocation
            if (miPosicion != null) {
                map.controller.animateTo(miPosicion)
            } else {
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
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                GPS_PERMISSION_REQUEST_CODE
            )
        } else {
            miUbicacionOverlay.enableMyLocation()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == GPS_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                miUbicacionOverlay.enableMyLocation()
            } else {
                Toast.makeText(this, "Permiso de GPS no otorgado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun configurarSpinners(spCategoria: Spinner) {
        val categorias = arrayOf("Emergencias", "Apoyo Estudiantil")
        spCategoria.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categorias)

        spCategoria.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.cargarContactosPorCategoria(categorias[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spContacto.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val listaActual = viewModel.contactosActuales.value
                if (!listaActual.isNullOrEmpty() && position in listaActual.indices) {
                    viewModel.numeroSeleccionado = listaActual[position].numero
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun configurarBotones(btnAvisos: Button, btnAlerta: Button, btnLlamar: Button) {
        btnAvisos.setOnClickListener {
            viewModel.generarAvisoAleatorio()
        }

        btnAlerta.setOnClickListener {
            viewModel.generarAlertaAleatoria()
        }

        btnLlamar.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${viewModel.numeroSeleccionado}")
            }
            startActivity(intent)
        }
    }

    private fun observarViewModel() {
        viewModel.aviso.observe(this) { aviso ->
            lanzarNotificacionHeadsUp(aviso.titulo, aviso.detalle)
        }

        viewModel.alerta.observe(this) { alerta ->
            AlertDialog.Builder(this)
                .setTitle(alerta.titulo)
                .setMessage(alerta.detalle)
                .setPositiveButton("ENTENDIDO") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

        viewModel.contactosActuales.observe(this) { contactos ->
            val nombres = contactos.map { it.nombre }
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, nombres)
            spContacto.adapter = adapter
            if (contactos.isNotEmpty()) {
                viewModel.numeroSeleccionado = contactos[0].numero
            }
        }
    }

    private fun crearCanalNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Avisos e Informes Estudiantiles"
            val channel = NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Notificaciones informativas prioritarias"
                enableVibration(true)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun verificarPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), PERMISSION_REQUEST_CODE)
            }
        }
    }

    private fun lanzarNotificacionHeadsUp(titulo: String, mensaje: String) {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    private fun redimensionarDrawable(drawable: Drawable?, anchoDp: Int, altoDp: Int): Drawable? {
        if (drawable == null) return null

        val densidad = resources.displayMetrics.density
        val anchoPx = (anchoDp * densidad).toInt()
        val altoPx = (altoDp * densidad).toInt()

        val bitmap = when (drawable) {
            is BitmapDrawable -> drawable.bitmap
            else -> {
                val bmp = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bmp)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bmp
            }
        }
        val bitmapEscalado = Bitmap.createScaledBitmap(bitmap, anchoPx, altoPx, true)
        return BitmapDrawable(resources, bitmapEscalado)
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
        if (::miUbicacionOverlay.isInitialized) {
            miUbicacionOverlay.enableMyLocation()
        }
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
        if (::miUbicacionOverlay.isInitialized) {
            miUbicacionOverlay.disableMyLocation()
        }
    }
}