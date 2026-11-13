package com.example.interishopping

import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup

/**
 * Detalhe do pedido (GET pedido.php): andamento, itens e total.
 * O vendedor escolhe o status nos chips; a mensagem do WhatsApp muda junto (prévia).
 * "Atualizar status" grava no banco (POST atualizar_status.php); "Enviar pelo WhatsApp" abre o app com o texto pronto.
 */
class PedidoDetalheActivity : AppCompatActivity() {
    private var pedido: PedidoDetalhe? = null
    private var statusSalvo = StatusPedido.PENDENTE
    private var statusEscolhido = StatusPedido.PENDENTE

    private val chips = mapOf(
        R.id.chipPendente to StatusPedido.PENDENTE, R.id.chipProducao to StatusPedido.EM_PRODUCAO,
        R.id.chipACaminho to StatusPedido.A_CAMINHO, R.id.chipFinalizado to StatusPedido.FINALIZADO
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pedido_detalhe)
        findViewById<TextView>(R.id.tvVoltar).setOnClickListener { finish() }

        findViewById<ChipGroup>(R.id.chipsStatus).setOnCheckedStateChangeListener { _, ids ->
            statusEscolhido = chips[ids.firstOrNull()] ?: statusEscolhido
            atualizarMensagem()
        }
        findViewById<MaterialButton>(R.id.btnAtualizar).setOnClickListener { gravarStatus() }
        findViewById<MaterialButton>(R.id.btnEnviarWhats).setOnClickListener {
            pedido?.let { Whats.abrir(this, it.cliente_telefone, montarMensagem(this, it, statusEscolhido)) }
        }

        val id = intent.getIntExtra("id", 0)
        ApiClient.service.pedido(Session.usuarioId(this), id).executar(this, onOk = { mostrar(it) })
    }

    private fun mostrar(p: PedidoDetalhe) {
        pedido = p
        statusSalvo = StatusPedido.deChave(p.status); statusEscolhido = statusSalvo
        findViewById<TextView>(R.id.tvAvatar).text = p.cliente_nome.take(1).uppercase()
        findViewById<TextView>(R.id.tvCliente).text = p.cliente_nome
        findViewById<TextView>(R.id.tvSub).text = p.cliente_telefone + " · " + getString(R.string.pedido_codigo, p.id)

        val lista = findViewById<LinearLayout>(R.id.listaItens)
        lista.removeAllViews()
        p.itens.forEach { i ->
            val linha = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 10, 0, 0) }
            linha.addView(TextView(this).apply { text = "${i.nome} × ${i.quantidade}"; textSize = 15f },
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            linha.addView(TextView(this).apply {
                text = Validar.moeda(Validar.numero(i.preco_unitario) * i.quantidade); textSize = 15f; setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            lista.addView(linha)
        }
        val extras = mutableListOf(getString(R.string.detalhe_entrega) + ": " + Validar.moeda(Validar.numero(p.entrega)))
        if (Validar.numero(p.desconto) > 0) extras.add(getString(R.string.desconto) + ": -" + Validar.moeda(Validar.numero(p.desconto)))
        if (!p.pagamento.isNullOrBlank()) extras.add(getString(R.string.detalhe_pagamento, p.pagamento))
        findViewById<TextView>(R.id.tvExtras).text = extras.joinToString("\n")
        findViewById<TextView>(R.id.tvTotal).text = Validar.moeda(Validar.numero(p.total))

        findViewById<ChipGroup>(R.id.chipsStatus).check(chips.entries.first { it.value == statusSalvo }.key)
        findViewById<StatusStepperView>(R.id.stepper).definir(statusSalvo)
        atualizarMensagem()
    }

    private fun atualizarMensagem() {
        val p = pedido ?: return
        findViewById<TextView>(R.id.tvMensagem).text = montarMensagem(this, p, statusEscolhido)
    }

    private fun gravarStatus() {
        val p = pedido ?: return
        if (statusEscolhido == statusSalvo) return
        ApiClient.service.atualizarStatus(Session.usuarioId(this), p.id, statusEscolhido.chave).executar(this, onOk = {
            statusSalvo = statusEscolhido
            findViewById<StatusStepperView>(R.id.stepper).definir(statusSalvo)
            Toast.makeText(this, R.string.msg_status_atualizado, Toast.LENGTH_SHORT).show()
        })
    }
}
