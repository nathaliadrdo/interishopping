package com.example.interishopping

import android.os.Bundle
import android.util.Patterns
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class CadastroActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cadastro)

        findViewById<TextView>(R.id.tvVoltar).setOnClickListener { finish() }

        val tilNome = findViewById<TextInputLayout>(R.id.tilNome)
        val tilEmail = findViewById<TextInputLayout>(R.id.tilEmail)
        val tilTelefone = findViewById<TextInputLayout>(R.id.tilTelefone)
        val tilSenha = findViewById<TextInputLayout>(R.id.tilSenha)
        val etNome = findViewById<TextInputEditText>(R.id.etNome)
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etTelefone = findViewById<TextInputEditText>(R.id.etTelefone)
        val etSenha = findViewById<TextInputEditText>(R.id.etSenha)
        val btn = findViewById<MaterialButton>(R.id.btnCriarConta)

        btn.setOnClickListener {
            val nome = etNome.text?.toString()?.trim().orEmpty()
            val email = etEmail.text?.toString()?.trim().orEmpty()
            val telefone = etTelefone.text?.toString()?.trim().orEmpty()
            val senha = etSenha.text?.toString().orEmpty()

            tilNome.error = if (nome.length < 2) getString(R.string.err_nome) else null
            tilEmail.error = if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) getString(R.string.err_email) else null
            tilTelefone.error = if (!Validar.telefone(telefone)) getString(R.string.err_telefone) else null
            tilSenha.error = if (senha.length < 6) getString(R.string.err_senha_curta) else null
            if (listOf(tilNome, tilEmail, tilTelefone, tilSenha).any { it.error != null }) return@setOnClickListener

            btn.isEnabled = false
            ApiClient.service.cadastro(nome, email, telefone.filter { it.isDigit() }, senha).executar(
                ctx = this,
                onOk = {
                    Toast.makeText(this, R.string.msg_conta_criada, Toast.LENGTH_LONG).show()
                    finish()
                },
                onFim = { btn.isEnabled = true }
            )
        }
    }
}
