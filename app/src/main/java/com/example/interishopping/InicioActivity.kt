package com.example.interishopping

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class InicioActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inicio)
        Nav.configurar(this, Aba.INICIO)

        val nome = Session.nome(this).substringBefore(' ')
        findViewById<TextView>(R.id.tvSaudacao).text = getString(R.string.saudacao, nome)
        findViewById<TextView>(R.id.tvAvatar).text = nome.take(1).uppercase()
        findViewById<TextView>(R.id.tvAvatar).setOnClickListener { startActivity(android.content.Intent(this, PerfilActivity::class.java)) }
    }

    override fun onResume() {
        super.onResume()
        ApiClient.service.dashboard(Session.usuarioId(this)).executar(this, onOk = { mostrar(it) })
    }

    private fun mostrar(d: Dashboard) {
        findViewById<TextView>(R.id.tvFaturamento).text = Validar.moeda(d.faturamento_mes)
        findViewById<TextView>(R.id.tvPedidosHoje).text = d.pedidos_hoje.toString()
        findViewById<TextView>(R.id.tvACaminho).text = d.a_caminho.toString()
        findViewById<TextView>(R.id.tvEstoqueBaixoInicio).text = d.estoque_baixo.toString()

        val variacao = findViewById<TextView>(R.id.tvVariacao)
        if (d.faturamento_mes_anterior > 0) {
            val pct = (d.faturamento_mes - d.faturamento_mes_anterior) / d.faturamento_mes_anterior * 100
            variacao.text = getString(R.string.inicio_vs_anterior, String.format("%+.0f%%", pct))
        } else variacao.setText(R.string.inicio_sem_comparacao)

        val porDia = d.vendas_semana.associate { it.dia to it.total }
        val rotulos = listOf(R.string.dia_seg, R.string.dia_ter, R.string.dia_qua, R.string.dia_qui, R.string.dia_sex, R.string.dia_sab, R.string.dia_dom)
            .map { getString(it) }
        findViewById<BarChartView>(R.id.graficoSemana).definir((1..7).map { porDia[it] ?: 0.0 }, rotulos)

        val cores = listOf(R.color.acafrao, R.color.azul_producao, R.color.verde_medio, R.color.verde_pinheiro).map { ContextCompat.getColor(this, it) }
        val qtds = StatusPedido.values().map { d.por_status[it.chave] ?: 0 }
        findViewById<DonutChartView>(R.id.graficoStatus).definir(qtds, cores)
        listOf(R.id.legPendente, R.id.legProducao, R.id.legACaminho, R.id.legFinalizado).forEachIndexed { i, id ->
            val tv = findViewById<TextView>(id)
            tv.text = "${getString(StatusPedido.values()[i].rotulo)}: ${qtds[i]}"
            val bolinha = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(cores[i]); setSize(dpPx(10), dpPx(10)) }
            tv.setCompoundDrawablesRelativeWithIntrinsicBounds(bolinha, null, null, null)
        }
    }

    private fun dpPx(v: Int) = (v * resources.displayMetrics.density).toInt()
}
