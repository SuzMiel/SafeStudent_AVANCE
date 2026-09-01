package com.example.safestudent

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {

    var codigoEstudiante: String = ""
    var sedeEstudiante: String = ""

    private val _estadoAlerta = MutableLiveData("Estado: Normal / Monitoreando")
    val estadoAlerta: LiveData<String> get() = _estadoAlerta

    fun validarCampos(codigo: String, sede: String): Boolean {
        return codigo.isNotBlank() && sede.isNotBlank()
    }

    fun activarAlerta() {
        _estadoAlerta.value = "Estado: ¡ALERTA SOS ENVIADA A SEGURIDAD!"
    }
}