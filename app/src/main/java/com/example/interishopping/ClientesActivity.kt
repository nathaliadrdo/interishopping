package com.example.interishopping

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

/** Clientes (GET clientes.php). O botão de WhatsApp abre a conversa sem mensagem pronta. */
class ClientesActivity : AppCompatActivity() {
    private val adapter = ClienteAdapter { Whats.abrir(this, it.telefone, "") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_clientes)
        Nav.configurar(this, Aba.CLIENTES)
        findViewById<RecyclerView>(R.id.rvClientes).apply { layoutManager = LinearLayoutManager(context); adapter = this@ClientesActivity.adapter }
        findViewById<EditText>(R.id.etBusca).setOnEditorActionListener { _, acao, _ ->
            if (acao == EditorInfo.IME_ACTION_SEARCH) carregar(); true
        }
    }

    override fun onResume() { super.onResume(); carregar() }

    private fun carregar() {
        val busca = findViewById<EditText>(R.id.etBusca).text.toString().trim()
        ApiClient.service.listarClientes(Session.usuarioId(this), busca).executar(this, onOk = {
            adapter.atualizar(it)
            findViewById<TextView>(R.id.tvVazio).visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
        })
    }
}

class ClienteAdapter(private val aoChamar: (Cliente) -> Unit) : RecyclerView.Adapter<ClienteAdapter.VH>() {
    private var itens: List<Cliente> = emptyList()
    fun atualizar(novos: List<Cliente>) { itens = novos; notifyDataSetChanged() }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val inicial: TextView = v.findViewById(R.id.tvInicial)
        val nome: TextView = v.findViewById(R.id.tvNome)
        val resumo: TextView = v.findViewById(R.id.tvResumo)
        val whats: MaterialButton = v.findViewById(R.id.btnWhats)
    }

    override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_cliente, p, false))
    override fun getItemCount() = itens.size
    override fun onBindViewHolder(h: VH, i: Int) {
        val c = itens[i]
        h.inicial.text = c.nome.take(1).uppercase()
        h.nome.text = c.nome
        h.resumo.text = h.itemView.context.getString(R.string.cliente_resumo, c.pedidos, Validar.moeda(Validar.numero(c.total_gasto)))
        h.whats.setOnClickListener { aoChamar(c) }
    }
}
