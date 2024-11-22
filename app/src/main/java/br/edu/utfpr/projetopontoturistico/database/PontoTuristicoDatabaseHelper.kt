package br.edu.utfpr.projetopontoturistico

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class PontoTuristicoDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "pontos_turisticos.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "pontos_turisticos"
        private const val COLUMN_ID = "id"
        private const val COLUMN_NOME = "nome"
        private const val COLUMN_DESCRICAO = "descricao"
        private const val COLUMN_LATITUDE = "latitude"
        private const val COLUMN_LONGITUDE = "longitude"
        private const val COLUMN_FOTO = "foto"
    }


    override fun onCreate(db: SQLiteDatabase?) {
        val createTableSQL = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_DESCRICAO TEXT,
                $COLUMN_NOME TEXT,
                $COLUMN_LATITUDE REAL,
                $COLUMN_LONGITUDE REAL,
                $COLUMN_FOTO TEXT
            )
        """.trimIndent()
        db?.execSQL(createTableSQL)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun salvarPontoTuristico(nome : String, descricao: String, latitude: Double, longitude: Double, foto: String) {
        val db = writableDatabase
        val sql = """
            INSERT INTO $TABLE_NAME ($COLUMN_NOME,$COLUMN_DESCRICAO, $COLUMN_LATITUDE, $COLUMN_LONGITUDE, $COLUMN_FOTO)
            VALUES (?, ?, ?, ?, ?)
        """
        val statement = db.compileStatement(sql)
        statement.bindString(1, nome)
        statement.bindString(2, descricao)
        statement.bindDouble(3, latitude)
        statement.bindDouble(4, longitude)
        statement.bindString(5, foto)
        statement.executeInsert()
        db.close()
    }

    fun getAllPontosTuristicos(): List<PontoTuristico> {
        val pontosTuristicos = mutableListOf<PontoTuristico>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME", null)

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID))
                val nome = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOME))
                val descricao = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRICAO))
                val latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LATITUDE))
                val longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LONGITUDE))
                val foto = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FOTO))

                val pontoTuristico = PontoTuristico(id, nome, descricao, latitude, longitude, foto)
                pontosTuristicos.add(pontoTuristico)
            } while (cursor.moveToNext())
        }
        cursor.close()

        return pontosTuristicos
    }

    fun atualizarPontoTuristico(pontoId: Long, nome : String, descricao: String, latitude: Double, longitude: Double, foto: String): Int {
        val db = writableDatabase

        val values = ContentValues().apply {
            put(COLUMN_NOME, nome)
            put(COLUMN_DESCRICAO, descricao)
            put(COLUMN_LATITUDE, latitude)
            put(COLUMN_LONGITUDE, longitude)
            put(COLUMN_FOTO, foto)
        }

        val rowsAffected = db.update(
            TABLE_NAME,
            values,
            "$COLUMN_ID = ?",
            arrayOf(pontoId.toString())
        )

        db.close()
        return rowsAffected
    }


    fun deletePontoTuristico(id: Long): Int {
        val db = writableDatabase
        val rowsDeleted = db.delete(TABLE_NAME, "$COLUMN_ID = ?", arrayOf(id.toString()))
        db.close()
        return rowsDeleted
    }
}
