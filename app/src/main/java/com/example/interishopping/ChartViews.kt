package com.example.interishopping

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

/** Converte dp em pixels (os gráficos desenham em pixels). */
private fun View.dp(v: Float) = v * resources.displayMetrics.density

/**
 * Gráfico de barras da semana (Canvas, sem biblioteca externa).
 * dados: 7 valores (segunda..domingo); rotulos: 7 textos; a barra de maior valor fica em verde-escuro.
 */
class BarChartView @JvmOverloads constructor(c: Context, a: AttributeSet? = null) : View(c, a) {
    private var dados: List<Double> = List(7) { 0.0 }
    private var rotulos: List<String> = List(7) { "" }
    private val barra = Paint(Paint.ANTI_ALIAS_FLAG)
    private val texto = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val corForte = ContextCompat.getColor(c, R.color.verde_pinheiro)
    private val corSuave = ContextCompat.getColor(c, R.color.verde_claro_barra)
    private val corTexto = ContextCompat.getColor(c, R.color.texto_secundario)

    fun definir(valores: List<Double>, textos: List<String>) {
        dados = valores; rotulos = textos; invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        texto.textSize = dp(12f); texto.color = corTexto
        val n = dados.size.coerceAtLeast(1)
        val areaTexto = dp(22f)
        val alturaMax = height - areaTexto - dp(6f)
        val maximo = (dados.maxOrNull() ?: 0.0).takeIf { it > 0 } ?: 1.0
        val fatia = width.toFloat() / n
        val largura = fatia * 0.52f
        val idxMax = dados.indices.maxByOrNull { dados[it] } ?: -1
        for (i in 0 until n) {
            val cx = fatia * i + fatia / 2
            val h = (dados[i] / maximo * alturaMax).toFloat().coerceAtLeast(dp(4f))   // mínimo 4dp para a barra aparecer
            barra.color = if (i == idxMax && dados[i] > 0) corForte else corSuave
            val r = RectF(cx - largura / 2, alturaMax - h + dp(6f), cx + largura / 2, alturaMax + dp(6f))
            canvas.drawRoundRect(r, dp(8f), dp(8f), barra)
            canvas.drawText(rotulos.getOrElse(i) { "" }, cx, height - dp(4f), texto)
        }
    }
}

/** Gráfico de rosca (donut) dos pedidos por status; o número total fica no centro. */
class DonutChartView @JvmOverloads constructor(c: Context, a: AttributeSet? = null) : View(c, a) {
    private var valores: List<Int> = emptyList()
    private var cores: List<Int> = emptyList()
    private val arco = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.BUTT }
    private val numero = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val legenda = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val corTrilha = ContextCompat.getColor(c, R.color.borda)
    private val corNumero = ContextCompat.getColor(c, R.color.verde_texto)
    private val corTexto = ContextCompat.getColor(c, R.color.texto_secundario)

    fun definir(v: List<Int>, c: List<Int>) { valores = v; cores = c; invalidate() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val espessura = dp(18f)
        arco.strokeWidth = espessura
        val lado = minOf(width, height) - espessura
        val esq = (width - lado) / 2f; val topo = (height - lado) / 2f
        val oval = RectF(esq, topo, esq + lado, topo + lado)
        val total = valores.sum()
        if (total == 0) {
            arco.color = corTrilha
            canvas.drawArc(oval, 0f, 360f, false, arco)
        } else {
            var inicio = -90f
            valores.forEachIndexed { i, v ->
                if (v <= 0) return@forEachIndexed
                val varredura = v * 360f / total
                arco.color = cores[i]
                canvas.drawArc(oval, inicio, (varredura - 2f).coerceAtLeast(1f), false, arco)   // 2° de folga entre fatias
                inicio += varredura
            }
        }
        numero.color = corNumero; numero.textSize = dp(26f)
        legenda.color = corTexto; legenda.textSize = dp(11f)
        canvas.drawText(total.toString(), width / 2f, height / 2f + dp(6f), numero)
        canvas.drawText(context.getString(R.string.inicio_pedidos_unid), width / 2f, height / 2f + dp(22f), legenda)
    }
}

/**
 * Andamento do pedido: 4 bolinhas ligadas por uma linha.
 * Etapas anteriores = verde com check; etapa atual = açafrão; futuras = vazias.
 */
class StatusStepperView @JvmOverloads constructor(c: Context, a: AttributeSet? = null) : View(c, a) {
    private var atual = 0
    private val rotulos = StatusPedido.values().map { c.getString(it.rotulo) }
    private val linha = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    private val bola = Paint(Paint.ANTI_ALIAS_FLAG)
    private val traco = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private val texto = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val verde = ContextCompat.getColor(c, R.color.verde_pinheiro)
    private val acafrao = ContextCompat.getColor(c, R.color.acafrao)
    private val cinza = ContextCompat.getColor(c, R.color.borda)
    private val tinta = ContextCompat.getColor(c, R.color.tinta)
    private val secundario = ContextCompat.getColor(c, R.color.texto_secundario)
    private val branco = ContextCompat.getColor(c, R.color.sobre_verde)

    fun definir(status: StatusPedido) { atual = status.ordinal; invalidate() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val n = rotulos.size
        val raio = dp(13f)
        val margem = raio + dp(10f)
        val y = dp(18f)
        val passo = (width - 2 * margem) / (n - 1)
        linha.strokeWidth = dp(4f)
        linha.color = cinza
        canvas.drawLine(margem, y, width - margem, y, linha)
        linha.color = verde
        canvas.drawLine(margem, y, margem + passo * atual, y, linha)
        texto.textSize = dp(12f)
        for (i in 0 until n) {
            val x = margem + passo * i
            when {
                i < atual -> {
                    bola.color = verde; canvas.drawCircle(x, y, raio, bola)
                    traco.color = branco; traco.strokeWidth = dp(2.4f)
                    canvas.drawLine(x - dp(6f), y, x - dp(2f), y + dp(4f), traco)   // o "check"
                    canvas.drawLine(x - dp(2f), y + dp(4f), x + dp(6f), y - dp(4f), traco)
                }
                i == atual -> {
                    bola.color = acafrao; canvas.drawCircle(x, y, raio + dp(2f), bola)
                    bola.color = tinta; canvas.drawCircle(x, y, dp(4f), bola)
                }
                else -> {
                    bola.color = ContextCompat.getColor(context, R.color.branco); canvas.drawCircle(x, y, raio, bola)
                    traco.color = cinza; traco.strokeWidth = dp(3f)
                    canvas.drawCircle(x, y, raio - dp(1.5f), traco)
                }
            }
            texto.color = if (i == atual) tinta else secundario
            texto.isFakeBoldText = i == atual
            canvas.drawText(rotulos[i], x, y + raio + dp(22f), texto)
        }
    }

    override fun onMeasure(w: Int, h: Int) {
        setMeasuredDimension(resolveSize(0, w), dp(70f).toInt())
    }
}
