package com.example.interishopping

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

/** Lista de cards de pedidos. Toque no card = detalhe; botão = abre o WhatsApp já com a mensagem. */
class PedidoAdapter(
    private val aoAbrir: (PedidoResumo) -> Unit,
    private val aoAvisar: (PedidoResumo) -> Unit
) : RecyclerView.Adapter<PedidoAdapter.VH>() {

    private var itens: List<PedidoResumo> = emptyList()

    fun atualizar(novos: List<PedidoResumo>) { itens = novos; notifyDataSetChanged() }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val cliente: TextView = v.findViewById(R.id.tvCliente)
        val status: TextView = v.findViewById(R.id.tvStatus)
        val resumo: TextView = v.findViewById(R.id.tvResumo)
        val total: TextView = v.findViewById(R.id.tvTotal)
        val avisar: MaterialButton = v.findViewById(R.id.btnAvisar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_pedido, parent, false))

    override fun getItemCount() = itens.size

    override fun onBindViewHolder(h: VH, position: Int) {
        val p = itens[position]
        val ctx = h.itemView.context
        h.cliente.text = ctx.getString(R.string.pedido_codigo, p.id) + " · " + p.cliente_nome
        h.resumo.text = p.resumo
        h.total.text = Validar.moeda(Validar.numero(p.total))
        estilizarStatus(h.status, StatusPedido.deChave(p.status))
        h.itemView.setOnClickListener { aoAbrir(p) }
        h.avisar.setOnClickListener { aoAvisar(p) }
    }
}

/** Texto + cor do "selo" de status (fundo e texto mudam por status). Usado na lista e no detalhe. */
fun estilizarStatus(tv: TextView, s: StatusPedido) {
    val ctx = tv.context
    tv.setText(s.rotulo)
    val (fundo, texto) = when (s) {
        StatusPedido.PENDENTE -> R.drawable.bg_pill_amber to R.color.ambar_texto
        StatusPedido.EM_PRODUCAO -> R.drawable.bg_pill_blue to R.color.azul_texto
        StatusPedido.A_CAMINHO, StatusPedido.FINALIZADO -> R.drawable.bg_pill_mint to R.color.verde_texto
    }
    tv.setBackgroundResource(fundo)
    tv.setTextColor(ContextCompat.getColor(ctx, texto))
}
