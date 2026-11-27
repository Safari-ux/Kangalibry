package com.example.turkids.repository

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import com.example.turkids.model.LugarDetectado
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume

val permisosUbicacion = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
)

class UbicacionRepository(contextoApp: Context) {

    private val contexto: Context = contextoApp.applicationContext
    private val administrador =
        contexto.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    fun permisoConcedido(): Boolean = permisosUbicacion.any { tienePermiso(it) }

    fun ubicacionActivada(): Boolean = LocationManagerCompat.isLocationEnabled(administrador)

    suspend fun obtenerLugarActual(): LugarDetectado? {
        val ubicacion = obtenerUbicacion() ?: return null
        val direccion = buscarDireccion(ubicacion)
        return LugarDetectado(
            codigoPais = direccion?.countryCode,
            nombrePais = direccion?.countryName,
            coordenadas = String.format(Locale.US, "%.2f, %.2f", ubicacion.latitude, ubicacion.longitude)
        )
    }

    private fun tienePermiso(permiso: String): Boolean =
        ContextCompat.checkSelfPermission(contexto, permiso) == PackageManager.PERMISSION_GRANTED

    private fun proveedoresDisponibles(): List<String> =
        elegirProveedores(
            sdk = Build.VERSION.SDK_INT,
            tieneUbicacionPrecisa = tienePermiso(Manifest.permission.ACCESS_FINE_LOCATION)
        ) { administrador.isProviderEnabled(it) }

    // primero la ubicacion actual de cada proveedor; si ninguno responde, la ultima conocida
    private suspend fun obtenerUbicacion(): Location? {
        val proveedores = proveedoresDisponibles()
        for (proveedor in proveedores) {
            val ubicacion = withTimeoutOrNull(TIEMPO_ESPERA_MS) { ubicacionActual(proveedor) }
            if (ubicacion != null) return ubicacion
        }
        return (proveedores + LocationManager.PASSIVE_PROVIDER).firstNotNullOfOrNull { ultimaConocida(it) }
    }

    @SuppressLint("MissingPermission")
    private suspend fun ubicacionActual(proveedor: String): Location? =
        suspendCancellableCoroutine { continuacion ->
            val senal = CancellationSignal()
            continuacion.invokeOnCancellation { senal.cancel() }
            try {
                LocationManagerCompat.getCurrentLocation(
                    administrador,
                    proveedor,
                    senal,
                    ContextCompat.getMainExecutor(contexto)
                ) { ubicacion -> continuacion.resume(ubicacion) }
            } catch (e: SecurityException) {
                continuacion.resume(null)
            }
        }

    @SuppressLint("MissingPermission")
    private fun ultimaConocida(proveedor: String): Location? =
        try {
            administrador.getLastKnownLocation(proveedor)
        } catch (e: SecurityException) {
            null
        }

    private suspend fun buscarDireccion(ubicacion: Location): Address? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(contexto, Locale.getDefault())
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuacion ->
                    geocoder.getFromLocation(
                        ubicacion.latitude,
                        ubicacion.longitude,
                        1,
                        object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<Address>) {
                                continuacion.resume(addresses.firstOrNull())
                            }

                            override fun onError(errorMessage: String?) {
                                continuacion.resume(null)
                            }
                        }
                    )
                }
            } else {
                withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(ubicacion.latitude, ubicacion.longitude, 1)?.firstOrNull()
                }
            }
        } catch (e: IOException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private companion object {
        const val TIEMPO_ESPERA_MS = 6_000L
    }
}
