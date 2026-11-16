package com.nexus.seeduque.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.seeduque.database.*
import com.nexus.seeduque.repository.SeEduqueRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.roundToInt

data class InfoBloqueioTarefa(
    val bloqueada: Boolean = false,
    val titulosBloqueadores: List<String> = emptyList(),
    val temDependencias: Boolean = false,
    val ehGargalo: Boolean = false,
    val qtdSucessorasTravadas: Int = 0,
    val pertenceCaminhoCritico: Boolean = false
)

data class ProjecaoProjetoNivel2(
    val projeto: Projeto,
    val statusProjecao: String, // "nivel0", "concluido", "projetado"
    val motivoNivel0: String = "",
    val dataMelhorCaso: String = "",
    val dataEsperada: String = "",
    val dataHistorica: String = "",
    val emRiscoPrazo: Boolean = false,
    val horasRestantes: Double = 0.0,
    val horasCaminhoCritico: Double = 0.0,
    val nosCaminhoCritico: Int = 0,
    val tarefasSemEstimativa: Int = 0,
    val totalPendentes: Int = 0,
    val taxaIncompletudePct: Int = 0,
    val fatorCalibracao: Double = 1.0
)

class MainViewModel(private val repository: SeEduqueRepository) : ViewModel() {

    private val _areaFiltroId = MutableStateFlow<String?>(null)
    val areaFiltroId: StateFlow<String?> = _areaFiltroId.asStateFlow()

    private val _mensagemToast = MutableStateFlow<String?>(null)
    val mensagemToast: StateFlow<String?> = _mensagemToast.asStateFlow()

    private val _tarefaEmFoco = MutableStateFlow<Tarefa?>(null)
    val tarefaEmFoco: StateFlow<Tarefa?> = _tarefaEmFoco.asStateFlow()

    private val _segundosFoco = MutableStateFlow(0L)
    val segundosFoco: StateFlow<Long> = _segundosFoco.asStateFlow()

    private var inicioFocoMs: Long = 0L
    private var timerJob: Job? = null

    val areas: StateFlow<List<Area>> = repository.observarAreas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val todosProjetos = repository.observarProjetos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val todasTarefas = repository.observarTarefas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val todosObjetivos = repository.observarObjetivos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val todosMarcos = repository.observarMarcos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dependencias: StateFlow<List<Dependencia>> = repository.observarDependencias()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val disponibilidade: StateFlow<List<Disponibilidade>> = repository.observarDisponibilidade()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sessoes: StateFlow<List<Sessao>> = repository.observarSessoes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val revisoes: StateFlow<List<Revisao>> = repository.observarRevisoes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val retrospectivas: StateFlow<List<Retrospectiva>> = repository.observarRetrospectivas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projetos: StateFlow<List<Projeto>> = combine(todosProjetos, _areaFiltroId) { projs, areaId ->
        if (areaId.isNullOrBlank()) projs else projs.filter { it.areaId == areaId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tarefas: StateFlow<List<Tarefa>> = combine(todasTarefas, projetos, _areaFiltroId) { tasks, projsFiltrados, areaId ->
        if (areaId.isNullOrBlank()) tasks
        else {
            val idsPermitidos = projsFiltrados.map { it.id }.toSet()
            tasks.filter { it.projetoId != null && idsPermitidos.contains(it.projetoId) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val objetivos: StateFlow<List<Objetivo>> = combine(todosObjetivos, projetos, _areaFiltroId) { goals, projsFiltrados, areaId ->
        if (areaId.isNullOrBlank()) goals
        else {
            val idsPermitidos = projsFiltrados.map { it.id }.toSet()
            goals.filter { it.areaId == areaId || (it.projetoId != null && idsPermitidos.contains(it.projetoId)) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val marcos: StateFlow<List<Marco>> = combine(todosMarcos, projetos, _areaFiltroId) { ms, projsFiltrados, areaId ->
        if (areaId.isNullOrBlank()) ms
        else {
            val idsPermitidos = projsFiltrados.map { it.id }.toSet()
            ms.filter { it.projetoId != null && idsPermitidos.contains(it.projetoId) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.garantirDadosIniciais()
        }
    }

    fun emitirToast(msg: String) {
        _mensagemToast.value = msg
    }

    fun limparToast() {
        _mensagemToast.value = null
    }

    fun selecionarFiltroArea(areaId: String?) {
        _areaFiltroId.value = areaId
        val nomeArea = areas.value.find { it.id == areaId }?.nome
        emitirToast(if (nomeArea != null) "🎯 Foco ativado: $nomeArea" else "🌐 Visão Global (Todas as Áreas)")
    }

    fun hojeIso(): String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    // =========================================================================
    // MOTOR ANALÍTICO NÍVEL 2: BLOQUEIOS, GARGALOS E CAMINHO CRÍTICO
    // =========================================================================

    fun calcularMapaBloqueios(
        listaTarefas: List<Tarefa>,
        listaDeps: List<Dependencia>,
        listaProjetos: List<Projeto>
    ): Map<String, InfoBloqueioTarefa> {
        val idsCriticos = calcularTarefasNoCaminhoCritico(listaTarefas, listaDeps, listaProjetos)
        val mapa = mutableMapOf<String, InfoBloqueioTarefa>()

        listaTarefas.forEach { t ->
            val entradas = listaDeps.filter { it.sucessoraId == t.id }
            val saidas = listaDeps.filter { it.predecessoraId == t.id }

            val bloqueadoresAtivos = mutableListOf<String>()
            entradas.forEach { d ->
                val pred = listaTarefas.find { it.id == d.predecessoraId }
                if (pred != null && pred.estado != "Concluída") {
                    bloqueadoresAtivos.add(pred.titulo)
                }
            }

            var sucessorasPendentes = 0
            saidas.forEach { d ->
                val succ = listaTarefas.find { it.id == d.sucessoraId }
                if (succ != null && succ.estado != "Concluída") {
                    sucessorasPendentes++
                }
            }

            mapa[t.id] = InfoBloqueioTarefa(
                bloqueada = bloqueadoresAtivos.isNotEmpty(),
                titulosBloqueadores = bloqueadoresAtivos,
                temDependencias = entradas.isNotEmpty(),
                ehGargalo = sucessorasPendentes >= 2 && t.estado != "Concluída",
                qtdSucessorasTravadas = sucessorasPendentes,
                pertenceCaminhoCritico = idsCriticos.contains(t.id) && t.estado != "Concluída"
            )
        }
        return mapa
    }

    private fun calcularTarefasNoCaminhoCritico(
        listaTarefas: List<Tarefa>,
        listaDeps: List<Dependencia>,
        listaProjetos: List<Projeto>
    ): Set<String> {
        val criticos = mutableSetOf<String>()
        listaProjetos.forEach { p ->
            val res = calcularCaminhoCriticoProjeto(p.id, listaTarefas, listaDeps)
            if (res.second.size >= 2 && res.first > 0) {
                criticos.addAll(res.second)
            }
        }
        return criticos
    }

    fun calcularCaminhoCriticoProjeto(
        projetoId: String,
        listaTarefas: List<Tarefa>,
        listaDeps: List<Dependencia>
    ): Pair<Int, List<String>> {
        val pendentes = listaTarefas.filter { it.projetoId == projetoId && it.estado != "Concluída" }
        val pesos = pendentes.associate { it.id to max(0, it.estimativaMinutos) }
        val memo = mutableMapOf<String, Pair<Int, List<String>>>()

        fun dfs(atualId: String, visitados: Set<String>): Pair<Int, List<String>> {
            if (visitados.contains(atualId)) return 0 to emptyList()
            memo[atualId]?.let { return it }

            val pesoProprio = pesos[atualId] ?: 0
            val saidas = listaDeps.filter { it.predecessoraId == atualId && pesos.containsKey(it.sucessoraId) }

            var melhorSub: Pair<Int, List<String>> = 0 to emptyList()
            saidas.forEach { dep ->
                val sub = dfs(dep.sucessoraId, visitados + atualId)
                if (sub.first > melhorSub.first || (sub.first == melhorSub.first && sub.second.size > melhorSub.second.size)) {
                    melhorSub = sub
                }
            }

            val total = (pesoProprio + melhorSub.first) to (listOf(atualId) + melhorSub.second)
            memo[atualId] = total
            return total
        }

        var melhorProjeto: Pair<Int, List<String>> = 0 to emptyList()
        pendentes.forEach { t ->
            val cand = dfs(t.id, emptySet())
            if (cand.first > melhorProjeto.first) {
                melhorProjeto = cand
            }
        }
        return melhorProjeto
    }

    // =========================================================================
    // MOTOR PROBABILÍSTICO NÍVEL 2: K_CALIB, FAIXAS E CAPACIDADE 7 DIAS
    // =========================================================================

    fun calcularKCalibGlobal(listaTarefas: List<Tarefa>): Triple<Double, Int, Int> {
        val concluidasMedidas = listaTarefas.filter {
            it.estado == "Concluída" && it.estimativaMinutos > 0 && it.minutosReais > 0
        }
        val somaEst = concluidasMedidas.sumOf { it.estimativaMinutos }
        val somaReal = concluidasMedidas.sumOf { it.minutosReais }
        val k = if (somaEst > 0) (somaReal.toDouble() / somaEst.toDouble()).coerceIn(0.5, 3.0) else 1.0
        return Triple(k, somaEst, somaReal)
    }

    fun minutosDisponiveisNaData(dataIso: String, diaSemanaZeroASeis: Int, listaDisp: List<Disponibilidade>): Int {
        val excecoes = listaDisp.filter { d ->
            val temJanela = !d.validoDe.isNullOrBlank() || !d.validoAte.isNullOrBlank()
            if (!temJanela) false
            else {
                val deOk = d.validoDe.isNullOrBlank() || d.validoDe <= dataIso
                val ateOk = d.validoAte.isNullOrBlank() || d.validoAte >= dataIso
                deOk && ateOk && (d.diaSemana == diaSemanaZeroASeis || d.diaSemana == -1)
            }
        }

        val slotsAplicaveis = if (excecoes.isNotEmpty()) {
            excecoes
        } else {
            listaDisp.filter { it.validoDe.isNullOrBlank() && it.validoAte.isNullOrBlank() && it.diaSemana == diaSemanaZeroASeis }
        }

        return slotsAplicaveis.sumOf { slot -> duracaoSlotMinutos(slot) }
    }

    fun calcularResumoCapacidade7Dias(
        listaSessoes: List<Sessao>,
        listaDisp: List<Disponibilidade>
    ): Triple<Double, Double, Int> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cursor = Calendar.getInstance()
        var minDisponiveis7d = 0

        for (i in 0 until 7) {
            val dIso = sdf.format(cursor.time)
            val diaZeroASeis = cursor.get(Calendar.DAY_OF_WEEK) - 1
            minDisponiveis7d += minutosDisponiveisNaData(dIso, diaZeroASeis, listaDisp)
            cursor.add(Calendar.DAY_OF_YEAR, 1)
        }

        val agora = System.currentTimeMillis()
        val fim7Dias = agora + (7L * 24L * 3600L * 1000L)
        val minSessoes7d = listaSessoes
            .filter { it.inicioMs in agora..fim7Dias }
            .sumOf { it.duracaoMinutos }

        val hPlan = ((minSessoes7d / 60.0) * 10.0).roundToInt() / 10.0
        val hDisp = ((minDisponiveis7d / 60.0) * 10.0).roundToInt() / 10.0
        val pct = if (minDisponiveis7d > 0) ((minSessoes7d.toDouble() / minDisponiveis7d.toDouble()) * 100.0).roundToInt() else 0

        return Triple(hPlan, hDisp, pct)
    }

    private fun duracaoSlotMinutos(slot: Disponibilidade): Int {
        if (!slot.ativo) return 0
        if (slot.horaInicio == "00:00" && slot.horaFim == "00:00") return 0
        return try {
            val pIni = slot.horaInicio.split(":").map { it.toInt() }
            val pFim = slot.horaFim.split(":").map { it.toInt() }
            val minIni = pIni[0] * 60 + pIni[1]
            val minFim = if (pFim[0] == 0 && pFim[1] == 0) 24 * 60 else pFim[0] * 60 + pFim[1]
            max(0, minFim - minIni)
        } catch (e: Exception) {
            0
        }
    }

    fun calcularProjecaoProjeto(
        projeto: Projeto,
        listaTarefas: List<Tarefa>,
        listaDeps: List<Dependencia>,
        listaDisp: List<Disponibilidade>
    ): ProjecaoProjetoNivel2 {
        val tarefasProj = listaTarefas.filter { it.projetoId == projeto.id && !it.arquivada }
        if (tarefasProj.isEmpty()) {
            return ProjecaoProjetoNivel2(projeto, "nivel0", motivoNivel0 = "Nenhuma tarefa vinculada ao projeto.")
        }

        val pendentes = tarefasProj.filter { it.estado != "Concluída" }
        if (pendentes.isEmpty()) {
            return ProjecaoProjetoNivel2(projeto, "concluido")
        }

        val semEstimativa = pendentes.count { it.estimativaMinutos <= 0 }
        val taxaIncompletude = ((semEstimativa.toDouble() / pendentes.size.toDouble()) * 100.0).roundToInt()
        val somaMinutosRestantes = pendentes.sumOf { max(0, it.estimativaMinutos) }

        val cp = calcularCaminhoCriticoProjeto(projeto.id, listaTarefas, listaDeps)
        val minutosBase = max(somaMinutosRestantes, cp.first)

        if (minutosBase <= 0) {
            return ProjecaoProjetoNivel2(
                projeto = projeto,
                statusProjecao = "nivel0",
                motivoNivel0 = "Existem ${pendentes.size} tarefas pendentes, mas nenhuma possui estimativa (Incerteza: 100%).",
                tarefasSemEstimativa = semEstimativa,
                totalPendentes = pendentes.size,
                taxaIncompletudePct = 100
            )
        }

        val concluidasProj = tarefasProj.filter { it.estado == "Concluída" && it.estimativaMinutos > 0 && it.minutosReais > 0 }
        val kCalib = if (concluidasProj.isNotEmpty()) {
            val estP = concluidasProj.sumOf { it.estimativaMinutos }.toDouble()
            val actP = concluidasProj.sumOf { it.minutosReais }.toDouble()
            (actP / estP).coerceIn(0.5, 3.0)
        } else {
            calcularKCalibGlobal(listaTarefas).first
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calTeste = Calendar.getInstance()
        var cap14Dias = 0
        for (i in 0 until 14) {
            val dIso = sdf.format(calTeste.time)
            val diaZeroASeis = calTeste.get(Calendar.DAY_OF_WEEK) - 1
            cap14Dias += minutosDisponiveisNaData(dIso, diaZeroASeis, listaDisp)
            calTeste.add(Calendar.DAY_OF_YEAR, 1)
        }

        if (cap14Dias <= 0) {
            return ProjecaoProjetoNivel2(
                projeto = projeto,
                statusProjecao = "nivel0",
                motivoNivel0 = "Nenhuma janela de disponibilidade ativa configurada na agenda.",
                tarefasSemEstimativa = semEstimativa,
                totalPendentes = pendentes.size,
                taxaIncompletudePct = taxaIncompletude
            )
        }

        fun projetarData(minutosAlvo: Int, buffer: Double, fator: Double): String {
            var queima = minutosAlvo * fator
            val cursor = Calendar.getInstance()
            var seguranca = 365
            while (queima > 0 && seguranca > 0) {
                val dIso = sdf.format(cursor.time)
                val diaZeroASeis = cursor.get(Calendar.DAY_OF_WEEK) - 1
                val capLiquida = minutosDisponiveisNaData(dIso, diaZeroASeis, listaDisp) * (1.0 - buffer)
                if (capLiquida > 0) queima -= capLiquida
                if (queima > 0) {
                    cursor.add(Calendar.DAY_OF_YEAR, 1)
                    seguranca--
                }
            }
            return sdf.format(cursor.time)
        }

        val melhorCaso = projetarData(minutosBase, 0.0, 1.0)
        val esperado = projetarData(minutosBase, 0.20, 1.0)
        val historico = projetarData(minutosBase, 0.20, kCalib)
        val risco = !projeto.dataAlvo.isNullOrBlank() && esperado > projeto.dataAlvo

        return ProjecaoProjetoNivel2(
            projeto = projeto,
            statusProjecao = "projetado",
            dataMelhorCaso = melhorCaso,
            dataEsperada = esperado,
            dataHistorica = historico,
            emRiscoPrazo = risco,
            horasRestantes = ((somaMinutosRestantes / 60.0) * 10.0).roundToInt() / 10.0,
            horasCaminhoCritico = ((cp.first / 60.0) * 10.0).roundToInt() / 10.0,
            nosCaminhoCritico = cp.second.size,
            tarefasSemEstimativa = semEstimativa,
            totalPendentes = pendentes.size,
            taxaIncompletudePct = taxaIncompletude,
            fatorCalibracao = (kCalib * 100.0).roundToInt() / 100.0
        )
    }

    // =========================================================================
    // CRONÓMETRO DE FOCO INTEGRADO
    // =========================================================================

    fun iniciarFoco(tarefa: Tarefa) {
        timerJob?.cancel()
        inicioFocoMs = System.currentTimeMillis()
        _tarefaEmFoco.value = tarefa
        _segundosFoco.value = 0L

        if (tarefa.estado == "Não iniciada") {
            atualizarEstadoTarefa(tarefa, "Em andamento")
        }

        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _segundosFoco.value = (System.currentTimeMillis() - inicioFocoMs) / 1000L
            }
        }
        emitirToast("▶ Foco iniciado: ${tarefa.titulo}")
    }

    fun pararFoco(salvarTempo: Boolean, concluirTarefa: Boolean = false) {
        timerJob?.cancel()
        timerJob = null
        val tarefa = _tarefaEmFoco.value
        val segs = _segundosFoco.value
        _tarefaEmFoco.value = null
        _segundosFoco.value = 0L

        if (!salvarTempo || tarefa == null) {
            emitirToast("Cronómetro cancelado.")
            return
        }

        val minutosGastos = max(1, (segs / 60.0).roundToInt())
        val novoTotalReal = tarefa.minutosReais + minutosGastos
        val novoEstado = if (concluirTarefa) "Concluída" else "Em andamento"
        val agora = System.currentTimeMillis()

        viewModelScope.launch {
            repository.atualizarTarefa(
                tarefa.copy(
                    estado = novoEstado,
                    minutosReais = novoTotalReal,
                    alteradoEm = agora,
                    concluidaEm = if (concluirTarefa) agora else tarefa.concluidaEm
                )
            )
            repository.inserirSessao(
                Sessao(
                    tarefaId = tarefa.id,
                    projetoId = tarefa.projetoId,
                    titulo = "Foco: ${tarefa.titulo}",
                    inicioMs = inicioFocoMs,
                    fimMs = agora,
                    duracaoMinutos = minutosGastos,
                    estado = "Concluída"
                )
            )
            emitirToast("✓ +${minutosGastos}m gravados em '${tarefa.titulo}' (K_calib atualizado)")
        }
    }

    // =========================================================================
    // CRUD COMPLETO COM VALIDAÇÕES DE NÍVEL 2
    // =========================================================================

    fun criarTarefaCompleta(
        titulo: String,
        projetoId: String? = null,
        prioridade: String = "Média",
        estimativaMinutos: Int = 0,
        prazoData: String? = null
    ) {
        if (titulo.isBlank()) {
            emitirToast("Informe o título da tarefa.")
            return
        }
        viewModelScope.launch {
            repository.inserirTarefa(
                Tarefa(
                    titulo = titulo.trim(),
                    projetoId = projetoId?.takeIf { it.isNotBlank() },
                    prioridade = prioridade,
                    estimativaMinutos = max(0, estimativaMinutos),
                    prazoData = prazoData?.takeIf { it.isNotBlank() }
                )
            )
            emitirToast("✓ Tarefa adicionada à fila!")
        }
    }

    fun editarTarefa(
        tarefa: Tarefa,
        novoTitulo: String,
        novoProjetoId: String?,
        novaPrioridade: String,
        novaEstimativa: Int,
        novoTempoReal: Int,
        novoPrazo: String?,
        novoEstado: String
    ) {
        if (novoTitulo.isBlank()) return
        val agora = System.currentTimeMillis()
        viewModelScope.launch {
            repository.atualizarTarefa(
                tarefa.copy(
                    titulo = novoTitulo.trim(),
                    projetoId = novoProjetoId?.takeIf { it.isNotBlank() },
                    prioridade = novaPrioridade,
                    estimativaMinutos = max(0, novaEstimativa),
                    minutosReais = max(0, novoTempoReal),
                    prazoData = novoPrazo?.takeIf { it.isNotBlank() },
                    estado = novoEstado,
                    alteradoEm = agora,
                    concluidaEm = if (novoEstado == "Concluída") agora else null
                )
            )
            emitirToast("✓ Tarefa atualizada!")
        }
    }

    fun adiarTarefaSeteDias(tarefa: Tarefa) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        if (!tarefa.prazoData.isNullOrBlank()) {
            runCatching { sdf.parse(tarefa.prazoData)?.let { cal.time = it } }
        }
        cal.add(Calendar.DAY_OF_YEAR, 7)
        val novoPrazo = sdf.format(cal.time)

        viewModelScope.launch {
            repository.atualizarTarefa(
                tarefa.copy(prazoData = novoPrazo, alteradoEm = System.currentTimeMillis())
            )
            emitirToast("✓ Prazo adiado para $novoPrazo (+7 dias)")
        }
    }

    fun concluirTarefaComCalibracao(tarefa: Tarefa, minutosReaisExecutados: Int) {
        val agora = System.currentTimeMillis()
        viewModelScope.launch {
            repository.atualizarTarefa(
                tarefa.copy(
                    estado = "Concluída",
                    minutosReais = max(0, minutosReaisExecutados),
                    alteradoEm = agora,
                    concluidaEm = agora
                )
            )
            emitirToast("✓ Tarefa concluída e K_calib atualizado!")
        }
    }

    fun atualizarEstadoTarefa(tarefa: Tarefa, novoEstado: String) {
        val agora = System.currentTimeMillis()
        viewModelScope.launch {
            val concluidaEm = if (novoEstado == "Concluída") agora else null
            repository.atualizarTarefa(
                tarefa.copy(estado = novoEstado, alteradoEm = agora, concluidaEm = concluidaEm)
            )
            emitirToast("Estado alterado para '$novoEstado'")
        }
    }

    fun manterTarefaFantasma(tarefa: Tarefa) {
        viewModelScope.launch {
            repository.atualizarTarefa(tarefa.copy(alteradoEm = System.currentTimeMillis()))
            emitirToast("✓ Tarefa renovada no backlog por mais 30 dias.")
        }
    }

    fun arquivarTarefa(tarefa: Tarefa) {
        viewModelScope.launch {
            repository.atualizarTarefa(tarefa.copy(arquivada = true, alteradoEm = System.currentTimeMillis()))
            emitirToast("Tarefa arquivada.")
        }
    }

    fun criarArea(nome: String, descricao: String, cor: String) {
        if (nome.isBlank()) return
        viewModelScope.launch {
            repository.inserirArea(Area(nome = nome.trim(), descricao = descricao.trim(), cor = cor.ifBlank { "#00F0FF" }))
            emitirToast("✓ Área '$nome' criada!")
        }
    }

    fun arquivarArea(area: Area) {
        viewModelScope.launch {
            repository.atualizarArea(area.copy(status = "Arquivada"))
            if (_areaFiltroId.value == area.id) _areaFiltroId.value = null
            emitirToast("Área arquivada.")
        }
    }

    fun criarProjeto(nome: String, areaId: String?, objetivo: String, dataAlvo: String?) {
        if (nome.isBlank()) return
        viewModelScope.launch {
            repository.inserirProjeto(
                Projeto(
                    nome = nome.trim(),
                    areaId = areaId?.takeIf { it.isNotBlank() },
                    objetivo = objetivo.trim(),
                    dataAlvo = dataAlvo?.takeIf { it.isNotBlank() }
                )
            )
            emitirToast("✓ Projeto '$nome' criado!")
        }
    }

    fun editarProjeto(
        projeto: Projeto,
        novoNome: String,
        novaAreaId: String?,
        novoObjetivo: String,
        novaDataAlvo: String?,
        novoEstado: String
    ) {
        if (novoNome.isBlank()) return
        viewModelScope.launch {
            repository.atualizarProjeto(
                projeto.copy(
                    nome = novoNome.trim(),
                    areaId = novaAreaId?.takeIf { it.isNotBlank() },
                    objetivo = novoObjetivo.trim(),
                    dataAlvo = novaDataAlvo?.takeIf { it.isNotBlank() },
                    estado = novoEstado,
                    alteradoEm = System.currentTimeMillis()
                )
            )
            emitirToast("✓ Projeto atualizado!")
        }
    }

    fun arquivarProjeto(projeto: Projeto) {
        viewModelScope.launch {
            repository.atualizarProjeto(projeto.copy(arquivado = true))
            emitirToast("Projeto arquivado.")
        }
    }

    fun criarObjetivo(titulo: String, horizonte: String, projetoId: String?, dataAlvo: String?) {
        if (titulo.isBlank()) return
        viewModelScope.launch {
            repository.inserirObjetivo(
                Objetivo(
                    titulo = titulo.trim(),
                    horizonte = horizonte,
                    areaId = _areaFiltroId.value,
                    projetoId = projetoId?.takeIf { it.isNotBlank() },
                    dataAlvo = dataAlvo?.takeIf { it.isNotBlank() }
                )
            )
            emitirToast("✓ Objetivo registado!")
        }
    }

    fun alternarEstadoObjetivo(obj: Objetivo) {
        val novo = if (obj.estado == "Concluído") "Ativo" else "Concluído"
        viewModelScope.launch { repository.atualizarObjetivo(obj.copy(estado = novo)) }
    }

    fun criarMarco(titulo: String, projetoId: String?, dataAlvo: String?) {
        if (titulo.isBlank()) return
        viewModelScope.launch {
            repository.inserirMarco(
                Marco(
                    titulo = titulo.trim(),
                    projetoId = projetoId?.takeIf { it.isNotBlank() },
                    dataAlvo = dataAlvo?.takeIf { it.isNotBlank() }
                )
            )
            emitirToast("✓ Marco criado!")
        }
    }

    fun alternarEstadoMarco(marco: Marco) {
        val novo = if (marco.estado == "Concluído") "Pendente" else "Concluído"
        viewModelScope.launch { repository.atualizarMarco(marco.copy(estado = novo)) }
    }

    fun criarDependencia(predecessoraId: String, sucessoraId: String, notas: String) {
        if (predecessoraId.isBlank() || sucessoraId.isBlank()) {
            emitirToast("Selecione ambas as tarefas.")
            return
        }
        if (predecessoraId == sucessoraId) {
            emitirToast("⚠ Uma tarefa não pode depender de si mesma.")
            return
        }

        val atuais = dependencias.value
        if (atuais.any { it.predecessoraId == predecessoraId && it.sucessoraId == sucessoraId }) {
            emitirToast("⚠ Este vínculo já existe.")
            return
        }

        val grafo = mutableMapOf<String, MutableList<String>>()
        atuais.forEach { d -> grafo.getOrPut(d.predecessoraId) { mutableListOf() }.add(d.sucessoraId) }
        grafo.getOrPut(predecessoraId) { mutableListOf() }.add(sucessoraId)

        fun temCaminhoDfs(atual: String, alvo: String, visitados: MutableSet<String>): Boolean {
            if (atual == alvo) return true
            visitados.add(atual)
            for (vizinho in grafo[atual] ?: emptyList()) {
                if (!visitados.contains(vizinho) && temCaminhoDfs(vizinho, alvo, visitados)) return true
            }
            return false
        }

        if (temCaminhoDfs(sucessoraId, predecessoraId, mutableSetOf())) {
            emitirToast("🚫 Ciclo inválido: dependência circular detetada!")
            return
        }

        viewModelScope.launch {
            repository.inserirDependencia(Dependencia(predecessoraId = predecessoraId, sucessoraId = sucessoraId, notas = notas.trim()))
            emitirToast("✓ Dependência vinculada sem ciclos!")
        }
    }

    fun removerDependencia(depId: String) {
        viewModelScope.launch {
            repository.removerDependencia(depId)
            emitirToast("Vínculo removido.")
        }
    }

    fun alternarSlotDisponibilidade(disp: Disponibilidade) {
        viewModelScope.launch {
            repository.atualizarDisponibilidade(disp.copy(ativo = !disp.ativo))
        }
    }

    fun criarExcecaoDisponibilidade(diaSemana: Int, validoDe: String, validoAte: String, horaInicio: String, horaFim: String) {
        if (validoDe.isBlank() || validoAte.isBlank()) {
            emitirToast("Informe as datas de início e fim da exceção.")
            return
        }
        viewModelScope.launch {
            repository.inserirDisponibilidade(
                Disponibilidade(
                    diaSemana = diaSemana,
                    horaInicio = horaInicio.ifBlank { "00:00" },
                    horaFim = horaFim.ifBlank { "00:00" },
                    ativo = true,
                    validoDe = validoDe.trim(),
                    validoAte = validoAte.trim()
                )
            )
            emitirToast("✓ Exceção temporal adicionada!")
        }
    }

    fun removerDisponibilidade(dispId: String) {
        viewModelScope.launch {
            repository.removerDisponibilidade(dispId)
            emitirToast("Exceção removida.")
        }
    }

    fun criarSessao(titulo: String, duracaoMinutos: Int = 60, tarefaId: String? = null) {
        if (titulo.isBlank()) {
            emitirToast("Informe o título da sessão.")
            return
        }
        val agora = System.currentTimeMillis()
        val fim = agora + (max(15, duracaoMinutos) * 60_000L)

        // Verifica colisão de horário com sessões planejadas ativas
        val conflito = sessoes.value.find { s ->
            s.estado != "Concluída" && agora < s.fimMs && fim > s.inicioMs
        }

        val projId = tarefas.value.find { it.id == tarefaId }?.projetoId
        viewModelScope.launch {
            repository.inserirSessao(
                Sessao(
                    tarefaId = tarefaId,
                    projetoId = projId,
                    titulo = titulo.trim(),
                    inicioMs = agora,
                    fimMs = fim,
                    duracaoMinutos = max(15, duracaoMinutos)
                )
            )
            if (conflito != null) {
                emitirToast("⚠ Sessão criada (sobrepõe '${conflito.titulo}')")
            } else {
                emitirToast("✓ Sessão agendada sem conflitos!")
            }
        }
    }

    fun alternarEstadoSessao(sessao: Sessao) {
        val novo = if (sessao.estado == "Concluída") "Planejada" else "Concluída"
        viewModelScope.launch {
            repository.atualizarSessao(sessao.copy(estado = novo))
            emitirToast("Sessão marcada como $novo")
        }
    }

    fun criarRevisao(titulo: String, regraIntervalo: String = "D+7", observacoes: String = "") {
        if (titulo.isBlank()) return
        val diasOffset = when (regraIntervalo) {
            "D+1" -> 1
            "D+3" -> 3
            "D+15" -> 15
            else -> 7
        }
        val dataAlvoMs = System.currentTimeMillis() + (diasOffset * 86_400_000L)
        viewModelScope.launch {
            repository.inserirRevisao(
                Revisao(
                    titulo = titulo.trim(),
                    regraIntervalo = regraIntervalo,
                    observacoes = observacoes.trim(),
                    dataAgendada = dataAlvoMs
                )
            )
            emitirToast("✓ Revisão agendada para $regraIntervalo!")
        }
    }

    /**
     * Conclui a revisão e propaga automaticamente o próximo ciclo de repetição espaçada:
     * D+1 -> D+3 -> D+7 -> D+15
     */
    fun concluirRevisao(revisao: Revisao) {
        val concluindo = revisao.estado != "Concluída"
        val novoEstado = if (concluindo) "Concluída" else "Pendente"

        viewModelScope.launch {
            repository.atualizarRevisao(revisao.copy(estado = novoEstado))

            if (concluindo) {
                val proximoCiclo = when (revisao.regraIntervalo) {
                    "D+1" -> "D+3" to 3
                    "D+3" -> "D+7" to 7
                    "D+7" -> "D+15" to 15
                    else -> null
                }
                if (proximoCiclo != null) {
                    val (novaRegra, dias) = proximoCiclo
                    val tituloLimpo = revisao.titulo.replace(Regex("^\\[D\\+\\d+\\]\\s*"), "")
                    repository.inserirRevisao(
                        Revisao(
                            titulo = "[$novaRegra] $tituloLimpo",
                            tipoOrigem = revisao.tipoOrigem,
                            origemId = revisao.origemId,
                            regraIntervalo = novaRegra,
                            observacoes = revisao.observacoes,
                            dataAgendada = System.currentTimeMillis() + (dias * 86_400_000L),
                            estado = "Pendente"
                        )
                    )
                    emitirToast("✓ Revisão concluída! Próximo ciclo ($novaRegra) agendado automaticamente.")
                } else {
                    emitirToast("✓ Ciclo completo de revisões concluído!")
                }
            } else {
                emitirToast("Revisão reaberta.")
            }
        }
    }

    fun criarRetrospectiva(avancou: String, bloqueou: String, mudar: String) {
        if (avancou.isBlank() && bloqueou.isBlank() && mudar.isBlank()) {
            emitirToast("Preencha ao menos um campo da reflexão.")
            return
        }
        viewModelScope.launch {
            repository.inserirRetrospectiva(
                Retrospectiva(oQueAvancou = avancou.trim(), oQueBloqueou = bloqueou.trim(), oQueMudar = mudar.trim())
            )
            emitirToast("✓ Reflexão semanal registada no histórico!")
        }
    }

    fun sincronizarComCalendario(context: Context, sessao: Sessao) {
        try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, "[Se Eduque] ${sessao.titulo}")
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, sessao.inicioMs)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, sessao.fimMs)
                putExtra(CalendarContract.Events.DESCRIPTION, "Sessão de Foco gerada pelo Se Eduque Nível 2")
            }
            context.startActivity(intent)
            viewModelScope.launch { repository.atualizarSessao(sessao.copy(sincronizadoCalendar = true)) }
        } catch (e: Exception) {
            emitirToast("Não foi possível abrir a aplicação de Calendário.")
        }
    }

    fun realizarBackupLocal(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val json = repository.getDadosBackup()
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    OutputStreamWriter(stream).use { writer -> writer.write(json) }
                }
                emitirToast("✓ Backup JSON exportado com sucesso!")
            } catch (e: Exception) {
                emitirToast("Erro ao exportar backup.")
            }
        }
    }

    fun restaurarBackupLocal(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val jsonStr = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (!jsonStr.isNullOrBlank()) {
                    val qtd = repository.restaurarDadosBackup(jsonStr)
                    emitirToast("✓ Backup restaurado ($qtd registos importados)!")
                }
            } catch (e: Exception) {
                emitirToast("Erro ao ler ficheiro JSON de backup.")
            }
        }
    }

    fun injetarMassaDeTeste() {
        viewModelScope.launch {
            repository.gerarCenarioTesteNivel2()
            emitirToast("🧪 Cenário de Teste Nível 2 gerado! Verifique Kanban e Previsões.")
        }
    }

    fun limparMassaDeTeste() {
        viewModelScope.launch {
            repository.limparCenarioTesteNivel2()
            emitirToast("🧹 Massa de teste removida com sucesso!")
        }
    }
}