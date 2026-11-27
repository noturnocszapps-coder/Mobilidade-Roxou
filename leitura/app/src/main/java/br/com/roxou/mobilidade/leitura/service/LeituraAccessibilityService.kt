package br.com.roxou.mobilidade.leitura.service

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
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

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (!prefsManager.isRecording.value) return

        val packageName = event.packageName?.toString() ?: return
        prefsManager.addRecentPackage(packageName)

        val monitored = prefsManager.monitoredPackages.value
        if (!monitored.contains(packageName)) return

        val now = System.currentTimeMillis()
        val lastTime = lastSnapshotTimeMap[packageName] ?: 0L
        if (now - lastTime < 500) {
            return // Throttling: max 1 snapshot every 500ms per package
        }

        val eventTypeStr = getEventTypeString(event.eventType)
        val nodes = mutableListOf<NodeInfoItem>()

        // 1. Traverse rootInActiveWindow
        try {
            val rootNode = rootInActiveWindow
            if (rootNode != null) {
                traverseNode(rootNode, 0, nodes)
                rootNode.recycle()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Traverse getWindows()
        try {
            val windows = windows
            if (windows != null) {
                for (window in windows) {
                    val root = window.root
                    if (root != null) {
                        traverseNode(root, 0, nodes)
                        root.recycle()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. For notification events, include event text
        if (event.eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            for (text in event.text) {
                if (!text.isNullOrBlank()) {
                    nodes.add(
                        NodeInfoItem(
                            text = text.toString(),
                            contentDescription = "NotificationText",
                            viewIdResourceName = "notification_text",
                            className = "Notification",
                            bounds = null,
                            depth = 0
                        )
                    )
                }
            }
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

        val lastHash = lastContentHashMap[packageName]
        if (lastHash == contentHash) {
            return // Discard identical snapshot to previous
        }

        lastSnapshotTimeMap[packageName] = now
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
        val text = node.text?.toString()
        val desc = node.contentDescription?.toString()
        val viewId = node.viewIdResourceName
        val className = node.className?.toString()

        val rect = Rect()
        node.getBoundsInScreen(rect)
        val boundsStr = "[${rect.left},${rect.top}][${rect.right},${rect.bottom}]"

        if (!text.isNullOrBlank() || !desc.isNullOrBlank() || !viewId.isNullOrBlank()) {
            list.add(
                NodeInfoItem(
                    text = text,
                    contentDescription = desc,
                    viewIdResourceName = viewId,
                    className = className,
                    bounds = boundsStr,
                    depth = depth
                )
            )
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                traverseNode(child, depth + 1, list)
                child.recycle()
            }
        }
    }

    private fun maskPhoneNumber(input: String): String {
        // Regex matching phone number patterns (8 to 15 digits with spaces, dashes, parentheses, optional +)
        val phoneRegex = Regex("\\+?[0-9][0-9\\s\\-\\(\\)]{7,14}[0-9]")
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
        serviceJob.cancel()
    }
}
