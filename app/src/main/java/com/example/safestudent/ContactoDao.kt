package com.example.safestudent

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ContactoDao {

    @Insert
    suspend fun insertar(contacto: ContactoEntity)

    @Query("SELECT * FROM tabla_contactos WHERE categoria = :categoria")
    suspend fun obtenerPorCategoria(categoria: String): List<ContactoEntity>

    @Query("SELECT COUNT(*) FROM tabla_contactos")
    suspend fun contarContactos(): Int
}