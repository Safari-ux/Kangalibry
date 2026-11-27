package com.example.turkids.repository

import android.location.LocationManager
import android.os.Build

// con ubicacion aproximada no hay gps: android 12+ usa fused y antes usa la red
internal fun elegirProveedores(
    sdk: Int,
    tieneUbicacionPrecisa: Boolean,
    habilitado: (String) -> Boolean
): List<String> =
    listOfNotNull(
        LocationManager.FUSED_PROVIDER.takeIf { sdk >= Build.VERSION_CODES.S },
        LocationManager.NETWORK_PROVIDER,
        LocationManager.GPS_PROVIDER.takeIf { tieneUbicacionPrecisa }
    ).filter(habilitado)
