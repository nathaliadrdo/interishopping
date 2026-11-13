package com.example.interishopping

import android.app.Activity
import android.content.Intent
import android.widget.TextView
import androidx.core.content.ContextCompat

/** As cinco abas da barra de navegação inferior (include_nav.xml). */
enum class Aba(val viewId: Int, val tela: Class<out Activity>) {
    INICIO(R.id.navInicio, InicioActivity::class.java),
    PEDIDOS(R.id.navPedidos, PedidosActivity::class.java),
    CATALOGO(R.id.navCatalogo, ProdutosActivity::class.java),
    CLIENTES(R.id.navClientes, ClientesActivity::class.java),
    PERFIL(R.id.navPerfil, PerfilActivity::class.java)
}

object Nav {
    /**
     * Pinta a aba ativa e configura o clique das outras.
     * Regra: o Início é a "raiz". Abrir outra aba empilha a tela por cima do Início;
     * voltar para o Início desempilha tudo (CLEAR_TOP). Assim o botão "voltar" do aparelho sempre leva ao Início.
     */
    fun configurar(activity: Activity, ativa: Aba) {
        val verde = ContextCompat.getColor(activity, R.color.verde_texto)
        Aba.values().forEach { aba ->
            val tv = activity.findViewById<TextView>(aba.viewId)
            if (aba == ativa) {
                tv.setTextColor(verde)
                tv.compoundDrawableTintList = android.content.res.ColorStateList.valueOf(verde)
            } else {
                tv.setOnClickListener { ir(activity, ativa, aba) }
            }
        }
    }

    private fun ir(activity: Activity, atual: Aba, destino: Aba) {
        val intent = Intent(activity, destino.tela)
        if (destino == Aba.INICIO) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        } else if (atual != Aba.INICIO) {
            activity.finish()   // troca de aba sem acumular telas na pilha
        }
        activity.startActivity(intent)
    }
}
