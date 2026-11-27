package br.com.roxou.mobilidade.leitura.service

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.graphics.Rect
import android.view.Display
import androidx.annotation.RequiresApi
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
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

        maybeOcr(packageName, nodes)

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

    // ---------- Leitura por imagem (OCR) do cartão de oferta ----------
    // A tela de oferta da 99 (Flutter) não expõe texto para a acessibilidade.
    // Quando um cartão de oferta aparece, tira um print SÓ em memória, lê o texto
    // no próprio celular (ML Kit, sem internet), descarta a imagem e grava só o texto.

    private val lastOcrTime = mutableMapOf<String, Long>()
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    private fun maybeOcr(packageName: String, nodes: List<NodeInfoItem>) {
        if (Build.VERSION.SDK_INT < 30) return
        val offer = nodes.firstOrNull { n ->
            val id = n.viewIdResourceName ?: ""
            id.endsWith("/primary_touch_area") ||
                id.endsWith("/broad_order_container") ||
                (n.text?.contains("opções de corridas") == true)
        } ?: return

        val now = System.currentTimeMillis()
        if (now - (lastOcrTime[packageName] ?: 0L) < 1500) return
        lastOcrTime[packageName] = now

        // Na Uber o cartão tem limites conhecidos; na 99 lê a tela inteira.
        val crop = if (offer.viewIdResourceName?.endsWith("/primary_touch_area") == true) parseBounds(offer.bounds) else null
        val why = offer.viewIdResourceName?.substringAfterLast('/') ?: "janela_99"
        takeShot(packageName, crop, "OCR:$why")
        handler.postDelayed({ takeShot(packageName, crop, "OCR:$why+1s") }, 1200)
        handler.postDelayed({ takeShot(packageName, crop, "OCR:$why+3s") }, 3000)
    }

    private fun parseBounds(b: String?): Rect? {
        val m = Regex("\\[(-?\\d+),(-?\\d+)]\\[(-?\\d+),(-?\\d+)]").find(b ?: return null) ?: return null
        val (l, t, r, bo) = m.destructured
        return Rect(l.toInt(), t.toInt(), r.toInt(), bo.toInt())
    }

    private fun takeShot(packageName: String, crop: Rect?, label: String) {
        if (Build.VERSION.SDK_INT < 30) return
        takeShotApi30(packageName, crop, label)
    }

    @RequiresApi(30)
    private fun takeShotApi30(packageName: String, crop: Rect?, label: String) {
        try {
            takeScreenshot(Display.DEFAULT_DISPLAY, mainExecutor, object : AccessibilityService.TakeScreenshotCallback {
                override fun onSuccess(result: AccessibilityService.ScreenshotResult) {
                    val hb = result.hardwareBuffer
                    val hw = Bitmap.wrapHardwareBuffer(hb, result.colorSpace)
                    hb.close()
                    if (hw == null) { saveNodes(packageName, "$label FALHA sem_bitmap", emptyList()); return }
                    val full = hw.copy(Bitmap.Config.ARGB_8888, false)
                    hw.recycle()
                    var offX = 0
                    var offY = 0
                    val bmp = if (crop != null) {
                        val l = crop.left.coerceIn(0, full.width - 1)
                        val t = crop.top.coerceIn(0, full.height - 1)
                        val w = (crop.right.coerceAtMost(full.width) - l).coerceAtLeast(1)
                        val h = (crop.bottom.coerceAtMost(full.height) - t).coerceAtLeast(1)
                        offX = l; offY = t
                        Bitmap.createBitmap(full, l, t, w, h).also { if (it != full) full.recycle() }
                    } else full

                    recognizer.process(InputImage.fromBitmap(bmp, 0))
                        .addOnSuccessListener { text ->
                            val lines = mutableListOf<NodeInfoItem>()
                            for (block in text.textBlocks) for (line in block.lines) {
                                val r = line.boundingBox
                                lines.add(
                                    NodeInfoItem(
                                        text = if (prefsManager.maskPhones.value) maskPhoneNumber(line.text) else line.text,
                                        contentDescription = null,
                                        viewIdResourceName = null,
                                        className = "OCR",
                                        bounds = r?.let { "[${it.left + offX},${it.top + offY}][${it.right + offX},${it.bottom + offY}]" },
                                        depth = 0
                                    )
                                )
                            }
                            saveNodes(packageName, if (lines.isEmpty()) "$label VAZIO" else label, lines)
                            bmp.recycle() // a imagem nunca é gravada
                        }
                        .addOnFailureListener { e ->
                            saveNodes(packageName, "$label FALHA ${e.javaClass.simpleName}", emptyList())
                            bmp.recycle()
                        }
                }

                override fun onFailure(errorCode: Int) {
                    saveNodes(packageName, "$label FALHA codigo=$errorCode", emptyList())
                }
            })
        } catch (e: Exception) {
            saveNodes(packageName, "$label FALHA ${e.javaClass.simpleName}", emptyList())
        }
    }

    private fun saveNodes(packageName: String, eventType: String, nodes: List<NodeInfoItem>) {
        val now = System.currentTimeMillis()
        val dateObj = Date(now)
        val json = gson.toJson(nodes)
        val snapshot = SnapshotEntity(
            timestamp = now,
            dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(dateObj),
            timeString = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(dateObj),
            packageName = packageName,
            eventType = eventType,
            nodeDumpJson = json,
            contentHash = json.hashCode()
        )
        serviceScope.launch {
            try { database.snapshotDao().insert(snapshot) } catch (e: Exception) { e.printStackTrace() }
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
