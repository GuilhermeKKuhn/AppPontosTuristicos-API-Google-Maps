package br.edu.utfpr.projetopontoturistico

data class PontoTuristico(
    val id: Long,
    val nome : String,
    val descricao: String,
    val latitude: Double,
    val longitude: Double,
    val foto: String
)
