package br.edu.utfpr.projetopontoturistico

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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

class MainActivity : AppCompatActivity(), LocationListener {

    private lateinit var etDescricao: EditText
    private lateinit var etNome: EditText
    private lateinit var locationManager: LocationManager
    private lateinit var banco: PontoTuristicoDatabaseHelper
    private lateinit var btSalvar: Button
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

        // Inicializa os componentes da interface
        etNome = findViewById(R.id.etNome)
        etDescricao = findViewById(R.id.etDescricao)
        ivFoto = findViewById(R.id.ivFoto)
        btSalvar = findViewById(R.id.btSalvar)
        btCapturarFoto = findViewById(R.id.btCapturarFoto)

        // Inicializa o banco de dados
        banco = PontoTuristicoDatabaseHelper(this)

        // Configuração do LocationManager
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager


        // Verifica permissões para obter a localização
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

        // Solicita atualizações de localização (se necessário)
        locationManager.requestLocationUpdates(
            LocationManager.NETWORK_PROVIDER,
            0,
            0f,
            this
        )

        // Recebe os dados passados pela ListarActivity
        val intent = intent
        pontoId = intent.getLongExtra("id", -1)
        val nome = intent.getStringExtra("nome")
        val descricao = intent.getStringExtra("descricao")
        latitude = intent.getDoubleExtra("latitude", 0.0)
        longitude = intent.getDoubleExtra("longitude", 0.0)
        val foto = intent.getStringExtra("foto")

        //todo - FALTA A FOTO E AJUSTAR LATITUDE E LONGITUDE
        etDescricao.setText(descricao)
        etNome.setText(nome)

        // Aqui você pode configurar a foto no ImageView se necessário
        // Exemplo: ivFoto.setImageURI(Uri.parse(foto))

        btCapturarFoto.setOnClickListener {
            abrirCamera()
        }

        btSalvar.setOnClickListener {
            btSalvarPontoOnClick()
        }
    }

    // Método para abrir a câmera
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


    // Criar arquivo para salvar a foto
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

        // Recuperar valores salvos da config
        val tipoMapa = sharedPreferences.getString("tipo_mapa", "NORMAL") ?: "NORMAL"
        val nivelZoom = sharedPreferences.getString("zoom_level", "10")?.toInt() ?: 10
        val usarGeocoding = sharedPreferences.getBoolean("usar_geocod", false)

        return ConfigMaps(tipoMapa, nivelZoom, usarGeocoding)
    }

    private fun btSalvarPontoOnClick() {
        val descricao = etDescricao.text.toString()
        val nome = etNome.text.toString()

        // Verifica se o caminho da foto foi capturado anteriormente
        val foto = fotoUri?.toString() ?: "" // Se fotoUri for nulo, salva uma string vazia

        if (nome.isNotEmpty()) {
            if (descricao.isNotEmpty()) {
                if (latitude != 0.0 && longitude != 0.0) {
                    if (pontoId != -1L) {
                        // Atualiza ponto turístico existente
                        banco.atualizarPontoTuristico(pontoId, nome, descricao, latitude, longitude, foto)
                        Toast.makeText(this, "Ponto turístico atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                    } else {
                        // Salva novo ponto turístico
                        banco.salvarPontoTuristico(nome, descricao, latitude, longitude, foto)
                        Toast.makeText(this, "Ponto turístico salvo com sucesso!", Toast.LENGTH_SHORT).show()
                    }
                    // Volta para a tela de listagem
                    val intent = Intent(this, ListarActivity::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Não foi possível capturar a localização.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Por favor, insira uma descrição.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Por favor, insira um nome.", Toast.LENGTH_SHORT).show()
        }
    }


    fun btCapturarLocalizacaoOnClick(view: View) {
        val intent = Intent(this, MapsActivity::class.java)
        // startActivityForResult captura os dados do mapa
        startActivityForResult(intent, 1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            1 -> { // Resultado vindo do MapsActivity
                if (resultCode == RESULT_OK) {
                    // Configurações do mapa
                    val configuracoes = carregarConfiguracoes(this)

                    // Atualizar latitude e longitude
                    latitude = data?.getDoubleExtra("latitude", 0.0) ?: 0.0
                    longitude = data?.getDoubleExtra("longitude", 0.0) ?: 0.0

                    Toast.makeText(this, "Localização capturada: $latitude, $longitude", Toast.LENGTH_SHORT).show()

                    // Atualizar o mapa
                    val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
                    mapFragment.getMapAsync { googleMap ->
                        // Aplicar tipo de mapa
                        when (configuracoes.tipoMapa) {
                            "NORMAL" -> googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
                            "SATELLITE" -> googleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
                            "TERRAIN" -> googleMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
                        }

                        // Atualizar posição e zoom da câmera
                        val zoom = configuracoes.nivelZoom.toFloat()
                        val cameraPosition = CameraPosition.Builder()
                            .target(LatLng(latitude, longitude))
                            .zoom(zoom)
                            .build()

                        googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                    }
                }
            }

            2 -> { // Resultado vindo da câmera
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


    // Este método é chamado quando a localização é alterada
    override fun onLocationChanged(location: Location) {
        latitude = location.latitude
        longitude = location.longitude

        // Garantir que as configurações do mapa estão carregadas corretamente
        val configuracoes = carregarConfiguracoes(this)

        // Verifica se o fragmento do mapa já foi inicializado
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync { googleMap ->
            // Verifica se o googleMap foi carregado corretamente
            if (googleMap != null) {
                // Configurar o tipo de mapa
                when (configuracoes.tipoMapa) {
                    "NORMAL" -> googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
                    "SATELLITE" -> googleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
                    "TERRAIN" -> googleMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
                }

                // Aplicar o zoom
                val zoom = configuracoes.nivelZoom.toFloat()
                val cameraPosition = CameraPosition.Builder()
                    .target(LatLng(latitude, longitude))
                    .zoom(zoom)
                    .build()

                googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
            }
        }
    }

    // Função de permissões
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            1 -> { // Código para permissões de localização
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

            100 -> { // Código para permissões de câmera
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permissão concedida, tenta abrir a câmera
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


    private fun verificarPermissoesCamera(): Boolean {
        val permissions = arrayOf(Manifest.permission.CAMERA)
        val permissionCode = 100

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissions, permissionCode)
            return false
        }
        return true
    }
}
