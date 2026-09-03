package com.example.safestudent

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

data class MensajeAlerta(val titulo: String, val detalle: String)

class AlertaViewModel(application: Application) : AndroidViewModel(application) {

    private val dao: ContactoDao = AppDatabase.getDatabase(application).contactoDao()

    private val listaAvisos = listOf(
        MensajeAlerta("Aniversario SENATI Tacna", "¡Te invitamos a las actividades académicas y deportivas este viernes!"),
        MensajeAlerta("Paro de Transportistas Tacna", "Comunicado: Clases virtuales del 7 al 9 de Septiembre por prevención."),
        MensajeAlerta("Prácticas Pre-Profesionales", "Recordatorio: Fecha límite para presentar la carta de presentación de empresa.")
    )

    private val listaAlertas = listOf(
        MensajeAlerta("ALERTA SÍSMICA DE EMERGENCIA", "Sismo detectado en Tacna. Mantenga la calma y diríjase a las zonas de seguridad más cercanas."),
        MensajeAlerta("ALERTA METEOROLÓGICA", "Lluvias intensas detectadas. Resguarde equipos electrónicos y evite áreas descubiertas."),
        MensajeAlerta("ALERTA DE VIENTOS EXTREMOS", "Vientos fuertes en la zona. Evite estructuras frágiles y asegure ventanas en las aulas.")
    )

    private val _aviso = MutableLiveData<MensajeAlerta>()
    val aviso: LiveData<MensajeAlerta> get() = _aviso

    private val _alerta = MutableLiveData<MensajeAlerta>()
    val alerta: LiveData<MensajeAlerta> get() = _alerta

    // Contactos cargados desde SQLite para la UI
    private val _contactosActuales = MutableLiveData<List<ContactoEntity>>()
    val contactosActuales: LiveData<List<ContactoEntity>> get() = _contactosActuales

    var numeroSeleccionado: String = "116"

    init {
        precargarDatosSiEsNecesario()
    }

    private fun precargarDatosSiEsNecesario() = viewModelScope.launch {
        // Si la tabla está vacía en SQLite, insertamos los contactos iniciales
        if (dao.contarContactos() == 0) {
            dao.insertar(ContactoEntity(categoria = "Emergencias", nombre = "Bomberos (116)", numero = "116"))
            dao.insertar(ContactoEntity(categoria = "Emergencias", nombre = "Policía (105)", numero = "105"))
            dao.insertar(ContactoEntity(categoria = "Emergencias", nombre = "Seguridad Campus", numero = "952000111"))

            dao.insertar(ContactoEntity(categoria = "Apoyo Estudiantil", nombre = "Tutoría", numero = "952333444"))
            dao.insertar(ContactoEntity(categoria = "Apoyo Estudiantil", nombre = "Secretaría", numero = "952555666"))
            dao.insertar(ContactoEntity(categoria = "Apoyo Estudiantil", nombre = "Fondo Mi Futuro", numero = "952999000"))
        }
        cargarContactosPorCategoria("Emergencias")
    }

    fun cargarContactosPorCategoria(categoria: String) = viewModelScope.launch {
        _contactosActuales.value = dao.obtenerPorCategoria(categoria)
    }

    fun generarAvisoAleatorio() {
        _aviso.value = listaAvisos.random()
    }

    fun generarAlertaAleatoria() {
        _alerta.value = listaAlertas.random()
    }
}