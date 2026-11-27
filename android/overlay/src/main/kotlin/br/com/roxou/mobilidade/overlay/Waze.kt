package br.com.roxou.mobilidade.overlay

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Abre o mapa escolhido pelo motorista (Waze ou Google Maps) já navegando para o endereço.
 * Se o app não estiver instalado, abre o link no navegador.
 */
object Waze {
    enum class MapApp(val label: String) { WAZE("Waze"), GOOGLE("Google Maps") }

    fun navigate(context: Context, address: String): Boolean {
        val (pkg, appUri, webUri) = when (PanelPrefs.mapApp(context)) {
            MapApp.WAZE -> Triple(
                "com.waze",
                "https://waze.com/ul?q=${Uri.encode(address)}&navigate=yes",
                "https://waze.com/ul?q=${Uri.encode(address)}&navigate=yes",
            )
            MapApp.GOOGLE -> Triple(
                "com.google.android.apps.maps",
                "google.navigation:q=${Uri.encode(address)}",
                "https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(address)}&travelmode=driving",
            )
        }
        fun open(uri: String, withPkg: Boolean) = try {
            val i = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (withPkg) i.setPackage(pkg)
            context.startActivity(i)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
        return open(appUri, true) || open(webUri, false)
    }
}
