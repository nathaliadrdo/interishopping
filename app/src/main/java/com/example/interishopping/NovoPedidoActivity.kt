package com.example.interishopping

import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * Novo pedido. O app mostra o total estimado, mas quem calcula o total oficial é o servidor
 * (criar_pedido.php), com os preços do banco: o app não manda valor de total.
 */
class NovoPedidoActivity : AppCompatActivity() {
    private var catalogo: List<Produto> = emptyList()
    private val itens = mutableListOf<Pair<Produto, Int>>()     // produto + quantidade

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_novo_pedido)
        findViewById<TextView>(R.id.tvVoltar).setOnClickListener { finish() }

        // Carrega o catálogo uma vez, para o diálogo "Adicionar item".
        ApiClient.service.listarProdutos(Session.usuarioId(this), "").executar(this, onOk = { catalogo = it })

        findViewById<MaterialButton>(R.id.btnAdicionarItem).setOnClickListener { escolherProduto() }
        findViewById<TextInputEditText>(R.id.etEntrega).doAfterTextChanged { atualizarTotais() }
        findViewById<TextInputEditText>(R.id.etDesconto).doAfterTextChanged { atualizarTotais() }
        findViewById<MaterialButton>(R.id.btnSalvarPedido).setOnClickListener { salvar() }
        atualizarTotais()
    }

    /** 1º passo do diálogo: lista de produtos com estoque. */
    private fun escolherProduto() {
        val disponiveis = catalogo.filter { it.estoque > 0 }
        if (disponiveis.isEmpty()) { Toast.makeText(this, R.string.msg_cadastre_produtos, Toast.LENGTH_LONG).show(); return }
        AlertDialog.Builder(this).setTitle(R.string.dlg_escolher_produto)
            .setItems(disponiveis.map { "${it.nome} · ${Validar.moeda(Validar.numero(it.preco))}" }.toTypedArray()) { _, i ->
                pedirQuantidade(disponiveis[i])
            }.show()
    }

    /** 2º passo: quantidade (inteiro > 0). */
    private fun pedirQuantidade(p: Produto) {
        val campo = EditText(this).apply { inputType = InputType.TYPE_CLASS_NUMBER; hint = getString(R.string.hint_quantidade); setText("1") }
        val caixa = LinearLayout(this).apply { setPadding(56, 24, 56, 0); addView(campo, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) }
        AlertDialog.Builder(this).setTitle(getString(R.string.dlg_quantidade, p.nome)).setView(caixa)
            .setPositiveButton(R.string.btn_adicionar) { _, _ ->
                val q = campo.text.toString().trim().toIntOrNull()
                if (q == null || q <= 0) { Toast.makeText(this, R.string.err_quantidade, Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                val i = itens.indexOfFirst { it.first.id == p.id }
                if (i >= 0) itens[i] = p to (itens[i].second + q) else itens.add(p to q)   // mesmo produto soma a quantidade
                redesenharItens()
            }.setNegativeButton(R.string.btn_cancelar, null).show()
    }

    private fun redesenharItens() {
        val lista = findViewById<LinearLayout>(R.id.listaItens)
        lista.removeAllViews()
        itens.forEachIndexed { idx, (p, q) ->
            val linha = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; orientation = LinearLayout.HORIZONTAL; minimumHeight = 48 * 3 }
            val texto = TextView(this).apply {
                text = "${p.nome} × $q — " + Validar.moeda(Validar.numero(p.preco) * q); textSize = 15f
            }
            val remover = TextView(this).apply {
                text = "✕"; textSize = 18f; gravity = Gravity.CENTER; contentDescription = getString(R.string.cd_remover_item)
                setPadding(32, 16, 32, 16); setOnClickListener { itens.removeAt(idx); redesenharItens() }
            }
            linha.addView(texto, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            linha.addView(remover)
            lista.addView(linha)
        }
        atualizarTotais()
    }

    private fun valorCampo(id: Int): Double = Validar.valor(findViewById<EditText>(id).text.toString()) ?: 0.0

    private fun atualizarTotais() {
        val sub = itens.sumOf { Validar.numero(it.first.preco) * it.second }
        val total = (sub + valorCampo(R.id.etEntrega) - valorCampo(R.id.etDesconto)).coerceAtLeast(0.0)
        findViewById<TextView>(R.id.tvSubtotal).text = getString(R.string.subtotal) + ": " + Validar.moeda(sub)
        findViewById<TextView>(R.id.tvTotal).text = getString(R.string.total) + ": " + Validar.moeda(total)
    }

    private fun salvar() {
        val nome = findViewById<EditText>(R.id.etCliente).text.toString().trim()
        val whats = findViewById<EditText>(R.id.etWhats).text.toString().trim()
        val entregaTxt = findViewById<EditText>(R.id.etEntrega).text.toString().trim()
        val descontoTxt = findViewById<EditText>(R.id.etDesconto).text.toString().trim()
        val entrega = if (entregaTxt.isEmpty()) 0.0 else Validar.valor(entregaTxt)
        val desconto = if (descontoTxt.isEmpty()) 0.0 else Validar.valor(descontoTxt)

        findViewById<TextInputLayout>(R.id.tilCliente).error = if (nome.isEmpty()) getString(R.string.err_nome) else null
        findViewById<TextInputLayout>(R.id.tilWhats).error = if (!Validar.telefone(whats)) getString(R.string.err_telefone) else null
        findViewById<TextInputLayout>(R.id.tilEntrega).error = if (entrega == null) getString(R.string.err_valor_invalido) else null
        findViewById<TextInputLayout>(R.id.tilDesconto).error = if (desconto == null) getString(R.string.err_valor_invalido) else null
        if (nome.isEmpty() || !Validar.telefone(whats) || entrega == null || desconto == null) return
        if (itens.isEmpty()) { Toast.makeText(this, R.string.err_sem_itens, Toast.LENGTH_SHORT).show(); return }

        val pagamento = when (findViewById<ChipGroup>(R.id.chipsPagamento).checkedChipId) {
            R.id.chipDinheiro -> getString(R.string.pag_dinheiro)
            R.id.chipCartao -> getString(R.string.pag_cartao)
            else -> getString(R.string.pag_pix)
        }
        // Só id e quantidade vão ao servidor; o preço vem do banco.
        val json = JSONArray().apply { itens.forEach { (p, q) -> put(JSONObject().put("produto_id", p.id).put("quantidade", q)) } }.toString()

        val botao = findViewById<MaterialButton>(R.id.btnSalvarPedido)
        botao.isEnabled = false
        ApiClient.service.criarPedido(
            Session.usuarioId(this), nome, whats.filter { it.isDigit() },
            String.format(Locale.US, "%.2f", entrega), String.format(Locale.US, "%.2f", desconto), pagamento, json
        ).executar(this, onOk = {
            Toast.makeText(this, R.string.msg_pedido_salvo, Toast.LENGTH_SHORT).show()
            finish()
        }, onFim = { botao.isEnabled = true })
    }
}
