package br.edu.utfpr.projetopontoturistico

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import java.io.File
import java.io.FileNotFoundException

class ElementoListaAdapter(
    private val context: Context,
    private val listaPontos: List<PontoTuristico>
) : BaseAdapter() {

    override fun getCount(): Int = listaPontos.size

    override fun getItem(position: Int): Any = listaPontos[position]

    override fun getItemId(position: Int): Long = listaPontos[position].id

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView ?: LayoutInflater.from(context).inflate(R.layout.elemento_lista, parent, false)

        val cadastro = listaPontos[position]

        val tvNome = view.findViewById<TextView>(R.id.tvNome)
        val tvDescricao = view.findViewById<TextView>(R.id.tvDescricao)
        val tvLongitude = view.findViewById<TextView>(R.id.tvLongitude)
        val tvLatitude = view.findViewById<TextView>(R.id.tvLatitude)
        val ivFoto = view.findViewById<ImageView>(R.id.tvFoto)

        tvNome.text = cadastro.nome
        tvDescricao.text = cadastro.descricao
        tvLatitude.text = cadastro.latitude.toString()
        tvLongitude.text = cadastro.longitude.toString()

        val fotoUri = Uri.parse(cadastro.foto)  // Assume que cadastro.fotoPath contém o caminho 'content://'

        // Carregar a imagem usando ContentResolver
        try {
            val inputStream = context.contentResolver.openInputStream(fotoUri)
            if (inputStream != null) {
                val bitmap = BitmapFactory.decodeStream(inputStream)
                ivFoto.setImageBitmap(bitmap)
                inputStream.close()
            } else {
                ivFoto.setImageResource(R.drawable.ic_launcher_background) // Imagem padrão caso não consiga carregar
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            ivFoto.setImageResource(R.drawable.ic_launcher_background) // Imagem padrão
        }

        return view

    }
}
