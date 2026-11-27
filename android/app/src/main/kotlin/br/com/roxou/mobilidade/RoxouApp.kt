package br.com.roxou.mobilidade

import android.app.Application
import br.com.roxou.mobilidade.data.RoxouRepository

/** Injeção de dependência manual: uma única instância do repositório para o app todo. */
class RoxouApp : Application() {
    val repository: RoxouRepository by lazy { RoxouRepository.get(this) }

    override fun onCreate() {
        super.onCreate()
        br.com.roxou.mobilidade.overlay.Diag.init(this)
    }
}
