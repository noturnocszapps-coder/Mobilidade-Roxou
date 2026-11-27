package br.com.roxou.mobilidade.overlay

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Notificação fixa da corrida aceita, com os botões "Embarque" e "Destino".
 * Assim o motorista abre o mapa a qualquer hora, mesmo depois que a janela sumiu.
 * Os endereços continuam só na memória do serviço de leitura.
 */
object RideNotification {
    private const val CHANNEL = "corrida_atual"
    private const val ID = 4242
    const val ACTION_PICKUP = "br.com.roxou.mobilidade.EMBARQUE"
    const val ACTION_DROPOFF = "br.com.roxou.mobilidade.DESTINO"
    const val ACTION_END = "br.com.roxou.mobilidade.TERMINAR"

    private fun action(c: Context, a: String, code: Int) = PendingIntent.getBroadcast(
        c, code, Intent(c, RideActionReceiver::class.java).setAction(a),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun show(c: Context, subtitle: String) {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = c.getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL, "Corrida atual", NotificationManager.IMPORTANCE_LOW).apply {
                        description = "Botões para abrir o embarque e o destino no mapa"
                        setShowBadge(false)
                    },
                )
            }
        }
        val map = PanelPrefs.mapApp(c).label
        val n = NotificationCompat.Builder(c, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setContentTitle("Corrida aceita · abrir no $map")
            .setContentText(subtitle)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setTimeoutAfter(2 * 60 * 60_000L)
            .addAction(0, "Embarque", action(c, ACTION_PICKUP, 1))
            .addAction(0, "Destino", action(c, ACTION_DROPOFF, 2))
            .addAction(0, "Terminar", action(c, ACTION_END, 3))
            .build()
        runCatching { NotificationManagerCompat.from(c).notify(ID, n) } // sem permissão de notificação: só não aparece
    }

    fun hide(c: Context) = NotificationManagerCompat.from(c).cancel(ID)
}

class RideActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            RideNotification.ACTION_PICKUP -> OfferReaderService.openMapFromOutside(context, toDropoff = false)
            RideNotification.ACTION_DROPOFF -> OfferReaderService.openMapFromOutside(context, toDropoff = true)
            RideNotification.ACTION_END -> RideNotification.hide(context)
        }
    }
}
