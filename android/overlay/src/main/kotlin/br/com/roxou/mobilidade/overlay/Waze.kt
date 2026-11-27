package br.com.roxou.mobilidade.overlay

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/** Abre o Waze já navegando para o endereço. Se o Waze não estiver instalado, abre o link no navegador. */
object Waze {
    fun navigate(context: Context, address: String): Boolean {
        val uri = Uri.parse("https://waze.com/ul?q=${Uri.encode(address)}&navigate=yes")
        val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(Intent(intent).setPackage("com.waze"))
            true
        } catch (_: ActivityNotFoundException) {
            try {
                context.startActivity(intent)
                true
            } catch (_: ActivityNotFoundException) {
                false
            }
        }
    }
}
