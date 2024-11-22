package br.edu.utfpr.projetopontoturistico

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.preference.PreferenceManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.Locale

class MainActivity : AppCompatActivity(), LocationListener {

    private lateinit var etDescricao: EditText
    private lateinit var etNome: EditText
    private lateinit var locationManager: LocationManager
    private lateinit var banco: PontoTuristicoDatabaseHelper
    private lateinit var btSalvar: Button
    private lateinit var btExcluir: Button
    private lateinit var btCapturarLocalizacao: Button
    private lateinit var ivFoto: ImageView
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var pontoId: Long = -1

    private lateinit var btCapturarFoto: Button
    private var fotoUri: Uri? = null
    private val REQUEST_IMAGE_CAPTURE = 2

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etNome = findViewById(R.id.etNome)
        etDescricao = findViewById(R.id.etDescricao)
        ivFoto = findViewById(R.id.ivFoto)
        btSalvar = findViewById(R.id.btSalvar)
        btExcluir = findViewById(R.id.btExcluir)
        btCapturarLocalizacao = findViewById(R.id.btCapturarLocalizacao)
        btCapturarFoto = findViewById(R.id.btCapturarFoto)

        banco = PontoTuristicoDatabaseHelper(this)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        locationManager.requestLocationUpdates(
            LocationManager.NETWORK_PROVIDER,
            0,
            0f,
            this
        )

        val intent = intent
        pontoId = intent.getLongExtra("id", -1)
        val nome = intent.getStringExtra("nome")
        val descricao = intent.getStringExtra("descricao")
        latitude = intent.getDoubleExtra("latitude", 0.0)
        longitude = intent.getDoubleExtra("longitude", 0.0)
        val foto = intent.getStringExtra("foto")

        etDescricao.setText(descricao)
        etNome.setText(nome)

        if (!foto.isNullOrEmpty()) {
            val fotoUri = Uri.parse(foto)

            try {
                val inputStream = contentResolver.openInputStream(fotoUri)
                if (inputStream != null) {
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    ivFoto.setImageBitmap(bitmap)
                    inputStream.close()
                } else {
                    ivFoto.setImageResource(R.drawable.ic_launcher_background)
                }
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
                ivFoto.setImageResource(R.drawable.ic_launcher_background)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Erro ao carregar a imagem.", Toast.LENGTH_SHORT).show()
            }
        } else {
            ivFoto.setImageResource(R.drawable.ic_launcher_background)
        }

        btCapturarFoto.setOnClickListener {
            abrirCamera()
        }

        btSalvar.setOnClickListener {
            btSalvarPontoOnClick()
        }

        btExcluir.setOnClickListener {
            btExcluirOnClick()
        }

        btCapturarLocalizacao.setOnClickListener {
            btCapturarLocalizacaoOnClick()
        }
    }

    private fun abrirCamera() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
        } else {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

            val fotoFile: File? = criarArquivoFoto()
            if (fotoFile != null) {
                fotoUri = FileProvider.getUriForFile(
                    this,
                    "${applicationContext.packageName}.provider",
                    fotoFile
                )
                intent.putExtra(MediaStore.EXTRA_OUTPUT, fotoUri)
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }


    private fun criarArquivoFoto(): File? {
        val storageDir = getExternalFilesDir("Pictures")
        return try {
            File.createTempFile("foto_", ".jpg", storageDir)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    private fun carregarConfiguracoes(context: Context): ConfigMaps {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        val tipoMapa = sharedPreferences.getString("tipo_mapa", "NORMAL") ?: "NORMAL"
        val nivelZoom = sharedPreferences.getString("zoom_level", "10")?.toInt() ?: 10
        val usarGeocoding = sharedPreferences.getBoolean("usar_geocod", false)

        return ConfigMaps(tipoMapa, nivelZoom, usarGeocoding)
    }

    private fun btSalvarPontoOnClick() {
        val descricao = etDescricao.text.toString()
        val nome = etNome.text.toString()
        val foto = fotoUri?.toString() ?: ""

        if (nome.isNotEmpty()) {
            if (latitude != 0.0 && longitude != 0.0) {
                if (pontoId != -1L) {
                    banco.atualizarPontoTuristico(pontoId, nome, descricao, latitude, longitude, foto)
                    Toast.makeText(this, "Ponto turístico atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                } else {
                    banco.salvarPontoTuristico(nome, descricao, latitude, longitude, foto)
                    Toast.makeText(this, "Ponto turístico salvo com sucesso!", Toast.LENGTH_SHORT).show()
                }
                val intent = Intent(this, ListarActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Não foi possível capturar a localização.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Por favor, insira um nome.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun btExcluirOnClick() {
        banco.deletePontoTuristico(pontoId)
        Toast.makeText(this, "Ponto turístico atualizado com sucesso!", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, ListarActivity::class.java)
        startActivity(intent)
    }


    private fun btCapturarLocalizacaoOnClick() {
        val intent = Intent(this, MapsActivity::class.java)
        startActivityForResult(intent, 1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            1 -> {
                if (resultCode == RESULT_OK) {
                    val configuracoes = carregarConfiguracoes(this)

                    latitude = data?.getDoubleExtra("latitude", 0.0) ?: 0.0
                    longitude = data?.getDoubleExtra("longitude", 0.0) ?: 0.0

                    Toast.makeText(this, "Localização capturada: $latitude, $longitude", Toast.LENGTH_SHORT).show()

                    val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
                    mapFragment.getMapAsync { googleMap ->
                        when (configuracoes.tipoMapa) {
                            "NORMAL" -> googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
                            "SATELLITE" -> googleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
                            "TERRAIN" -> googleMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
                        }

                        val zoom = configuracoes.nivelZoom.toFloat()
                        val cameraPosition = CameraPosition.Builder()
                            .target(LatLng(latitude, longitude))
                            .zoom(zoom)
                            .build()

                        googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                    }
                }
            }

            2 -> {
                if (resultCode == RESULT_OK) {
                    if (fotoUri != null) {
                        ivFoto.setImageURI(fotoUri)
                    } else {
                        Toast.makeText(this, "Erro ao capturar a foto.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Foto não capturada.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    override fun onLocationChanged(location: Location) {
        latitude = location.latitude
        longitude = location.longitude
        getAddressFromCoordinates(latitude, longitude)

        val configuracoes = carregarConfiguracoes(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync { googleMap ->
            if (googleMap != null) {
                when (configuracoes.tipoMapa) {
                    "NORMAL" -> googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
                    "SATELLITE" -> googleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
                    "TERRAIN" -> googleMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
                }

                val zoom = configuracoes.nivelZoom.toFloat()
                val cameraPosition = CameraPosition.Builder()
                    .target(LatLng(latitude, longitude))
                    .zoom(zoom)
                    .build()

                googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
            }
        }
    }




    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            1 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return
                    }
                    locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, this, null)
                } else {
                    Toast.makeText(this, "Permissão para acessar a localização é necessária.", Toast.LENGTH_SHORT).show()
                }
            }

            100 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    abrirCamera()
                } else {
                    Toast.makeText(this, "Permissão da câmera negada.", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                Toast.makeText(this, "Permissões não reconhecidas.", Toast.LENGTH_SHORT).show()
            }
        }
    }




    private fun getAddressFromCoordinates(latitude: Double, longitude: Double) {
        val geocoder = Geocoder(this, Locale.getDefault())

        try {
            val addresses: MutableList<Address>? = geocoder.getFromLocation(latitude, longitude, 1)

            if (addresses != null) {
                if (addresses.isNotEmpty()) {
                    val address = addresses?.get(0)
                    val addressText = StringBuilder()

                    if (address != null) {
                        for (i in 0..address.maxAddressLineIndex) {
                            addressText.append(address.getAddressLine(i)).append("\n")
                        }
                    }
                    etDescricao.setText(addressText.toString())

                } else {
                    Toast.makeText(this, "Nenhum endereço encontrado para as coordenadas.", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Erro ao obter o endereço.", Toast.LENGTH_SHORT).show()
        }
    }
}
