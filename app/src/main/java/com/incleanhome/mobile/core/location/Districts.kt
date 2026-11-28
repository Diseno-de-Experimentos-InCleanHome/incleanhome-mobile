package com.incleanhome.mobile.core.location

import java.text.Normalizer
import java.util.Locale

data class District(val key: String, val name: String)

/**
 * Closed list of districts accepted by the API for US32 and US33 (Lima Metropolitana and Callao).
 * The keys are the canonical ones of incleanhome-backend (DistrictCatalog); the app keeps no
 * coordinates: distances always come from the API. A 400 with validDistricts replaces this list.
 */
object Districts {
    val ALL: List<District> = listOf(
        District("ancon", "Ancón"),
        District("ate", "Ate"),
        District("barranco", "Barranco"),
        District("brena", "Breña"),
        District("carabayllo", "Carabayllo"),
        District("cercado_de_lima", "Cercado de Lima"),
        District("chaclacayo", "Chaclacayo"),
        District("chorrillos", "Chorrillos"),
        District("cieneguilla", "Cieneguilla"),
        District("comas", "Comas"),
        District("el_agustino", "El Agustino"),
        District("independencia", "Independencia"),
        District("jesus_maria", "Jesús María"),
        District("la_molina", "La Molina"),
        District("la_victoria", "La Victoria"),
        District("lince", "Lince"),
        District("los_olivos", "Los Olivos"),
        District("lurigancho", "Lurigancho-Chosica"),
        District("lurin", "Lurín"),
        District("magdalena", "Magdalena del Mar"),
        District("miraflores", "Miraflores"),
        District("pachacamac", "Pachacámac"),
        District("pucusana", "Pucusana"),
        District("pueblo_libre", "Pueblo Libre"),
        District("puente_piedra", "Puente Piedra"),
        District("punta_hermosa", "Punta Hermosa"),
        District("punta_negra", "Punta Negra"),
        District("rimac", "Rímac"),
        District("san_bartolo", "San Bartolo"),
        District("san_borja", "San Borja"),
        District("san_isidro", "San Isidro"),
        District("san_juan_de_lurigancho", "San Juan de Lurigancho"),
        District("san_juan_de_miraflores", "San Juan de Miraflores"),
        District("san_luis", "San Luis"),
        District("san_martin", "San Martín de Porres"),
        District("san_miguel", "San Miguel"),
        District("santa_anita", "Santa Anita"),
        District("santa_maria_del_mar", "Santa María del Mar"),
        District("santa_rosa", "Santa Rosa"),
        District("surco", "Santiago de Surco"),
        District("surquillo", "Surquillo"),
        District("villa_el_salvador", "Villa El Salvador"),
        District("villa_maria_del_triunfo", "Villa María del Triunfo"),
        District("bellavista", "Bellavista"),
        District("callao", "Callao"),
        District("carmen_de_la_legua_reynoso", "Carmen de la Legua Reynoso"),
        District("la_perla", "La Perla"),
        District("la_punta", "La Punta"),
        District("mi_peru", "Mi Perú"),
        District("ventanilla", "Ventanilla")
    ).sortedBy { normalize(it.name) }

    private val byNormalized: Map<String, District> =
        ALL.flatMap { listOf(it.key to it, normalize(it.name) to it) }.toMap()

    /** "  Jesús María " becomes jesus_maria, the same normalization the API applies. */
    fun normalize(text: String): String = Normalizer.normalize(text.trim(), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase(Locale.ROOT)
        .split(Regex("[\\s_-]+"))
        .filter(String::isNotEmpty)
        .joinToString("_")

    fun find(text: String?): District? = text?.takeIf(String::isNotBlank)?.let { byNormalized[normalize(it)] }

    /** Display name of a canonical key; unknown keys are shown as received. */
    fun label(key: String?): String? = key?.let { find(it)?.name ?: it }

    /** Keeps the order of the catalog and adds the keys the API reports that the app does not know. */
    fun fromApi(validKeys: List<String>): List<District> {
        if (validKeys.isEmpty()) return ALL
        return validKeys.map { key -> find(key) ?: District(key, key) }.sortedBy { normalize(it.name) }
    }
}
