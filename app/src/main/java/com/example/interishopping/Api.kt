package com.example.interishopping   // <- troque pelo package do seu projeto

import android.content.Context
import android.widget.Toast
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.Locale
import java.util.concurrent.TimeUnit

// ---------- Modelos (espelham o JSON da API PHP) ----------
data class Produto(
    val id: Int,
    val nome: String,
    val categoria: String?,
    val preco: String,
    val estoque: Int
)

data class LoginResp(val usuario_id: Int, val nome: String)
data class ApiResp(val status: String?, val id: Int?, val error: String?)

// v2: modelos de clientes, pedidos e painel (espelham o JSON de clientes.php, pedidos.php, pedido.php, dashboard.php).
// Valores em dinheiro chegam como String (o PDO devolve DECIMAL como texto) e são convertidos na hora de exibir.
data class Cliente(val id: Int, val nome: String, val telefone: String, val pedidos: Int, val total_gasto: String)
data class PedidoResumo(
    val id: Int, val status: String, val total: String, val criado_em: String,
    val cliente_nome: String, val cliente_telefone: String, val resumo: String
)
data class ItemPedido(val nome: String, val quantidade: Int, val preco_unitario: String)
data class PedidoDetalhe(
    val id: Int, val status: String, val entrega: String, val pagamento: String?, val desconto: String,
    val total: String, val criado_em: String, val cliente_nome: String, val cliente_telefone: String,
    val itens: List<ItemPedido>
)
data class VendaDia(val dia: Int, val total: Double)          // dia: 1 = segunda ... 7 = domingo
data class Dashboard(
    val faturamento_mes: Double, val faturamento_mes_anterior: Double, val pedidos_hoje: Int,
    val a_caminho: Int, val estoque_baixo: Int, val por_status: Map<String, Int>, val vendas_semana: List<VendaDia>
)

// ---------- Rotas da API (pasta interishopping_api no XAMPP) ----------
interface ApiService {
    @FormUrlEncoded
    @POST("cadastro.php")
    fun cadastro(
        @Field("nome") nome: String,
        @Field("email") email: String,
        @Field("telefone") telefone: String,
        @Field("senha") senha: String
    ): Call<ApiResp>

    @FormUrlEncoded
    @POST("login.php")
    fun login(@Field("login") login: String, @Field("senha") senha: String): Call<LoginResp>

    @GET("produtos.php")
    fun listarProdutos(@Query("usuario_id") usuarioId: Int, @Query("q") busca: String): Call<List<Produto>>

    @FormUrlEncoded
    @POST("incluir_produto.php")
    fun incluirProduto(
        @Field("usuario_id") usuarioId: Int,
        @Field("nome") nome: String,
        @Field("categoria") categoria: String,
        @Field("preco") preco: String,
        @Field("estoque") estoque: Int
    ): Call<ApiResp>

    @FormUrlEncoded
    @POST("editar_produto.php")
    fun editarProduto(
        @Field("usuario_id") usuarioId: Int,
        @Field("id") id: Int,
        @Field("nome") nome: String,
        @Field("categoria") categoria: String,
        @Field("preco") preco: String,
        @Field("estoque") estoque: Int
    ): Call<ApiResp>

    @FormUrlEncoded
    @POST("deletar_produto.php")
    fun deletarProduto(@Field("usuario_id") usuarioId: Int, @Field("id") id: Int): Call<ApiResp>

    // ----- v2: painel, clientes e pedidos -----
    @GET("dashboard.php")
    fun dashboard(@Query("usuario_id") usuarioId: Int): Call<Dashboard>

    @GET("clientes.php")
    fun listarClientes(@Query("usuario_id") usuarioId: Int, @Query("q") busca: String): Call<List<Cliente>>

    @GET("pedidos.php")
    fun listarPedidos(
        @Query("usuario_id") usuarioId: Int,
        @Query("status") status: String,
        @Query("q") busca: String
    ): Call<List<PedidoResumo>>

    @GET("pedido.php")
    fun pedido(@Query("usuario_id") usuarioId: Int, @Query("id") id: Int): Call<PedidoDetalhe>

    /** itens = JSON, ex.: [{"produto_id":1,"quantidade":2}]. O total é calculado no servidor. */
    @FormUrlEncoded
    @POST("criar_pedido.php")
    fun criarPedido(
        @Field("usuario_id") usuarioId: Int,
        @Field("cliente_nome") clienteNome: String,
        @Field("cliente_telefone") clienteTelefone: String,
        @Field("entrega") entrega: String,
        @Field("desconto") desconto: String,
        @Field("pagamento") pagamento: String,
        @Field("itens") itens: String
    ): Call<ApiResp>

    @FormUrlEncoded
    @POST("atualizar_status.php")
    fun atualizarStatus(
        @Field("usuario_id") usuarioId: Int,
        @Field("id") id: Int,
        @Field("status") status: String
    ): Call<ApiResp>
}

object ApiClient {
    // Emulador do Android Studio acessa o XAMPP do computador por 10.0.2.2.
    // Em celular real, use o IPv4 do computador (ipconfig), ex.: "http://192.168.0.101/interishopping_api/".
    const val BASE_URL = "http://10.0.2.2/interishopping_api/"

    val service: ApiService by lazy {
        val log = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC } // BASIC: não registra senha
        val client = OkHttpClient.Builder()
            .addInterceptor(log)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(Gson()))
            .build()
            .create(ApiService::class.java)
    }
}

// ---------- Sessão (guarda quem entrou) ----------
object Session {
    private const val PREFS = "interishopping_session"
    fun salvar(ctx: Context, usuarioId: Int, nome: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt("usuario_id", usuarioId).putString("nome", nome).apply()
    }
    fun usuarioId(ctx: Context): Int = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt("usuario_id", 0)
    fun nome(ctx: Context): String = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("nome", "").orEmpty()
    /** Sair da conta: apaga o usuário guardado. */
    fun sair(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
}

// ---------- Validações (regras do item 4.2 da Etapa 2) ----------
object Validar {
    /** DDD + 9 dígitos (11 números; o terceiro é 9). Aceita máscara: (11) 98888-1111. */
    fun telefone(s: String): Boolean {
        val d = s.filter { it.isDigit() }
        return d.length == 11 && d[2] == '9'
    }
    /** Aceita vírgula ou ponto. Retorna null se não for número ou se for negativo. */
    fun valor(s: String): Double? = s.trim().replace(",", ".").toDoubleOrNull()?.takeIf { it >= 0 }
    fun moeda(v: Double): String = "R$ " + String.format(Locale("pt", "BR"), "%.2f", v)
    /** Converte o texto de dinheiro que vem do servidor ("93.00") em número (0 se vier vazio). */
    fun numero(s: String?): Double = s?.toDoubleOrNull() ?: 0.0
}

// ---------- Execução de chamadas com tratamento de erro ----------
fun <T> Response<T>.mensagemErro(ctx: Context): String = when (code()) {
    401 -> ctx.getString(R.string.msg_login_invalido)
    // 409 tem dois significados: pedido sem estoque (v2) ou e-mail/telefone já cadastrado.
    409 -> if (errorBody()?.string().orEmpty().contains("Estoque", ignoreCase = true))
        ctx.getString(R.string.msg_estoque_insuficiente) else ctx.getString(R.string.msg_email_existente)
    400 -> ctx.getString(R.string.msg_dados_invalidos)
    else -> ctx.getString(R.string.msg_erro_servidor, code())
}

fun <T> Call<T>.executar(ctx: Context, onOk: (T) -> Unit, onFim: () -> Unit = {}) {
    enqueue(object : Callback<T> {
        override fun onResponse(call: Call<T>, response: Response<T>) {
            onFim()
            val corpo = response.body()
            if (response.isSuccessful && corpo != null) onOk(corpo)
            else Toast.makeText(ctx, response.mensagemErro(ctx), Toast.LENGTH_LONG).show()
        }

        override fun onFailure(call: Call<T>, t: Throwable) {
            onFim()
            Toast.makeText(ctx, ctx.getString(R.string.msg_sem_conexao), Toast.LENGTH_LONG).show()
        }
    })
}
