package com.example.interishopping

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Integração com o WhatsApp por link wa.me ("clique para conversar").
 * O WhatsApp abre com a conversa do cliente e a mensagem JÁ ESCRITA; o vendedor só toca em enviar.
 * Não precisa de conta comercial, de chave de API nem de permissão extra no AndroidManifest.
 */
object Whats {
    /** Telefone guardado sem máscara (DDD + 9 dígitos) + código do Brasil (55). */
    fun numeroInternacional(telefone: String): String = "55" + telefone.filter { it.isDigit() }

    /** Monta o link: https://wa.me/5511988881111?text=<mensagem codificada para URL>. */
    fun link(telefone: String, mensagem: String): Uri =
        Uri.parse("https://wa.me/${numeroInternacional(telefone)}?text=" + Uri.encode(mensagem))

    fun abrir(ctx: Context, telefone: String, mensagem: String) {
        try {
            ctx.startActivity(Intent(Intent.ACTION_VIEW, link(telefone, mensagem)))
        } catch (e: ActivityNotFoundException) {
            // Aparelho sem WhatsApp nem navegador para tratar o link.
            Toast.makeText(ctx, R.string.msg_whats_indisponivel, Toast.LENGTH_LONG).show()
        }
    }
}

/** Os 4 status do pedido, na ordem do fluxo. `chave` é o valor guardado no banco (ENUM do MySQL). */
enum class StatusPedido(val chave: String, val rotulo: Int, val mensagemWhats: Int) {
    PENDENTE("pendente", R.string.status_pendente, R.string.msgw_pendente),
    EM_PRODUCAO("em_producao", R.string.status_producao, R.string.msgw_producao),
    A_CAMINHO("a_caminho", R.string.status_a_caminho, R.string.msgw_a_caminho),
    FINALIZADO("finalizado", R.string.status_finalizado, R.string.msgw_finalizado);

    companion object {
        fun deChave(chave: String): StatusPedido = values().firstOrNull { it.chave == chave } ?: PENDENTE
    }
}

/** Texto da mensagem enviada ao cliente: usa o modelo do status (strings.xml, nos 3 idiomas). */
fun montarMensagem(ctx: Context, p: PedidoDetalhe, status: StatusPedido): String {
    val itens = p.itens.joinToString(", ") { "${it.nome} × ${it.quantidade}" }
    return ctx.getString(status.mensagemWhats, p.cliente_nome.substringBefore(' '), p.id, itens, Validar.moeda(Validar.numero(p.total)))
}
