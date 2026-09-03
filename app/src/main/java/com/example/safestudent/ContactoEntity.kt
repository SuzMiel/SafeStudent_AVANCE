package com.example.safestudent

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tabla_contactos")
data class ContactoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val categoria: String,
    val nombre: String,
    val numero: String
)