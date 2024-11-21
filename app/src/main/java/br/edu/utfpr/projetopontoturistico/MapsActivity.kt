package br.edu.utfpr.projetopontoturistico

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnSuccessListener
import androidx.preference.PreferenceManager

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var zoom: Float = 15f // Nível de zoom padrão
    private var tipoMapa: String = "NORMAL" // Tipo de mapa padrão (Normal, Satélite, Terreno)
    private var usandoGeocode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_maps)

        // Inicializa o FusedLocationProviderClient para obter a localização do usuário
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Carregar as configurações do SharedPreferences
        carregarConfiguracoes()

        // Configura o fragmento do mapa
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    // Função para carregar as configurações salvas nas SharedPreferences
    private fun carregarConfiguracoes() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        // Recuperar valores salvos da config
        tipoMapa = sharedPreferences.getString("tipo_mapa", "NORMAL") ?: "NORMAL"
        zoom = sharedPreferences.getString("zoom_level", "15")?.toFloat() ?: 15f
        usandoGeocode = sharedPreferences.getBoolean("usar_geocod", false)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Verifica se a permissão de localização foi concedida
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED) {

            // Habilita o recurso de localização no mapa
            mMap.isMyLocationEnabled = true

            // Obtém a localização atual do usuário
            fusedLocationClient.lastLocation.addOnSuccessListener(this, OnSuccessListener { location: Location? ->
                if (location != null) {
                    val userLocation = LatLng(location.latitude, location.longitude)

                    // Coloca um marcador na localização do usuário
                    mMap.addMarker(MarkerOptions().position(userLocation).title("Você está aqui"))
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, zoom))
                } else {
                    Toast.makeText(this, "Não foi possível obter a localização", Toast.LENGTH_SHORT).show()
                }
            })

        } else {
            // Se a permissão não foi concedida, solicita ao usuário
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        }

        // Agora aplique as configurações do mapa:
        aplicarConfiguracoes()
    }

    // Função para aplicar as configurações do tipo de mapa e zoom
    private fun aplicarConfiguracoes() {
        // Ajustar o tipo de mapa conforme as preferências salvas
        when (tipoMapa) {
            "NORMAL" -> mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            "SATELLITE" -> mMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
            "TERRAIN" -> mMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
            else -> mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        }

        // Ajustar o zoom conforme o valor salvo
        mMap.moveCamera(CameraUpdateFactory.zoomTo(zoom))

        // Se for necessário, você pode usar geocodificação ou outras funções dependendo da configuração
        if (usandoGeocode) {
            // Exemplo de como você poderia usar geocodificação, se necessário
            // Implementar geocodificação aqui, se for necessário
        }
    }

    // Metodo chamado quando o usuário responde à solicitação de permissão
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissão concedida, agora tenta obter a localização novamente
                onMapReady(mMap)
            } else {
                Toast.makeText(this, "Permissão para acessar a localização é necessária", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
