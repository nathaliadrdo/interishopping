package com.example.interishopping

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

class ProdutosActivity : AppCompatActivity() {
    private lateinit var adapter: ProdutoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_produtos)
        Nav.configurar(this, Aba.CATALOGO)

        adapter = ProdutoAdapter(
            aoEditar = {
                startActivity(Intent(this, ProdutoFormActivity::class.java)
                    .putExtra("id", it.id).putExtra("nome", it.nome).putExtra("categoria", it.categoria)
                    .putExtra("preco", it.preco).putExtra("estoque", it.estoque))
            },
            aoExcluir = { confirmarExclusao(it) }
        )
        findViewById<RecyclerView>(R.id.rvProdutos).apply { layoutManager = GridLayoutManager(context, 2); adapter = this@ProdutosActivity.adapter }
        findViewById<EditText>(R.id.etBusca).setOnEditorActionListener { _, acao, _ ->
            if (acao == EditorInfo.IME_ACTION_SEARCH) carregar(); true
        }
        findViewById<ExtendedFloatingActionButton>(R.id.fabNovoProduto).setOnClickListener {
            startActivity(Intent(this, ProdutoFormActivity::class.java))
        }
    }

    override fun onResume() { super.onResume(); carregar() }

    private fun carregar() {
        val busca = findViewById<EditText>(R.id.etBusca).text.toString().trim()
        ApiClient.service.listarProdutos(Session.usuarioId(this), busca).executar(this, onOk = { lista ->
            adapter.atualizar(lista)
            findViewById<TextView>(R.id.tvVazio).visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
            val baixo = lista.count { it.estoque < 5 }
            findViewById<TextView>(R.id.tvResumoCatalogo).text =
                "${lista.size} ${getString(R.string.catalogo_produtos)} · ${lista.sumOf { it.estoque }} ${getString(R.string.catalogo_itens_estoque)} · $baixo ${getString(R.string.catalogo_estoque_baixo)}"
        })
    }

    private fun confirmarExclusao(p: Produto) {
        AlertDialog.Builder(this).setTitle(R.string.dlg_excluir_titulo).setMessage(getString(R.string.dlg_excluir_msg, p.nome))
            .setPositiveButton(R.string.btn_excluir) { _, _ ->
                ApiClient.service.deletarProduto(Session.usuarioId(this), p.id).executar(this, onOk = {
                    Toast.makeText(this, R.string.msg_produto_excluido, Toast.LENGTH_SHORT).show(); carregar()
                })
            }.setNegativeButton(R.string.btn_cancelar, null).show()
    }
}
