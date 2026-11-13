package com.example.interishopping

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.Locale

class ProdutoFormActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_produto_form)

        findViewById<TextView>(R.id.tvVoltar).setOnClickListener { finish() }

        val id = intent.getIntExtra("id", 0)
        val tilNome = findViewById<TextInputLayout>(R.id.tilNomeProduto)
        val tilPreco = findViewById<TextInputLayout>(R.id.tilPreco)
        val tilEstoque = findViewById<TextInputLayout>(R.id.tilEstoque)
        val etNome = findViewById<TextInputEditText>(R.id.etNomeProduto)
        val etCategoria = findViewById<TextInputEditText>(R.id.etCategoria)
        val etPreco = findViewById<TextInputEditText>(R.id.etPreco)
        val etEstoque = findViewById<TextInputEditText>(R.id.etEstoque)
        val btn = findViewById<MaterialButton>(R.id.btnSalvarProduto)

        if (id > 0) {
            findViewById<TextView>(R.id.tvTitulo).setText(R.string.titulo_editar_produto)
            etNome.setText(intent.getStringExtra("nome"))
            etCategoria.setText(intent.getStringExtra("categoria"))
            etPreco.setText(intent.getStringExtra("preco"))
            etEstoque.setText(intent.getIntExtra("estoque", 0).toString())
        }

        btn.setOnClickListener {
            val nome = etNome.text?.toString()?.trim().orEmpty()
            val categoria = etCategoria.text?.toString()?.trim().orEmpty()
            val preco = Validar.valor(etPreco.text?.toString().orEmpty())
            val estoque = etEstoque.text?.toString()?.trim()?.toIntOrNull()?.takeIf { it >= 0 }

            tilNome.error = if (nome.isEmpty()) getString(R.string.err_nome_produto) else null
            tilPreco.error = if (preco == null) getString(R.string.err_preco) else null
            tilEstoque.error = if (estoque == null) getString(R.string.err_estoque) else null
            if (nome.isEmpty() || preco == null || estoque == null) return@setOnClickListener

            val precoApi = String.format(Locale.US, "%.2f", preco)
            val uid = Session.usuarioId(this)
            val chamada = if (id == 0) ApiClient.service.incluirProduto(uid, nome, categoria, precoApi, estoque)
            else ApiClient.service.editarProduto(uid, id, nome, categoria, precoApi, estoque)

            btn.isEnabled = false
            chamada.executar(
                ctx = this,
                onOk = {
                    Toast.makeText(this, R.string.msg_produto_salvo, Toast.LENGTH_SHORT).show()
                    finish()
                },
                onFim = { btn.isEnabled = true }
            )
        }
    }
}
