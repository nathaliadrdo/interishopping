package com.example.interishopping

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

/** Perfil: nome da conta, servidor configurado (ApiClient.BASE_URL) e "Sair da conta". */
class PerfilActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)
        Nav.configurar(this, Aba.PERFIL)

        val nome = Session.nome(this)
        findViewById<TextView>(R.id.tvAvatar).text = nome.take(1).uppercase()
        findViewById<TextView>(R.id.tvNome).text = nome
        findViewById<TextView>(R.id.tvServidor).text = getString(R.string.perfil_servidor, ApiClient.BASE_URL)

        findViewById<MaterialButton>(R.id.btnSair).setOnClickListener {
            Session.sair(this)
            // Limpa toda a pilha de telas para que "voltar" não retorne a uma tela de quem saiu.
            startActivity(Intent(this, LoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        }
    }
}
