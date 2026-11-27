package com.example.turkids

import com.example.turkids.repository.elegirProveedores
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain

class ProveedoresUbicacionTest : StringSpec({

    val todosActivos: (String) -> Boolean = { true }

    "con ubicacion aproximada en Android 12 o mas se usan fused y red, nunca GPS" {
        val proveedores = elegirProveedores(sdk = 33, tieneUbicacionPrecisa = false, habilitado = todosActivos)

        proveedores shouldContain "fused"
        proveedores shouldContain "network"
        proveedores shouldNotContain "gps"
    }

    "con ubicacion aproximada en Android 11 se usa la red" {
        elegirProveedores(sdk = 30, tieneUbicacionPrecisa = false, habilitado = todosActivos)
            .shouldContainExactly("network")
    }

    "con ubicacion precisa tambien se usa el GPS" {
        elegirProveedores(sdk = 33, tieneUbicacionPrecisa = true, habilitado = todosActivos)
            .shouldContainExactly("fused", "network", "gps")
    }

    "si la red esta desactivada igual queda fused con ubicacion aproximada" {
        elegirProveedores(sdk = 33, tieneUbicacionPrecisa = false) { it == "fused" }
            .shouldContainExactly("fused")
    }

    "se descartan los proveedores desactivados" {
        elegirProveedores(sdk = 33, tieneUbicacionPrecisa = true) { false }.shouldBeEmpty()
    }
})
