package com.example.interishopping

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val tilLogin = findViewById<TextInputLayout>(R.id.tilLogin)
        val tilSenha = findViewById<TextInputLayout>(R.id.tilSenha)
        val etLogin = findViewById<TextInputEditText>(R.id.etLogin)
        val etSenha = findViewById<TextInputEditText>(R.id.etSenha)
        val btnEntrar = findViewById<MaterialButton>(R.id.btnEntrar)

        btnEntrar.setOnClickListener {
            val login = etLogin.text?.toString()?.trim().orEmpty()
            val senha = etSenha.text?.toString().orEmpty()

            tilLogin.error = if (login.isEmpty()) getString(R.string.err_campo_obrigatorio) else null
            tilSenha.error = if (senha.isEmpty()) getString(R.string.err_campo_obrigatorio) else null
            if (login.isEmpty() || senha.isEmpty()) return@setOnClickListener

            btnEntrar.isEnabled = false
            ApiClient.service.login(login, senha).executar(
                ctx = this,
                onOk = { resp ->
                    Session.salvar(this, resp.usuario_id, resp.nome)
                    startActivity(Intent(this, InicioActivity::class.java))
                    finish()
                },
                onFim = { btnEntrar.isEnabled = true }
            )
        }

        findViewById<MaterialButton>(R.id.btnCriarConta).setOnClickListener {
            startActivity(Intent(this, CadastroActivity::class.java))
        }
        findViewById<TextView>(R.id.tvEsqueciSenha).setOnClickListener {
            android.widget.Toast.makeText(this, R.string.msg_em_desenvolvimento, android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
