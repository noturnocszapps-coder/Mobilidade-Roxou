package br.com.roxou.mobilidade.leitura.service

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import br.com.roxou.mobilidade.leitura.data.AppDatabase
import br.com.roxou.mobilidade.leitura.data.NodeInfoItem
import br.com.roxou.mobilidade.leitura.data.PreferencesManager
import br.com.roxou.mobilidade.leitura.data.SnapshotEntity
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LeituraAccessibilityService : AccessibilityService() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private lateinit var prefsManager: PreferencesManager
    private lateinit var database: AppDatabase
    private val gson = Gson()

    private val lastSnapshotTimeMap = mutableMapOf<String, Long>()
    private val lastContentHashMap = mutableMapOf<String, Int>()

    override fun onCreate() {
        super.onCreate()
        prefsManager = PreferencesManager.get(this)
        database = AppDatabase.getDatabase(this)
    }

    private val handler = Handler(Looper.getMainLooper())
    private val pendingCapture = mutableSetOf<String>()

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (!prefsManager.isRecording.value) return

        val packageName = event.packageName?.toString() ?: return
        prefsManager.addRecentPackage(packageName)

        val monitored = prefsManager.monitoredPackages.value
        if (!monitored.contains(packageName)) return

        val eventTypeStr = getEventTypeString(event.eventType)
        val notificationTexts =
            if (event.eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED)
                event.text.filter { !it.isNullOrBlank() }.map { it.toString() }
            else emptyList()

        val now = System.currentTimeMillis()
        val lastTime = lastSnapshotTimeMap[packageName] ?: 0L
        if (now - lastTime < 500 && notificationTexts.isEmpty()) {
            // Em vez de descartar, agenda uma nova leitura logo depois:
            // o cartão de oferta costuma terminar de desenhar alguns ms após o primeiro evento.
            if (pendingCapture.add(packageName)) {
                handler.postDelayed({
                    pendingCapture.remove(packageName)
                    capture(packageName, "$eventTypeStr+DELAYED", emptyList())
                }, 600)
            }
            return
        }
        capture(packageName, eventTypeStr, notificationTexts)
    }

    private fun capture(packageName: String, eventTypeStr: String, notificationTexts: List<String>) {
        val monitored = prefsManager.monitoredPackages.value
        val nodes = mutableListOf<NodeInfoItem>()

        // Só as janelas dos apps monitorados (antes entravam chat, barra de status e outros apps).
        try {
            var found = false
            windows?.forEach { window ->
                val root = window.root ?: return@forEach
                if ((root.packageName?.toString() ?: "") in monitored) {
                    found = true
                    traverseNode(root, 0, nodes)
                }
            }
            if (!found) {
                rootInActiveWindow?.let { root ->
                    if ((root.packageName?.toString() ?: "") in monitored) traverseNode(root, 0, nodes)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        for (text in notificationTexts) {
            nodes.add(
                NodeInfoItem(
                    text = text,
                    contentDescription = "NotificationText",
                    viewIdResourceName = "notification_text",
                    className = "Notification",
                    bounds = null,
                    depth = 0
                )
            )
        }

        if (nodes.isEmpty()) return

        val maskPhones = prefsManager.maskPhones.value
        val processedNodes = if (maskPhones) {
            nodes.map { node ->
                node.copy(
                    text = node.text?.let { maskPhoneNumber(it) },
                    contentDescription = node.contentDescription?.let { maskPhoneNumber(it) }
                )
            }
        } else {
            nodes
        }

        val nodeJson = gson.toJson(processedNodes)
        val contentHash = nodeJson.hashCode()

        val now = System.currentTimeMillis()
        lastSnapshotTimeMap[packageName] = now
        if (lastContentHashMap[packageName] == contentHash) return
        lastContentHashMap[packageName] = contentHash

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        val dateObj = Date(now)

        val snapshot = SnapshotEntity(
            timestamp = now,
            dateString = dateFormat.format(dateObj),
            timeString = timeFormat.format(dateObj),
            packageName = packageName,
            eventType = eventTypeStr,
            nodeDumpJson = nodeJson,
            contentHash = contentHash
        )

        serviceScope.launch {
            try {
                database.snapshotDao().insert(snapshot)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun traverseNode(node: AccessibilityNodeInfo, depth: Int, list: MutableList<NodeInfoItem>) {
        if (list.size >= 3000 || depth > 80) return

        val rect = Rect()
        node.getBoundsInScreen(rect)

        val extras = mutableListOf<String>()
        node.hintText?.let { extras.add("hint=$it") }
        if (Build.VERSION.SDK_INT >= 28) {
            node.paneTitle?.let { extras.add("pane=$it") }
            node.tooltipText?.let { extras.add("tooltip=$it") }
        }
        if (Build.VERSION.SDK_INT >= 30) {
            node.stateDescription?.let { extras.add("state=$it") }
        }

        // Grava TODOS os nós (mesmo sem texto), para enxergar a estrutura do cartão de oferta.
        list.add(
            NodeInfoItem(
                text = node.text?.toString(),
                contentDescription = node.contentDescription?.toString(),
                viewIdResourceName = node.viewIdResourceName,
                className = node.className?.toString(),
                bounds = "[${rect.left},${rect.top}][${rect.right},${rect.bottom}]",
                depth = depth,
                childCount = node.childCount,
                extra = extras.takeIf { it.isNotEmpty() }?.joinToString("; ")
            )
        )

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            traverseNode(child, depth + 1, list)
        }
    }

    private fun maskPhoneNumber(input: String): String {
        // Regex matching phone number patterns (8 to 15 digits with spaces, dashes, parentheses, optional +)
        val phoneRegex = Regex("(?<![\\d:/.,])(\\+?55\\s?)?(\\(?\\d{2}\\)?\\s?)?9?\\d{4}[-\\s]?\\d{4}(?![\\d:/.,])")
        return phoneRegex.replace(input) { "***" }
    }

    private fun getEventTypeString(type: Int): String {
        return when (type) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> "WINDOW_STATE"
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> "CONTENT_CHANGED"
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> "NOTIFICATION"
            else -> "EVENT_$type"
        }
    }

    override fun onInterrupt() {
        // Required override
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        serviceJob.cancel()
    }
}
