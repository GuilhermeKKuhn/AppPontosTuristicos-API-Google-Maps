package br.edu.utfpr.projetopontoturistico

import android.content.Intent
import android.os.Bundle
import android.widget.ListView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class ListarActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var banco: PontoTuristicoDatabaseHelper
    private lateinit var listaPontos: List<PontoTuristico>
    private lateinit var btnNovo: Button
    private lateinit var btnConfig: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_listar)

        listView = findViewById(R.id.listView)
        banco = PontoTuristicoDatabaseHelper(this)

        listaPontos = banco.getAllPontosTuristicos()

        val adapter = ElementoListaAdapter(this, listaPontos)
        listView.adapter = adapter

        listView.setOnItemClickListener { parent, view, position, id ->
            val ponto = listaPontos[position]

            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("id", ponto.id)
                putExtra("nome", ponto.nome)
                putExtra("descricao", ponto.descricao)
                putExtra("latitude", ponto.latitude)
                putExtra("longitude", ponto.longitude)
                putExtra("foto", ponto.foto)
            }
            startActivity(intent)
        }

        btnNovo = findViewById(R.id.btnNovo)

        btnNovo.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        btnConfig = findViewById(R.id.btnConfig)

        btnConfig.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }
}
