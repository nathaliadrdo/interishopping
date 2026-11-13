package com.example.interishopping

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

/** Grade de produtos. Estoque abaixo de 5 destaca a barra e o texto em vermelho (mesma regra do dashboard.php). */
class ProdutoAdapter(
    private val aoEditar: (Produto) -> Unit,
    private val aoExcluir: (Produto) -> Unit
) : RecyclerView.Adapter<ProdutoAdapter.VH>() {

    private var itens: List<Produto> = emptyList()
    fun atualizar(novos: List<Produto>) { itens = novos; notifyDataSetChanged() }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val inicial: TextView = v.findViewById(R.id.tvInicial)
        val nome: TextView = v.findViewById(R.id.tvNome)
        val preco: TextView = v.findViewById(R.id.tvPreco)
        val barra: ProgressBar = v.findViewById(R.id.barraEstoque)
        val estoque: TextView = v.findViewById(R.id.tvEstoque)
        val editar: TextView = v.findViewById(R.id.btnEditar)
        val excluir: TextView = v.findViewById(R.id.btnExcluir)
    }

    override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_produto, p, false))
    override fun getItemCount() = itens.size

    override fun onBindViewHolder(h: VH, i: Int) {
        val p = itens[i]
        val ctx = h.itemView.context
        h.inicial.text = p.nome.take(1).uppercase()
        h.nome.text = p.nome
        h.preco.text = Validar.moeda(Validar.numero(p.preco))
        h.estoque.text = ctx.getString(R.string.estoque_un, p.estoque)
        h.barra.progress = p.estoque.coerceAtMost(h.barra.max)
        val baixo = p.estoque < 5
        val cor = ContextCompat.getColor(ctx, if (baixo) R.color.alerta else R.color.verde_medio)
        h.barra.progressTintList = android.content.res.ColorStateList.valueOf(cor)
        h.estoque.setTextColor(if (baixo) cor else ContextCompat.getColor(ctx, R.color.texto_secundario))
        h.editar.setOnClickListener { aoEditar(p) }
        h.excluir.setOnClickListener { aoExcluir(p) }
    }
}
