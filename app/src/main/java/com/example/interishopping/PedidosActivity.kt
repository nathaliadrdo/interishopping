package com.example.interishopping

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

/** Lista de pedidos com filtro por status e busca por cliente (GET pedidos.php). */
class PedidosActivity : AppCompatActivity() {
    private lateinit var adapter: PedidoAdapter
    private var filtro = ""        // "" = todos; senão a chave do status (ex.: "pendente")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pedidos)
        Nav.configurar(this, Aba.PEDIDOS)

        adapter = PedidoAdapter(
            aoAbrir = { startActivity(Intent(this, PedidoDetalheActivity::class.java).putExtra("id", it.id)) },
            aoAvisar = { avisar(it) }
        )
        findViewById<RecyclerView>(R.id.rvPedidos).apply {
            layoutManager = LinearLayoutManager(this@PedidosActivity); adapter = this@PedidosActivity.adapter
        }

        findViewById<ChipGroup>(R.id.chipsStatus).setOnCheckedStateChangeListener { _, ids ->
            filtro = when (ids.firstOrNull()) {
                R.id.chipPendente -> StatusPedido.PENDENTE.chave
                R.id.chipProducao -> StatusPedido.EM_PRODUCAO.chave
                R.id.chipACaminho -> StatusPedido.A_CAMINHO.chave
                R.id.chipFinalizado -> StatusPedido.FINALIZADO.chave
                else -> ""
            }
            carregar()
        }
        findViewById<EditText>(R.id.etBusca).setOnEditorActionListener { _, acao, _ ->
            if (acao == EditorInfo.IME_ACTION_SEARCH) carregar()
            true
        }
        findViewById<ExtendedFloatingActionButton>(R.id.fabNovoPedido).setOnClickListener {
            startActivity(Intent(this, NovoPedidoActivity::class.java))
        }
    }

    override fun onResume() { super.onResume(); carregar() }

    private fun carregar() {
        val busca = findViewById<EditText>(R.id.etBusca).text.toString().trim()
        ApiClient.service.listarPedidos(Session.usuarioId(this), filtro, busca).executar(this, onOk = {
            adapter.atualizar(it)
            findViewById<TextView>(R.id.tvVazio).visibility = if (it.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        })
    }

    /** Atalho do card: busca o pedido completo (com itens) e abre o WhatsApp com a mensagem do status atual. */
    private fun avisar(p: PedidoResumo) {
        ApiClient.service.pedido(Session.usuarioId(this), p.id).executar(this, onOk = { det ->
            Whats.abrir(this, det.cliente_telefone, montarMensagem(this, det, StatusPedido.deChave(det.status)))
        })
    }
}
