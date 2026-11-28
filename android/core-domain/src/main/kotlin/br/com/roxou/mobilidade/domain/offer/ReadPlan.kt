package br.com.roxou.mobilidade.domain.offer

/**
 * Quando ler a tela de novo enquanto um "cartão" do app de motorista está na tela.
 *
 * Causa raiz das ofertas perdidas durante a corrida (27/11): o ID que marca o cartão também
 * aparece na tela da corrida e na tela de "Procurando viagens" da Uber. O app achava que era
 * uma oferta, lia 8 vezes, não achava valor e DESISTIA até o ID sumir. Como o ID não sumia,
 * a oferta nova que chegava por cima nunca era lida. Agora o app nunca desiste: depois das
 * tentativas rápidas, continua lendo de tempos em tempos.
 */
object ReadPlan {
    enum class Action { WAIT, FIRST_TRIES, GIVE_UP_NOTICE, REREAD, KEEP_WATCHING }

    const val QUICK_TRIES = 8

    fun decide(
        offerRead: Boolean,
        tries: Int,
        msSinceRead: Long,
        rereads: Int,
        msSinceLastShot: Long,
        recentWindowChange: Boolean,
    ): Action {
        val interval = if (recentWindowChange) 1_500L else 3_000L
        return when {
            !offerRead && tries < QUICK_TRIES -> Action.FIRST_TRIES
            !offerRead && tries == QUICK_TRIES -> Action.GIVE_UP_NOTICE
            offerRead && msSinceRead > 3_000 && rereads < 10 -> Action.REREAD
            msSinceLastShot > interval -> Action.KEEP_WATCHING
            else -> Action.WAIT
        }
    }
}
