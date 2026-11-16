package com.nexus.seeduque.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.seeduque.database.*
import com.nexus.seeduque.ui.theme.CyberCores
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.roundToInt

// =============================================================================
// UTILITÁRIOS NATIVOS DE DATA E HORA (ANDROID DATEPICKER & TIMEPICKER)
// =============================================================================

fun abrirSeletorDataIso(
    context: Context,
    dataAtualIso: String?,
    onDataSelecionada: (String) -> Unit
) {
    val cal = Calendar.getInstance()
    if (!dataAtualIso.isNullOrBlank()) {
        runCatching {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dataAtualIso)?.let { cal.time = it }
        }
    }
    DatePickerDialog(
        context,
        { _, ano, mes, dia ->
            val formatada = String.format(Locale.getDefault(), "%04d-%02d-%02d", ano, mes + 1, dia)
            onDataSelecionada(formatada)
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    ).show()
}

fun abrirSeletorHora(
    context: Context,
    horaAtual: String,
    onHoraSelecionada: (String) -> Unit
) {
    val partes = horaAtual.split(":")
    val hIni = partes.getOrNull(0)?.toIntOrNull() ?: 21
    val mIni = partes.getOrNull(1)?.toIntOrNull() ?: 0
    TimePickerDialog(
        context,
        { _, h, m ->
            onHoraSelecionada(String.format(Locale.getDefault(), "%02d:%02d", h, m))
        },
        hIni,
        mIni,
        true
    ).show()
}

fun combinarDataHoraMs(dataIso: String, horaStr: String): Long {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        sdf.parse("$dataIso $horaStr")?.time ?: System.currentTimeMillis()
    } catch (e: Exception) {
        System.currentTimeMillis()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: MainViewModel) {
    var destinoPrincipal by remember { mutableIntStateOf(0) } // 0: Hoje, 1: Tarefas, 2: Projetos, 3: Agenda, 4: Mais
    var abaAtual by remember { mutableStateOf("hoje") }
    var mostrarCapturaRapida by remember { mutableStateOf(false) }

    val areas by viewModel.areas.collectAsState()
    val areaFiltroId by viewModel.areaFiltroId.collectAsState()
    val qtdSemProjeto by viewModel.qtdTarefasSemProjeto.collectAsState()
    val projetos by viewModel.projetos.collectAsState()
    val tarefas by viewModel.tarefas.collectAsState()
    val objetivos by viewModel.objetivos.collectAsState()
    val marcos by viewModel.marcos.collectAsState()
    val dependencias by viewModel.dependencias.collectAsState()
    val disponibilidade by viewModel.disponibilidade.collectAsState()
    val sessoes by viewModel.sessoes.collectAsState()
    val revisoes by viewModel.revisoes.collectAsState()
    val retrospectivas by viewModel.retrospectivas.collectAsState()

    val tarefaEmFoco by viewModel.tarefaEmFoco.collectAsState()
    val segundosFoco by viewModel.segundosFoco.collectAsState()
    val toastMsg by viewModel.mensagemToast.collectAsState()

    // Estados de Modais HUD preservados
    var tarefaParaConcluir by remember { mutableStateOf<Tarefa?>(null) }
    var tarefaParaEditar by remember { mutableStateOf<Tarefa?>(null) }
    var projetoParaEditar by remember { mutableStateOf<Projeto?>(null) }

    val mapaBloqueios = remember(tarefas, dependencias, projetos) {
        viewModel.calcularMapaBloqueios(tarefas, dependencias, projetos)
    }

    fun navegarPara(grupoOuDestino: Int, aba: String) {
        abaAtual = aba
        destinoPrincipal = when (aba) {
            "hoje" -> 0
            "tarefas", "kanban", "prontas", "dependencias" -> 1
            "projetos", "areas", "objetivos", "marcos", "previsoes" -> 2
            "agenda", "revisoes" -> 3
            else -> 4
        }
    }

    LaunchedEffect(toastMsg) {
        if (toastMsg != null) {
            delay(3200)
            viewModel.limparToast()
        }
    }

    val dataHojeLegivel = remember {
        SimpleDateFormat("EEE., d 'de' MMM.", Locale("pt", "BR")).format(Date())
    }

    Scaffold(
        containerColor = CyberCores.BgBase,
        bottomBar = {
            NavigationBar(
                containerColor = CyberCores.BgSurface,
                tonalElevation = 0.dp
            ) {
                val itensNav = listOf(
                    Triple(0, "Hoje", "hoje"),
                    Triple(1, "Tarefas", "tarefas"),
                    Triple(2, "Projetos", "projetos"),
                    Triple(3, "Agenda", "agenda"),
                    Triple(4, "Mais", "semanal")
                )
                itensNav.forEach { (idx, rotulo, abaPadrao) ->
                    val selecionado = destinoPrincipal == idx
                    NavigationBarItem(
                        selected = selecionado,
                        onClick = {
                            destinoPrincipal = idx
                            abaAtual = abaPadrao
                        },
                        icon = {
                            val iconeTxt = when (idx) {
                                0 -> "✦"
                                1 -> "☰"
                                2 -> "❐"
                                3 -> "📅"
                                else -> "•••"
                            }
                            Text(
                                text = iconeTxt,
                                color = if (selecionado) CyberCores.NeonCyan else CyberCores.TextDim,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        label = {
                            Text(
                                text = rotulo,
                                color = if (selecionado) CyberCores.NeonCyan else CyberCores.TextDim,
                                fontSize = 11.sp,
                                fontWeight = if (selecionado) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = CyberCores.NeonCyan.copy(alpha = 0.14f)
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { mostrarCapturaRapida = true },
                containerColor = CyberCores.NeonCyan,
                contentColor = Color.Black,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        },
        snackbarHost = {
            if (toastMsg != null) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.BottomCenter) {
                    Surface(
                        color = CyberCores.BgSurface,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, CyberCores.NeonCyan)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(toastMsg ?: "", color = CyberCores.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. CABEÇALHO LIMPO E HONESTO
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Se Eduque", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Column(horizontalAlignment = Alignment.End) {
                    Text(dataHojeLegivel, color = CyberCores.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text("Salvo neste aparelho", color = CyberCores.TextDim, fontSize = 10.sp)
                }
            }

            // 2. FAIXA DESLIZÁVEL DE ÁREAS (COM AVISO TRANSPARENTE DE TAREFAS SEM PROJETO)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        val ativoGlobal = areaFiltroId == null
                        FilterChip(
                            selected = ativoGlobal,
                            onClick = { viewModel.selecionarFiltroArea(null) },
                            label = { Text("Todas", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberCores.NeonCyan.copy(alpha = 0.2f),
                                selectedLabelColor = CyberCores.NeonCyan
                            )
                        )
                    }
                    items(areas) { area ->
                        val corArea = CyberCores.parseHex(area.cor)
                        val selecionado = areaFiltroId == area.id
                        FilterChip(
                            selected = selecionado,
                            onClick = { viewModel.selecionarFiltroArea(if (selecionado) null else area.id) },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(modifier = Modifier.size(8.dp).background(corArea, CircleShape))
                                    Text(area.nome, fontSize = 12.sp)
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = corArea.copy(alpha = 0.22f),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                if (areaFiltroId != null && qtdSemProjeto > 0) {
                    Text(
                        text = "ℹ $qtdSemProjeto tarefa(s) sem projeto estão visíveis em 'Todas'.",
                        color = CyberCores.TextDim,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            // 3. BARRA DE CRONÔMETRO DE FOCO ATIVO
            if (tarefaEmFoco != null) {
                val mins = String.format("%02d", segundosFoco / 60)
                val segs = String.format("%02d", segundosFoco % 60)
                Surface(
                    color = CyberCores.NeonGreen.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, CyberCores.NeonGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Em foco agora", color = CyberCores.NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(tarefaEmFoco?.titulo ?: "", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("$mins:$segs", color = CyberCores.NeonGreen, fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.pararFoco(salvarTempo = true, concluirTarefa = false) },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCores.NeonGreen),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) { Text("Gravar", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        Spacer(modifier = Modifier.width(4.dp))
                        OutlinedButton(
                            onClick = { viewModel.pararFoco(salvarTempo = true, concluirTarefa = true) },
                            border = BorderStroke(1.dp, CyberCores.NeonCyan),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) { Text("Concluir", color = CyberCores.NeonCyan, fontSize = 11.sp) }
                        Spacer(modifier = Modifier.width(4.dp))
                        TextButton(
                            onClick = { viewModel.pararFoco(salvarTempo = false) },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) { Text("✕", color = CyberCores.NeonMagenta, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }

            // 4. SELETOR DE SUB-VISTAS PARA ACESSAR TODOS OS RECURSOS EXISTENTES (QUANDO FORA DE "HOJE")
            if (destinoPrincipal != 0) {
                NavegacaoSubVistasDestino(
                    destinoPrincipal = destinoPrincipal,
                    abaAtual = abaAtual,
                    onSelectAba = { abaAtual = it }
                )
            }

            // 5. CONTEÚDO DA TELA ATIVA
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (abaAtual) {
                    "hoje" -> EcranHojeDashboard(
                        tarefas = tarefas,
                        projetos = projetos,
                        revisoes = revisoes,
                        mapaBloqueios = mapaBloqueios,
                        viewModel = viewModel,
                        onNavigate = { g, a -> navegarPara(g, a) },
                        onOpenConcluir = { tarefaParaConcluir = it },
                        onOpenEditar = { tarefaParaEditar = it }
                    )
                    "prontas" -> EcranProntasParaAgir(
                        tarefas = tarefas,
                        projetos = projetos,
                        mapaBloqueios = mapaBloqueios,
                        viewModel = viewModel,
                        onNavigate = { g, a -> navegarPara(g, a) },
                        onOpenConcluir = { tarefaParaConcluir = it },
                        onOpenEditar = { tarefaParaEditar = it }
                    )
                    "tarefas" -> EcranTarefasLista(
                        tarefas = tarefas,
                        projetos = projetos,
                        mapaBloqueios = mapaBloqueios,
                        viewModel = viewModel,
                        onOpenConcluir = { tarefaParaConcluir = it },
                        onOpenEditar = { tarefaParaEditar = it }
                    )
                    "kanban" -> EcranQuadroKanban(
                        tarefas = tarefas,
                        mapaBloqueios = mapaBloqueios,
                        viewModel = viewModel,
                        onOpenConcluir = { tarefaParaConcluir = it },
                        onOpenEditar = { tarefaParaEditar = it }
                    )
                    "areas" -> EcranAreas(areas = areas, viewModel = viewModel)
                    "projetos" -> EcranProjetos(
                        projetos = projetos,
                        areas = areas,
                        tarefas = tarefas,
                        dependencias = dependencias,
                        disponibilidade = disponibilidade,
                        viewModel = viewModel,
                        onOpenEditarProjeto = { projetoParaEditar = it }
                    )
                    "objetivos" -> EcranObjetivos(objetivos = objetivos, projetos = projetos, tarefas = tarefas, viewModel = viewModel)
                    "marcos" -> EcranMarcos(marcos = marcos, projetos = projetos, viewModel = viewModel)
                    "agenda" -> EcranPlanejamentoAgenda(sessoes = sessoes, disponibilidade = disponibilidade, tarefas = tarefas, viewModel = viewModel)
                    "dependencias" -> EcranDependencias(dependencias = dependencias, tarefas = tarefas, viewModel = viewModel)
                    "revisoes" -> EcranRevisoesEstudo(revisoes = revisoes, viewModel = viewModel)
                    "previsoes" -> EcranPrevisoesNivel2(
                        projetos = projetos,
                        tarefas = tarefas,
                        dependencias = dependencias,
                        disponibilidade = disponibilidade,
                        viewModel = viewModel,
                        onNavigate = { g, a -> navegarPara(g, a) }
                    )
                    "semanal" -> EcranRevisaoSemanalBackup(
                        tarefas = tarefas,
                        retrospectivas = retrospectivas,
                        mapaBloqueios = mapaBloqueios,
                        viewModel = viewModel,
                        onOpenEditar = { tarefaParaEditar = it },
                        onOpenConcluir = { tarefaParaConcluir = it }
                    )
                }
            }
        }

        // MODAL DE CAPTURA RÁPIDA HONESTA (NÃO INVENTA 30 MIN NEM PRAZO HOJE)
        if (mostrarCapturaRapida) {
            ModalCapturaRapidaHonesta(
                projetos = projetos,
                hojeIso = viewModel.hojeIso(),
                onDismiss = { mostrarCapturaRapida = false },
                onSalvarTarefa = { titulo, projId, prio, estMin, prazo ->
                    viewModel.criarTarefaCompleta(
                        titulo = titulo,
                        projetoId = projId,
                        prioridade = prio,
                        estimativaMinutos = estMin,
                        prazoData = prazo
                    )
                    mostrarCapturaRapida = false
                },
                onIrParaAgenda = {
                    mostrarCapturaRapida = false
                    navegarPara(3, "agenda")
                },
                onIrParaRevisoes = {
                    mostrarCapturaRapida = false
                    navegarPara(3, "revisoes")
                }
            )
        }

        // MODAL HUD DE CONCLUSÃO E CALIBRAÇÃO (K_CALIB)
        if (tarefaParaConcluir != null) {
            ModalConcluirCalibrar(
                tarefa = tarefaParaConcluir!!,
                onDismiss = { tarefaParaConcluir = null },
                onConfirm = { minReais ->
                    viewModel.concluirTarefaComCalibracao(tarefaParaConcluir!!, minReais)
                    tarefaParaConcluir = null
                }
            )
        }

        // MODAL HUD DE EDIÇÃO RÁPIDA DE TAREFA
        if (tarefaParaEditar != null) {
            ModalEditarTarefa(
                tarefa = tarefaParaEditar!!,
                projetos = projetos,
                onDismiss = { tarefaParaEditar = null },
                onConfirm = { titulo, projId, prio, est, real, prazo, estado ->
                    viewModel.editarTarefa(tarefaParaEditar!!, titulo, projId, prio, est, real, prazo, estado)
                    tarefaParaEditar = null
                }
            )
        }

        // MODAL HUD DE EDIÇÃO RÁPIDA DE PROJETO
        if (projetoParaEditar != null) {
            ModalEditarProjeto(
                projeto = projetoParaEditar!!,
                areas = areas,
                onDismiss = { projetoParaEditar = null },
                onConfirm = { nome, areaId, obj, dataAlvo, estado, tipoProj, unid, vAtual, vTotal ->
                    viewModel.editarProjeto(projetoParaEditar!!, nome, areaId, obj, dataAlvo, estado)
                    if (vTotal != projetoParaEditar!!.valorTotal || vAtual != projetoParaEditar!!.valorAtual || unid != projetoParaEditar!!.unidadeMedida || tipoProj != projetoParaEditar!!.tipoProjeto) {
                        viewModel.atualizarMetadadosProgressoProjeto(projetoParaEditar!!, tipoProj, unid, vAtual, vTotal)
                    }
                    projetoParaEditar = null
                }
            )
        }
    }
}

@Composable
fun NavegacaoSubVistasDestino(
    destinoPrincipal: Int,
    abaAtual: String,
    onSelectAba: (String) -> Unit
) {
    val subAbas = when (destinoPrincipal) {
        1 -> listOf("tarefas" to "Lista & Busca", "kanban" to "Quadro Kanban", "prontas" to "Prontas p/ Agir", "dependencias" to "Dependências")
        2 -> listOf("projetos" to "Projetos", "areas" to "Áreas", "objetivos" to "Objetivos", "marcos" to "Marcos", "previsoes" to "Previsões")
        3 -> listOf("agenda" to "Sessões & Capacidade", "revisoes" to "Revisões Espaçadas")
        4 -> listOf("semanal" to "Revisão Semanal, Backup & Testes", "previsoes" to "Calibração K_calib")
        else -> emptyList()
    }

    if (subAbas.isNotEmpty()) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(subAbas) { (chave, titulo) ->
                val selecionada = abaAtual == chave
                Surface(
                    color = if (selecionada) CyberCores.NeonCyan else CyberCores.BgSurface,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (selecionada) CyberCores.NeonCyan else Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier.clickable { onSelectAba(chave) }
                ) {
                    Text(
                        text = titulo,
                        color = if (selecionada) CyberCores.BgBase else CyberCores.TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModalCapturaRapidaHonesta(
    projetos: List<Projeto>,
    hojeIso: String,
    onDismiss: () -> Unit,
    onSalvarTarefa: (String, String?, String, Int, String?) -> Unit,
    onIrParaAgenda: () -> Unit,
    onIrParaRevisoes: () -> Unit
) {
    val context = LocalContext.current
    var titulo by remember { mutableStateOf("") }
    var erroTitulo by remember { mutableStateOf(false) }
    var expandirDetalhes by remember { mutableStateOf(false) }
    var projId by remember { mutableStateOf<String?>(null) }
    var prioridade by remember { mutableStateOf("Média") }
    var estimativaStr by remember { mutableStateOf("") }
    var prazoStr by remember { mutableStateOf("") }

    val amanhaIso = remember {
        val c = Calendar.getInstance()
        c.add(Calendar.DAY_OF_YEAR, 1)
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(c.time)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberCores.BgSurface,
        title = { Text("Captura rápida", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it; if (it.isNotBlank()) erroTitulo = false },
                    label = { Text("Título da tarefa *", color = CyberCores.TextDim) },
                    isError = erroTitulo,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                if (erroTitulo) {
                    Text("Digite um título antes de salvar.", color = CyberCores.NeonMagenta, fontSize = 11.sp)
                }

                TextButton(
                    onClick = { expandirDetalhes = !expandirDetalhes },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = if (expandirDetalhes) "▾ Ocultar detalhes opcionais" else "▸ Mais detalhes (opcional: projeto, prazo, estimativa)",
                        color = CyberCores.NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (expandirDetalhes) {
                    if (projetos.isNotEmpty()) {
                        Text("Projeto (opcional):", color = CyberCores.TextDim, fontSize = 11.sp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item {
                                FilterChip(
                                    selected = projId == null,
                                    onClick = { projId = null },
                                    label = { Text("Nenhum", fontSize = 11.sp) }
                                )
                            }
                            items(projetos) { p ->
                                FilterChip(
                                    selected = projId == p.id,
                                    onClick = { projId = if (projId == p.id) null else p.id },
                                    label = { Text(p.nome, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    Text("Prioridade:", color = CyberCores.TextDim, fontSize = 11.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("Baixa", "Média", "Alta", "Crítica")) { pr ->
                            FilterChip(
                                selected = prioridade == pr,
                                onClick = { prioridade = pr },
                                label = { Text(pr, fontSize = 11.sp) }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = estimativaStr,
                            onValueChange = { estimativaStr = it.filter { c -> c.isDigit() } },
                            label = { Text("Est. min (opc.)", color = CyberCores.TextDim) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        OutlinedButton(
                            onClick = { abrirSeletorDataIso(context, prazoStr) { prazoStr = it } },
                            border = BorderStroke(1.dp, CyberCores.NeonCyan),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text(
                                text = if (prazoStr.isBlank()) "📅 Escolher prazo" else "📅 $prazoStr",
                                color = CyberCores.NeonCyan,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { prazoStr = hojeIso }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                            Text("Hoje", color = CyberCores.NeonCyan, fontSize = 11.sp)
                        }
                        OutlinedButton(onClick = { prazoStr = amanhaIso }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                            Text("Amanhã", color = CyberCores.NeonCyan, fontSize = 11.sp)
                        }
                        TextButton(onClick = { prazoStr = "" }) {
                            Text("Sem prazo", color = CyberCores.TextDim, fontSize = 11.sp)
                        }
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.08f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onIrParaRevisoes) { Text("+ Nova revisão", color = CyberCores.NeonPurple, fontSize = 11.sp) }
                    TextButton(onClick = onIrParaAgenda) { Text("+ Agendar sessão", color = CyberCores.NeonCyan, fontSize = 11.sp) }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (titulo.isBlank()) {
                        erroTitulo = true
                    } else {
                        onSalvarTarefa(
                            titulo.trim(),
                            projId,
                            prioridade,
                            estimativaStr.toIntOrNull() ?: 0,
                            prazoStr.takeIf { it.isNotBlank() }
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCores.NeonCyan)
            ) { Text("Salvar tarefa", color = Color.Black, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = CyberCores.TextDim) }
        }
    )
}

// =============================================================================
// 1. ECRÃS DE EXECUÇÃO DIÁRIA (HOJE, PRONTAS P/ AGIR, TAREFAS, KANBAN MOBILE)
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranHojeDashboard(
    tarefas: List<Tarefa>,
    projetos: List<Projeto>,
    revisoes: List<Revisao>,
    mapaBloqueios: Map<String, InfoBloqueioTarefa>,
    viewModel: MainViewModel,
    onNavigate: (Int, String) -> Unit,
    onOpenConcluir: (Tarefa) -> Unit,
    onOpenEditar: (Tarefa) -> Unit
) {
    val hoje = viewModel.hojeIso()
    val fimHojeMs = viewModel.fimDeHojeMs()

    val ativas = tarefas.filter { it.estado != "Concluída" }
    val atrasadas = ativas.filter { !it.prazoData.isNullOrBlank() && it.prazoData < hoje }
    val vencemHoje = ativas.filter { it.prazoData == hoje }
    val revisoesHoje = revisoes.filter { it.estado != "Concluída" && it.dataAgendada <= fimHojeMs }

    val acoesPrioritarias = remember(ativas, atrasadas, vencemHoje, mapaBloqueios) {
        val prioritarias = (atrasadas + vencemHoje + ativas.filter {
            it.estado == "Em andamento" || (mapaBloqueios[it.id]?.ehGargalo == true)
        }).distinctBy { it.id }
        if (prioritarias.isNotEmpty()) prioritarias else ativas.filter { mapaBloqueios[it.id]?.bloqueada != true }.take(5)
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 80.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // SEÇÃO 1: RESUMO DO DIA
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("RESUMO DO DIA", color = CyberCores.TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CardIndicadorHoje(
                        valor = atrasadas.size.toString(),
                        rotulo = "Atrasadas",
                        corDestaque = if (atrasadas.isNotEmpty()) CyberCores.NeonMagenta else CyberCores.TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    CardIndicadorHoje(
                        valor = vencemHoje.size.toString(),
                        rotulo = "Hoje",
                        corDestaque = CyberCores.NeonCyan,
                        modifier = Modifier.weight(1f)
                    )
                    CardIndicadorHoje(
                        valor = revisoesHoje.size.toString(),
                        rotulo = "Revisão",
                        corDestaque = CyberCores.NeonPurple,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // SEÇÃO 2: PRÓXIMAS AÇÕES DO DIA (REVISÕES DE HOJE + TAREFAS PRIORITÁRIAS)
        item {
            Text("PRÓXIMAS AÇÕES DO DIA", color = CyberCores.TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        if (revisoesHoje.isEmpty() && acoesPrioritarias.isEmpty()) {
            item {
                EstadoVazioAcionavel(
                    texto = "Nenhuma revisão vencendo hoje ou tarefa urgente pendente.",
                    rotuloBotao = "Ver todas as tarefas / Gerar teste em Mais",
                    onClick = { onNavigate(1, "tarefas") }
                )
            }
        }

        // Revisões vencendo hoje
        items(revisoesHoje, key = { "rev_${it.id}" }) { rev ->
            val dataFmt = SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(rev.dataAgendada))
            Surface(
                color = CyberCores.BgCard,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, CyberCores.NeonPurple.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "📖 [REVISÃO ${rev.regraIntervalo}] ${rev.titulo}",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Agendada para $dataFmt • Ciclo ${rev.regraIntervalo}",
                        color = CyberCores.TextDim,
                        fontSize = 12.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.concluirRevisao(rev, agendarProximoCiclo = true) },
                            border = BorderStroke(1.dp, CyberCores.NeonCyan),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Concluir (+Agendar próx.)", color = CyberCores.NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { viewModel.concluirRevisao(rev, agendarProximoCiclo = false) },
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                        ) {
                            Text("Concluir fim", color = CyberCores.TextPrimary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Tarefas prioritárias do dia
        items(acoesPrioritarias, key = { "tar_${it.id}" }) { t ->
            val info = mapaBloqueios[t.id] ?: InfoBloqueioTarefa()
            val isAtrasada = !t.prazoData.isNullOrBlank() && t.prazoData < hoje
            val nomeProj = projetos.find { it.id == t.projetoId }?.nome

            val corBorda = when {
                isAtrasada -> CyberCores.NeonMagenta
                info.bloqueada -> CyberCores.CyberYellow
                info.ehGargalo || info.pertenceCaminhoCritico -> CyberCores.NeonCyan
                else -> Color.White.copy(alpha = 0.14f)
            }

            Surface(
                color = CyberCores.BgCard,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, corBorda),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenEditar(t) }
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = buildString {
                            if (isAtrasada) append("🔥 [ATRASADA] ")
                            else if (t.prazoData == hoje) append("⏰ [HOJE] ")
                            append(t.titulo)
                        },
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    val metaPartes = mutableListOf<String>()
                    if (!nomeProj.isNullOrBlank()) metaPartes.add(nomeProj)
                    if (!t.prazoData.isNullOrBlank()) metaPartes.add("Prazo: ${t.prazoData}")
                    if (t.estimativaMinutos > 0) metaPartes.add("${t.estimativaMinutos}m est.")
                    if (metaPartes.isNotEmpty()) {
                        Text(metaPartes.joinToString(" • "), color = CyberCores.TextDim, fontSize = 12.sp)
                    }

                    if (info.qtdSucessorasTravadas > 0) {
                        Text(
                            text = "⚡ Destrava ${info.qtdSucessorasTravadas} tarefa(s)",
                            color = CyberCores.CyberYellow,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (info.bloqueada) {
                        Text(
                            text = "⚠ Aguarda: ${info.titulosBloqueadores.joinToString()}",
                            color = CyberCores.CyberYellow,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!info.bloqueada) {
                            OutlinedButton(
                                onClick = { viewModel.iniciarFoco(t) },
                                border = BorderStroke(1.dp, CyberCores.NeonCyan),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("▶ Focar", color = CyberCores.NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        OutlinedButton(
                            onClick = { onOpenConcluir(t) },
                            border = BorderStroke(1.dp, CyberCores.NeonGreen.copy(alpha = 0.7f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("✓ Concluir", color = CyberCores.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = { onOpenEditar(t) }) {
                            Text("Editar", color = CyberCores.TextDim, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // SEÇÃO 3: REGISTRO RÁPIDO NOS PROJETOS ATIVOS
        if (projetos.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text("ATUALIZAÇÕES RÁPIDAS NOS PROJETOS", color = CyberCores.TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            items(projetos.filter { it.estado == "Ativo" }, key = { "proj_hoje_${it.id}" }) { p ->
                val tarefasProj = tarefas.filter { it.projetoId == p.id }
                val concluidas = tarefasProj.count { it.estado == "Concluída" }
                val resumoProgresso = if (p.valorTotal > 0) {
                    "${p.valorAtual}/${p.valorTotal} ${p.unidadeMedida}"
                } else {
                    "$concluidas/${tarefasProj.size} tarefas concluídas"
                }

                Surface(
                    color = CyberCores.BgCard,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("📚 ${p.nome}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(resumoProgresso, color = CyberCores.TextDim, fontSize = 12.sp)
                        }
                        if (p.valorTotal > 0) {
                            OutlinedButton(
                                onClick = { viewModel.registrarProgressoProjeto(p, 1) },
                                border = BorderStroke(1.dp, CyberCores.NeonCyan),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("+ 1 ${p.unidadeMedida}", color = CyberCores.NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { onNavigate(2, "projetos") },
                                border = BorderStroke(1.dp, CyberCores.NeonCyan.copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Abrir projeto", color = CyberCores.NeonCyan, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CardIndicadorHoje(
    valor: String,
    rotulo: String,
    corDestaque: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = CyberCores.BgCard,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(valor, color = corDestaque, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Text(rotulo, color = CyberCores.TextPrimary, fontSize = 12.sp)
        }
    }
}

@Composable
fun EcranProntasParaAgir(
    tarefas: List<Tarefa>,
    projetos: List<Projeto>,
    mapaBloqueios: Map<String, InfoBloqueioTarefa>,
    viewModel: MainViewModel,
    onNavigate: (Int, String) -> Unit,
    onOpenConcluir: (Tarefa) -> Unit,
    onOpenEditar: (Tarefa) -> Unit
) {
    val projetosAtivosIds = projetos.filter { it.estado == "Ativo" }.map { it.id }.toSet()
    val prontas = tarefas.filter { t ->
        t.estado != "Concluída" &&
                t.estado != "Pausada" &&
                t.projetoId != null &&
                projetosAtivosIds.contains(t.projetoId) &&
                t.estimativaMinutos > 0 &&
                (mapaBloqueios[t.id]?.bloqueada != true)
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
        item {
            Text(
                "FILA DE EXECUÇÃO PURA (${prontas.size}) — Projeto Ativo + Estimativa > 0 + Sem Bloqueios",
                color = CyberCores.NeonGreen,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        if (prontas.isEmpty()) {
            item {
                EstadoVazioAcionavel(
                    texto = "> Nenhuma tarefa elegível no momento. Adicione uma estimativa de minutos e vincule a um projeto ativo.",
                    rotuloBotao = "+ Cadastrar Tarefa Completa",
                    onClick = { onNavigate(1, "tarefas") }
                )
            }
        } else {
            items(prontas) { t ->
                CartaoTarefaNivel2(
                    tarefa = t,
                    projetoNome = projetos.find { it.id == t.projetoId }?.nome,
                    infoBloqueio = mapaBloqueios[t.id] ?: InfoBloqueioTarefa(),
                    hojeIso = viewModel.hojeIso(),
                    onFocar = { viewModel.iniciarFoco(t) },
                    onEditar = { onOpenEditar(t) },
                    onConcluirOuReabrir = { onOpenConcluir(t) },
                    onArquivar = { viewModel.arquivarTarefa(t) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranTarefasLista(
    tarefas: List<Tarefa>,
    projetos: List<Projeto>,
    mapaBloqueios: Map<String, InfoBloqueioTarefa>,
    viewModel: MainViewModel,
    onOpenConcluir: (Tarefa) -> Unit,
    onOpenEditar: (Tarefa) -> Unit
) {
    val context = LocalContext.current
    var titulo by remember { mutableStateOf("") }
    var estimativa by remember { mutableStateOf("") }
    var prazo by remember { mutableStateOf("") }
    var prioridade by remember { mutableStateOf("Média") }
    var projetoIdSelecionado by remember { mutableStateOf<String?>(null) }

    var buscaTexto by remember { mutableStateOf("") }
    var filtroEstado by remember { mutableStateOf<String?>(null) }
    var filtroPrioridade by remember { mutableStateOf<String?>(null) }

    val tarefasFiltradas = remember(tarefas, buscaTexto, filtroEstado, filtroPrioridade) {
        tarefas.filter { t ->
            val matchTexto = buscaTexto.isBlank() || t.titulo.contains(buscaTexto, ignoreCase = true)
            val matchEstado = filtroEstado == null || t.estado == filtroEstado
            val matchPrio = filtroPrioridade == null || t.prioridade == filtroPrioridade
            matchTexto && matchEstado && matchPrio
        }
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
        item {
            HudCard("> NOVA TAREFA ESTRUTURADA") {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Título da Tarefa *", color = CyberCores.TextDim) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = estimativa,
                        onValueChange = { estimativa = it.filter { c -> c.isDigit() } },
                        label = { Text("Estimativa (min)", color = CyberCores.TextDim) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedButton(
                        onClick = { abrirSeletorDataIso(context, prazo) { prazo = it } },
                        border = BorderStroke(1.dp, CyberCores.NeonCyan),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Text(
                            text = if (prazo.isBlank()) "📅 Prazo (opcional)" else "📅 $prazo",
                            color = CyberCores.NeonCyan,
                            fontSize = 11.sp
                        )
                    }
                    if (prazo.isNotBlank()) {
                        TextButton(onClick = { prazo = "" }) { Text("✕", color = CyberCores.TextDim) }
                    }
                }
                if (projetos.isNotEmpty()) {
                    Text("Vincular ao Projeto (opcional):", color = CyberCores.TextDim, fontSize = 11.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = projetoIdSelecionado == null,
                                onClick = { projetoIdSelecionado = null },
                                label = { Text("Sem Projeto", fontSize = 11.sp) }
                            )
                        }
                        items(projetos) { p ->
                            val sel = projetoIdSelecionado == p.id
                            FilterChip(
                                selected = sel,
                                onClick = { projetoIdSelecionado = if (sel) null else p.id },
                                label = { Text(p.nome, fontSize = 11.sp) }
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOf("Baixa", "Média", "Alta", "Crítica").forEach { pr ->
                        FilterChip(
                            selected = prioridade == pr,
                            onClick = { prioridade = pr },
                            label = { Text(pr, fontSize = 11.sp) }
                        )
                    }
                }
                Button(
                    onClick = {
                        viewModel.criarTarefaCompleta(
                            titulo = titulo,
                            projetoId = projetoIdSelecionado,
                            prioridade = prioridade,
                            estimativaMinutos = estimativa.toIntOrNull() ?: 0,
                            prazoData = prazo.takeIf { it.isNotBlank() }
                        )
                        titulo = ""
                        estimativa = ""
                        prazo = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCores.NeonCyan)
                ) {
                    Text("ADICIONAR TAREFA", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            HudCard("> FILTRAR FILA (${tarefasFiltradas.size} de ${tarefas.size})") {
                OutlinedTextField(
                    value = buscaTexto,
                    onValueChange = { buscaTexto = it },
                    label = { Text("🔍 Buscar por título...", color = CyberCores.TextDim) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(
                            selected = filtroEstado == null,
                            onClick = { filtroEstado = null },
                            label = { Text("Todos Estados", fontSize = 11.sp) }
                        )
                    }
                    val estados = listOf("Não iniciada", "Em andamento", "Aguardando/Bloqueada", "Pausada", "Concluída")
                    items(estados) { st ->
                        FilterChip(
                            selected = filtroEstado == st,
                            onClick = { filtroEstado = if (filtroEstado == st) null else st },
                            label = { Text(st, fontSize = 11.sp) }
                        )
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(
                            selected = filtroPrioridade == null,
                            onClick = { filtroPrioridade = null },
                            label = { Text("Todas Prioridades", fontSize = 11.sp) }
                        )
                    }
                    val prios = listOf("Crítica", "Alta", "Média", "Baixa")
                    items(prios) { pr ->
                        FilterChip(
                            selected = filtroPrioridade == pr,
                            onClick = { filtroPrioridade = if (filtroPrioridade == pr) null else pr },
                            label = { Text(pr, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        items(tarefasFiltradas) { t ->
            CartaoTarefaNivel2(
                tarefa = t,
                projetoNome = projetos.find { it.id == t.projetoId }?.nome,
                infoBloqueio = mapaBloqueios[t.id] ?: InfoBloqueioTarefa(),
                hojeIso = viewModel.hojeIso(),
                onFocar = { viewModel.iniciarFoco(t) },
                onEditar = { onOpenEditar(t) },
                onConcluirOuReabrir = {
                    if (t.estado == "Concluída") viewModel.atualizarEstadoTarefa(t, "Não iniciada")
                    else onOpenConcluir(t)
                },
                onArquivar = { viewModel.arquivarTarefa(t) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranQuadroKanban(
    tarefas: List<Tarefa>,
    mapaBloqueios: Map<String, InfoBloqueioTarefa>,
    viewModel: MainViewModel,
    onOpenConcluir: (Tarefa) -> Unit,
    onOpenEditar: (Tarefa) -> Unit
) {
    val colunas = listOf("Não iniciada", "Em andamento", "Aguardando/Bloqueada", "Pausada", "Concluída")
    var estagioSelecionado by remember { mutableIntStateOf(0) }
    var tarefaParaMoverMenu by remember { mutableStateOf<Tarefa?>(null) }

    // Sincroniza o estado efetivo no Kanban com o bloqueio derivado das dependências:
    // - Se tem bloqueador pendente e não está concluída -> aparece em "Aguardando/Bloqueada"
    // - Se estava em "Aguardando/Bloqueada" mas os bloqueadores foram concluídos -> aparece livre em "Não iniciada"
    fun estadoEfetivoKanban(t: Tarefa): String {
        if (t.estado == "Concluída") return "Concluída"
        val bloqueadaDerivada = mapaBloqueios[t.id]?.bloqueada == true
        if (bloqueadaDerivada) return "Aguardando/Bloqueada"
        if (t.estado == "Aguardando/Bloqueada") return "Não iniciada"
        return t.estado
    }

    val colStatus = colunas[estagioSelecionado]
    val tarefasDaColuna = tarefas.filter { estadoEfetivoKanban(it) == colStatus }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Seletor de Estágio Mobile (Pílulas com contador)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(colunas.indices.toList()) { idx ->
                val nomeEstagio = colunas[idx]
                val qtd = tarefas.count { estadoEfetivoKanban(it) == nomeEstagio }
                val ativo = estagioSelecionado == idx
                FilterChip(
                    selected = ativo,
                    onClick = { estagioSelecionado = idx },
                    label = {
                        Text(
                            text = "$nomeEstagio ($qtd)",
                            fontSize = 12.sp,
                            fontWeight = if (ativo) FontWeight.ExtraBold else FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyberCores.NeonCyan.copy(alpha = 0.22f),
                        selectedLabelColor = CyberCores.NeonCyan
                    )
                )
            }
        }

        // Lista em 100% da largura da tela do telemóvel
        if (tarefasDaColuna.isEmpty()) {
            Surface(
                color = CyberCores.BgCard,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Nenhuma tarefa no estágio '$colStatus'.",
                    color = CyberCores.TextDim,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(tarefasDaColuna, key = { "kb_${it.id}" }) { t ->
                    val info = mapaBloqueios[t.id] ?: InfoBloqueioTarefa()
                    Surface(
                        color = CyberCores.BgCard,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(
                            1.dp,
                            when {
                                info.ehGargalo -> CyberCores.NeonMagenta
                                info.bloqueada -> CyberCores.CyberYellow
                                info.pertenceCaminhoCritico -> CyberCores.NeonCyan
                                else -> Color.White.copy(alpha = 0.12f)
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenEditar(t) }
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = t.titulo,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = { onOpenEditar(t) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Text("✎ Editar", color = CyberCores.NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                BadgeTag(t.prioridade, if (t.prioridade == "Alta" || t.prioridade == "Crítica") CyberCores.NeonMagenta else CyberCores.TextDim)
                                BadgeTag(
                                    if (t.estimativaMinutos > 0) "${t.estimativaMinutos}m est. / ${t.minutosReais}m real" else "Sem estimativa",
                                    if (t.estimativaMinutos > 0) CyberCores.NeonGreen else CyberCores.CyberYellow
                                )
                            }

                            if (info.pertenceCaminhoCritico) Text("⚡ Caminho Crítico", color = CyberCores.NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            if (info.ehGargalo) Text("🔥 Gargalo: destrava ${info.qtdSucessorasTravadas} tarefa(s)", color = CyberCores.NeonMagenta, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            if (info.bloqueada) Text("⚠ Aguarda conclusão de: ${info.titulosBloqueadores.joinToString()}", color = CyberCores.CyberYellow, fontSize = 12.sp)

                            // Linha 1: Focar, Concluir e Mover para...
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (t.estado != "Concluída" && !info.bloqueada) {
                                    OutlinedButton(
                                        onClick = { viewModel.iniciarFoco(t) },
                                        border = BorderStroke(1.dp, CyberCores.NeonGreen),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("▶ Focar", color = CyberCores.NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (t.estado != "Concluída") {
                                    Button(
                                        onClick = { onOpenConcluir(t) },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyberCores.NeonCyan),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("✓ Concluir", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                OutlinedButton(
                                    onClick = { tarefaParaMoverMenu = t },
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                                ) {
                                    Text("Mover para...", color = CyberCores.TextPrimary, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (tarefaParaMoverMenu != null) {
        val tAlvo = tarefaParaMoverMenu!!
        AlertDialog(
            onDismissRequest = { tarefaParaMoverMenu = null },
            containerColor = CyberCores.BgSurface,
            title = { Text("Mover tarefa para estágio", color = CyberCores.NeonCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(tAlvo.titulo, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    colunas.forEachIndexed { idx, estagio ->
                        OutlinedButton(
                            onClick = {
                                tarefaParaMoverMenu = null
                                if (estagio == "Concluída") {
                                    onOpenConcluir(tAlvo)
                                } else {
                                    viewModel.atualizarEstadoTarefa(tAlvo, estagio)
                                    estagioSelecionado = idx
                                }
                            },
                            border = BorderStroke(1.dp, if (tAlvo.estado == estagio) CyberCores.NeonCyan else Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = estagio,
                                color = if (tAlvo.estado == estagio) CyberCores.NeonCyan else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { tarefaParaMoverMenu = null }) { Text("Cancelar", color = CyberCores.TextDim) }
            }
        )
    }
}

// =============================================================================
// 2. ECRÃS DE ESTRUTURA HIERÁRQUICA (ÁREAS ➔ PROJETOS ➔ OBJETIVOS ➔ MARCOS)
// =============================================================================

@Composable
fun EcranAreas(areas: List<Area>, viewModel: MainViewModel) {
    var nome by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var corHex by remember { mutableStateOf("#00F0FF") }
    var areaParaEditar by remember { mutableStateOf<Area?>(null) }
    val coresOpcoes = listOf("#FF0055", "#00F0FF", "#00FF66", "#FFAA00", "#BD00FF")

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
        item {
            HudCard("> 1. NOVA ÁREA DE VIDA / ESTUDO") {
                OutlinedTextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome (ex: Livros, Faculdade)", color = CyberCores.TextDim) }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Descrição", color = CyberCores.TextDim) }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Cor Néon:", color = CyberCores.TextDim, fontSize = 12.sp)
                    coresOpcoes.forEach { hex ->
                        val c = CyberCores.parseHex(hex)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(c, CircleShape)
                                .border(2.dp, if (corHex == hex) Color.White else Color.Transparent, CircleShape)
                                .clickable { corHex = hex }
                        )
                    }
                }
                Button(
                    onClick = { viewModel.criarArea(nome, desc, corHex); nome = ""; desc = "" },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCores.NeonCyan)
                ) { Text("SALVAR ÁREA", color = Color.Black, fontWeight = FontWeight.Bold) }
            }
        }
        items(areas) { a ->
            Surface(
                color = CyberCores.BgCard,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, CyberCores.parseHex(a.cor))
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(a.nome, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        if (!a.descricao.isNullOrBlank()) Text(a.descricao, color = CyberCores.TextDim, fontSize = 12.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { areaParaEditar = a }) {
                            Text("Editar", color = CyberCores.NeonCyan, fontSize = 12.sp)
                        }
                        TextButton(onClick = { viewModel.arquivarArea(a) }) {
                            Text("Arquivar", color = CyberCores.NeonMagenta, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    if (areaParaEditar != null) {
        val areaOriginal = areaParaEditar!!
        var editNome by remember { mutableStateOf(areaOriginal.nome) }
        var editDesc by remember { mutableStateOf(areaOriginal.descricao ?: "") }
        var editCor by remember { mutableStateOf(areaOriginal.cor) }

        AlertDialog(
            onDismissRequest = { areaParaEditar = null },
            containerColor = CyberCores.BgSurface,
            title = { Text("Editar Área", color = CyberCores.NeonCyan, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = editNome, onValueChange = { editNome = it }, label = { Text("Nome") }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                    OutlinedTextField(value = editDesc, onValueChange = { editDesc = it }, label = { Text("Descrição") }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        coresOpcoes.forEach { hex ->
                            val c = CyberCores.parseHex(hex)
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(c, CircleShape)
                                    .border(2.dp, if (editCor == hex) Color.White else Color.Transparent, CircleShape)
                                    .clickable { editCor = hex }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.editarArea(areaOriginal, editNome, editDesc, editCor)
                        areaParaEditar = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCores.NeonCyan)
                ) { Text("Salvar", color = Color.Black, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { areaParaEditar = null }) { Text("Cancelar", color = CyberCores.TextDim) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranProjetos(
    projetos: List<Projeto>,
    areas: List<Area>,
    tarefas: List<Tarefa>,
    dependencias: List<Dependencia>,
    disponibilidade: List<Disponibilidade>,
    viewModel: MainViewModel,
    onOpenEditarProjeto: (Projeto) -> Unit
) {
    val context = LocalContext.current
    var nome by remember { mutableStateOf("") }
    var objetivo by remember { mutableStateOf("") }
    var dataAlvo by remember { mutableStateOf("") }
    var areaEscolhida by remember { mutableStateOf<String?>(null) }
    var tipoEscolhido by remember { mutableStateOf("TAREFAS") }
    var totalMetaStr by remember { mutableStateOf("") }

    val tiposOpcoes = listOf(
        Triple("TAREFAS", "Tarefas", "tarefas"),
        Triple("LIVRO", "Livro (Pág.)", "Pág."),
        Triple("IDIOMA", "Idioma (min)", "min"),
        Triple("DISCIPLINA", "Disciplina (Aula)", "Aula")
    )

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
        item {
            HudCard("> 2. NOVO PROJETO (FILHO DE ÁREA)") {
                OutlinedTextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome do Projeto *", color = CyberCores.TextDim) }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                OutlinedTextField(value = objetivo, onValueChange = { objetivo = it }, label = { Text("Objetivo / Entrega Final", color = CyberCores.TextDim) }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))

                Text("Modo de Progresso:", color = CyberCores.TextDim, fontSize = 11.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(tiposOpcoes) { (chave, rotulo, _) ->
                        FilterChip(
                            selected = tipoEscolhido == chave,
                            onClick = { tipoEscolhido = chave },
                            label = { Text(rotulo, fontSize = 11.sp) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (tipoEscolhido != "TAREFAS") {
                        val unidAtual = tiposOpcoes.first { it.first == tipoEscolhido }.third
                        OutlinedTextField(
                            value = totalMetaStr,
                            onValueChange = { totalMetaStr = it.filter { c -> c.isDigit() } },
                            label = { Text("Total ($unidAtual)", color = CyberCores.TextDim) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }
                    OutlinedButton(
                        onClick = { abrirSeletorDataIso(context, dataAlvo) { dataAlvo = it } },
                        border = BorderStroke(1.dp, CyberCores.NeonCyan),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Text(
                            text = if (dataAlvo.isBlank()) "📅 Data-Alvo (opc.)" else "📅 $dataAlvo",
                            color = CyberCores.NeonCyan,
                            fontSize = 11.sp
                        )
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(areas) { a ->
                        val sel = areaEscolhida == a.id
                        FilterChip(selected = sel, onClick = { areaEscolhida = a.id }, label = { Text(a.nome, fontSize = 11.sp) })
                    }
                }

                Button(
                    onClick = {
                        val unid = tiposOpcoes.first { it.first == tipoEscolhido }.third
                        viewModel.criarProjeto(
                            nome = nome,
                            areaId = areaEscolhida,
                            objetivo = objetivo,
                            dataAlvo = dataAlvo,
                            tipoProjeto = tipoEscolhido,
                            unidadeMedida = unid,
                            valorTotal = if (tipoEscolhido == "TAREFAS") 0 else (totalMetaStr.toIntOrNull() ?: 100)
                        )
                        nome = ""; objetivo = ""; dataAlvo = ""; totalMetaStr = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCores.NeonCyan)
                ) { Text("CRIAR PROJETO", color = Color.Black, fontWeight = FontWeight.Bold) }
            }
        }

        items(projetos) { p ->
            val projecao = viewModel.calcularProjecaoProjeto(p, tarefas, dependencias, disponibilidade)
            val area = areas.find { it.id == p.areaId }
            Surface(
                color = CyberCores.BgCard,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, CyberCores.parseHex(area?.cor))
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(p.nome, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedButton(
                                onClick = { onOpenEditarProjeto(p) },
                                border = BorderStroke(1.dp, CyberCores.NeonCyan),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) { Text("✎ Editar", color = CyberCores.NeonCyan, fontSize = 11.sp) }
                            TextButton(onClick = { viewModel.arquivarProjeto(p) }) { Text("Arquivar", color = CyberCores.NeonMagenta, fontSize = 11.sp) }
                        }
                    }
                    if (!p.objetivo.isNullOrBlank()) Text(p.objetivo, color = CyberCores.TextDim, fontSize = 12.sp)

                    if (p.valorTotal > 0) {
                        val pctProj = (p.valorAtual.toFloat() / p.valorTotal.toFloat()).coerceIn(0f, 1f)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Progresso: ${p.valorAtual} / ${p.valorTotal} ${p.unidadeMedida}", color = CyberCores.NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            OutlinedButton(
                                onClick = { viewModel.registrarProgressoProjeto(p, 1) },
                                border = BorderStroke(1.dp, CyberCores.NeonCyan),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) { Text("+ 1 ${p.unidadeMedida}", color = CyberCores.NeonCyan, fontSize = 11.sp) }
                        }
                        LinearProgressIndicator(
                            progress = pctProj,
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = CyberCores.NeonCyan,
                            trackColor = Color.White.copy(alpha = 0.08f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (area != null) BadgeTag(area.nome, CyberCores.parseHex(area.cor))
                        BadgeTag(p.estado, CyberCores.NeonCyan)
                        BadgeTag(
                            "Incerteza: ${projecao.tarefasSemEstimativa}/${projecao.totalPendentes} s/ est. (${projecao.taxaIncompletudePct}%)",
                            if (projecao.tarefasSemEstimativa > 0) CyberCores.CyberYellow else CyberCores.NeonGreen
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranObjetivos(objetivos: List<Objetivo>, projetos: List<Projeto>, tarefas: List<Tarefa>, viewModel: MainViewModel) {
    var titulo by remember { mutableStateOf("") }
    var horizonte by remember { mutableStateOf("Médio Prazo") }
    var projEscolhido by remember { mutableStateOf<String?>(null) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
        item {
            HudCard("> 3. NOVO OBJETIVO / META") {
                OutlinedTextField(value = titulo, onValueChange = { titulo = it }, label = { Text("Título do Objetivo *", color = CyberCores.TextDim) }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Curto Prazo", "Médio Prazo", "Longo Prazo").forEach { h ->
                        FilterChip(selected = horizonte == h, onClick = { horizonte = h }, label = { Text(h, fontSize = 11.sp) })
                    }
                }
                if (projetos.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(projetos) { p ->
                            FilterChip(
                                selected = projEscolhido == p.id,
                                onClick = { projEscolhido = if (projEscolhido == p.id) null else p.id },
                                label = { Text("Proj: ${p.nome}", fontSize = 11.sp) }
                            )
                        }
                    }
                }
                Button(
                    onClick = { viewModel.criarObjetivo(titulo, horizonte, projEscolhido, null); titulo = "" },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCores.NeonCyan)
                ) { Text("SALVAR OBJETIVO", color = Color.Black, fontWeight = FontWeight.Bold) }
            }
        }
        items(objetivos) { o ->
            val tarefasDoProj = tarefas.filter { it.projetoId != null && it.projetoId == o.projetoId }
            val concluidas = tarefasDoProj.count { it.estado == "Concluída" }
            val pct = if (tarefasDoProj.isNotEmpty()) ((concluidas.toFloat() / tarefasDoProj.size.toFloat()) * 100f).roundToInt() else if (o.estado == "Concluído") 100 else 0

            Surface(color = CyberCores.BgCard, shape = RoundedCornerShape(6.dp), border = BorderStroke(1.dp, CyberCores.NeonCyan.copy(alpha = 0.3f))) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(o.titulo, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("${o.horizonte} • ${o.estado} ($pct%)", color = CyberCores.TextDim, fontSize = 12.sp)
                        }
                        OutlinedButton(onClick = { viewModel.alternarEstadoObjetivo(o) }) {
                            Text(if (o.estado == "Concluído") "Reabrir" else "Concluir", color = CyberCores.NeonGreen, fontSize = 11.sp)
                        }
                    }
                    LinearProgressIndicator(
                        progress = (pct / 100f).coerceIn(0f, 1f),
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = CyberCores.NeonGreen,
                        trackColor = Color.White.copy(alpha = 0.08f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranMarcos(marcos: List<Marco>, projetos: List<Projeto>, viewModel: MainViewModel) {
    val context = LocalContext.current
    var titulo by remember { mutableStateOf("") }
    var dataAlvo by remember { mutableStateOf("") }
    var projEscolhido by remember { mutableStateOf<String?>(null) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
        item {
            HudCard("> 4. NOVO MARCO INTERMEDIÁRIO / PROVA") {
                OutlinedTextField(value = titulo, onValueChange = { titulo = it }, label = { Text("Título do Marco *", color = CyberCores.TextDim) }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                OutlinedButton(
                    onClick = { abrirSeletorDataIso(context, dataAlvo) { dataAlvo = it } },
                    border = BorderStroke(1.dp, CyberCores.NeonCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (dataAlvo.isBlank()) "📅 Escolher Data-Alvo (Prova / Entrega)" else "📅 Data-Alvo: $dataAlvo",
                        color = CyberCores.NeonCyan,
                        fontSize = 12.sp
                    )
                }
                if (projetos.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(projetos) { p ->
                            FilterChip(
                                selected = projEscolhido == p.id,
                                onClick = { projEscolhido = if (projEscolhido == p.id) null else p.id },
                                label = { Text("Proj: ${p.nome}", fontSize = 11.sp) }
                            )
                        }
                    }
                }
                Button(
                    onClick = { viewModel.criarMarco(titulo, projEscolhido ?: projetos.firstOrNull()?.id, dataAlvo); titulo = ""; dataAlvo = "" },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCores.NeonCyan)
                ) { Text("CRIAR MARCO", color = Color.Black, fontWeight = FontWeight.Bold) }
            }
        }
        items(marcos) { m ->
            Surface(color = CyberCores.BgCard, shape = RoundedCornerShape(6.dp), border = BorderStroke(1.dp, CyberCores.CyberYellow.copy(alpha = 0.4f))) {
                Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("🚩 ${m.titulo}", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Alvo: ${m.dataAlvo ?: "Sem data"} • ${m.estado}", color = CyberCores.TextDim, fontSize = 12.sp)
                    }
                    OutlinedButton(onClick = { viewModel.alternarEstadoMarco(m) }) {
                        Text(if (m.estado == "Concluído") "Reabrir" else "Concluir", color = CyberCores.NeonCyan, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// =============================================================================
// 3. FLUXO & AGENDA (SESSÕES COM DATA/HORA REAL, CAPACIDADE, DEPENDÊNCIAS, REVISÕES)
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranPlanejamentoAgenda(
    sessoes: List<Sessao>,
    disponibilidade: List<Disponibilidade>,
    tarefas: List<Tarefa>,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    var tituloSessao by remember { mutableStateOf("") }
    var dataSessaoIso by remember { mutableStateOf(viewModel.hojeIso()) }
    var horaInicioSessao by remember { mutableStateOf("21:00") }
    var duracaoMin by remember { mutableStateOf("60") }
    var tarefaIdEscolhida by remember { mutableStateOf<String?>(null) }

    var excDe by remember { mutableStateOf("") }
    var excAte by remember { mutableStateOf("") }
    var excIni by remember { mutableStateOf("00:00") }
    var excFim by remember { mutableStateOf("00:00") }

    val diasNomes = listOf("Domingo", "Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado")
    val fmtDataHora = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }

    // Pré-validação de conflito no formulário antes de salvar
    val inicioEscolhidoMs = remember(dataSessaoIso, horaInicioSessao) {
        combinarDataHoraMs(dataSessaoIso, horaInicioSessao)
    }
    val fimEscolhidoMs = remember(inicioEscolhidoMs, duracaoMin) {
        inicioEscolhidoMs + (max(15, duracaoMin.toIntOrNull() ?: 60) * 60_000L)
    }
    val sessaoConflitante = remember(sessoes, inicioEscolhidoMs, fimEscolhidoMs) {
        sessoes.find { s ->
            s.estado != "Concluída" && inicioEscolhidoMs < s.fimMs && fimEscolhidoMs > s.inicioMs
        }
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
        item {
            val (hPlan, hDisp, pct) = viewModel.calcularResumoCapacidade7Dias(sessoes, disponibilidade)
            val sobrecarga = hPlan > hDisp && hDisp > 0.0
            val corBarra = if (sobrecarga) CyberCores.NeonMagenta else CyberCores.NeonCyan

            HudCard("> CAPACIDADE CONFIGURADA VS CARGA PLANEJADA (7 DIAS)") {
                LinearProgressIndicator(
                    progress = (pct / 100f).coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = corBarra,
                    trackColor = Color.White.copy(alpha = 0.08f)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = if (sobrecarga) "⚠ SOBRECARGA: ${hPlan}h agendadas p/ ${hDisp}h disp. ($pct%)"
                        else "Sessões: ${hPlan}h / Disponível (7d): ${hDisp}h ($pct%)",
                        color = if (sobrecarga) CyberCores.NeonMagenta else CyberCores.TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${tarefas.count { it.estado != "Concluída" && it.estimativaMinutos <= 0 }} s/ est.",
                        color = CyberCores.CyberYellow,
                        fontSize = 11.sp
                    )
                }
            }
        }

        item {
            HudCard("> AGENDAR SESSÃO DE FOCO (DATA E HORA REAIS)") {
                OutlinedTextField(value = tituloSessao, onValueChange = { tituloSessao = it }, label = { Text("Título da Sessão *", color = CyberCores.TextDim) }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { abrirSeletorDataIso(context, dataSessaoIso) { dataSessaoIso = it } },
                        border = BorderStroke(1.dp, CyberCores.NeonCyan),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Text("📅 $dataSessaoIso", color = CyberCores.NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { abrirSeletorHora(context, horaInicioSessao) { horaInicioSessao = it } },
                        border = BorderStroke(1.dp, CyberCores.NeonCyan),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🕒 $horaInicioSessao", color = CyberCores.NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedTextField(
                        value = duracaoMin,
                        onValueChange = { duracaoMin = it.filter { c -> c.isDigit() } },
                        label = { Text("Min", color = CyberCores.TextDim) },
                        modifier = Modifier.weight(0.8f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }

                if (sessaoConflitante != null) {
                    Surface(
                        color = CyberCores.CyberYellow.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, CyberCores.CyberYellow),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "⚠ Conflito de horário com '${sessaoConflitante.titulo}' (${fmtDataHora.format(Date(sessaoConflitante.inicioMs))}).",
                            color = CyberCores.CyberYellow,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                val pendentes = tarefas.filter { it.estado != "Concluída" }
                if (pendentes.isNotEmpty()) {
                    Text("Vincular à Tarefa (Opcional):", color = CyberCores.TextDim, fontSize = 11.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(pendentes) { t ->
                            val sel = tarefaIdEscolhida == t.id
                            FilterChip(
                                selected = sel,
                                onClick = {
                                    tarefaIdEscolhida = if (sel) null else t.id
                                    if (!sel && tituloSessao.isBlank()) tituloSessao = "Foco: ${t.titulo}"
                                },
                                label = { Text(t.titulo, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        viewModel.criarSessao(
                            titulo = tituloSessao,
                            duracaoMinutos = duracaoMin.toIntOrNull() ?: 60,
                            tarefaId = tarefaIdEscolhida,
                            inicioPersonalizadoMs = inicioEscolhidoMs
                        )
                        tituloSessao = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCores.NeonCyan)
                ) { Text("AGENDAR SESSÃO", color = Color.Black, fontWeight = FontWeight.Bold) }
            }
        }

        items(sessoes) { s ->
            val inicioFmt = fmtDataHora.format(Date(s.inicioMs))
            Surface(color = CyberCores.BgCard, shape = RoundedCornerShape(6.dp), border = BorderStroke(1.dp, CyberCores.NeonCyan.copy(alpha = 0.3f))) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("⏱ ${s.titulo}", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("$inicioFmt • ${s.duracaoMinutos}m • ${s.estado}", color = CyberCores.TextDim, fontSize = 11.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.alternarEstadoSessao(s) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(if (s.estado == "Concluída") "Reabrir" else "Concluir", color = CyberCores.NeonGreen, fontSize = 10.sp)
                        }
                        Button(
                            onClick = { viewModel.sincronizarComCalendario(context, s) },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCores.NeonCyan),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Abrir no Calendar", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            HudCard("> PADRÃO BASE SEMANAL DE CAPACIDADE") {
                disponibilidade.filter { it.validoDe.isNullOrBlank() }.forEach { slot ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("${diasNomes.getOrElse(slot.diaSemana) { "Dia" }}: ${slot.horaInicio} às ${slot.horaFim}", color = if (slot.ativo) Color.White else CyberCores.TextDim, fontSize = 13.sp)
                        Switch(checked = slot.ativo, onCheckedChange = { viewModel.alternarSlotDisponibilidade(slot) })
                    }
                }
            }
        }

        item {
            HudCard("> EXCEÇÕES TEMPORAIS (SEMANA DE PROVAS / FÉRIAS)") {
                Text("Use 00:00 às 00:00 para férias (capacidade zero).", color = CyberCores.TextDim, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { abrirSeletorDataIso(context, excDe) { excDe = it } },
                        border = BorderStroke(1.dp, CyberCores.NeonCyan),
                        modifier = Modifier.weight(1f)
                    ) { Text(if (excDe.isBlank()) "📅 De..." else "📅 De: $excDe", color = CyberCores.NeonCyan, fontSize = 11.sp) }

                    OutlinedButton(
                        onClick = { abrirSeletorDataIso(context, excAte) { excAte = it } },
                        border = BorderStroke(1.dp, CyberCores.NeonCyan),
                        modifier = Modifier.weight(1f)
                    ) { Text(if (excAte.isBlank()) "📅 Até..." else "📅 Até: $excAte", color = CyberCores.NeonCyan, fontSize = 11.sp) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { abrirSeletorHora(context, excIni) { excIni = it } },
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        modifier = Modifier.weight(1f)
                    ) { Text("🕒 Início: $excIni", color = Color.White, fontSize = 11.sp) }

                    OutlinedButton(
                        onClick = { abrirSeletorHora(context, excFim) { excFim = it } },
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        modifier = Modifier.weight(1f)
                    ) { Text("🕒 Fim: $excFim", color = Color.White, fontSize = 11.sp) }
                }
                Button(
                    onClick = { viewModel.criarExcecaoDisponibilidade(-1, excDe, excAte, excIni, excFim); excDe = ""; excAte = "" },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCores.CyberYellow)
                ) { Text("ADICIONAR EXCEÇÃO TEMPORAL", color = Color.Black, fontWeight = FontWeight.Bold) }

                disponibilidade.filter { !it.validoDe.isNullOrBlank() }.forEach { exc ->
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("⚡ ${exc.validoDe} a ${exc.validoAte} (${exc.horaInicio}-${exc.horaFim})", color = CyberCores.NeonCyan, fontSize = 12.sp)
                        TextButton(onClick = { viewModel.removerDisponibilidade(exc.id) }) { Text("Remover", color = CyberCores.NeonMagenta, fontSize = 11.sp) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranDependencias(dependencias: List<Dependencia>, tarefas: List<Tarefa>, viewModel: MainViewModel) {
    var predId by remember { mutableStateOf<String?>(null) }
    var succId by remember { mutableStateOf<String?>(null) }
    var buscaTarefa by remember { mutableStateOf("") }

    val tarefasFiltradas = remember(tarefas, buscaTarefa) {
        if (buscaTarefa.isBlank()) tarefas else tarefas.filter { it.titulo.contains(buscaTarefa, ignoreCase = true) }
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
        item {
            HudCard("> VINCULAR DEPENDÊNCIA DIRECIONADA (A ➔ B)") {
                OutlinedTextField(
                    value = buscaTarefa,
                    onValueChange = { buscaTarefa = it },
                    label = { Text("🔍 Filtrar tarefas para vincular...", color = CyberCores.TextDim) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Text("1. Selecione a Tarefa Bloqueadora (Predecessora A):", color = CyberCores.TextDim, fontSize = 11.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(tarefasFiltradas) { t ->
                        FilterChip(selected = predId == t.id, onClick = { predId = t.id }, label = { Text(t.titulo, fontSize = 11.sp) })
                    }
                }
                Text("2. Selecione a Tarefa que Aguarda (Sucessora B):", color = CyberCores.TextDim, fontSize = 11.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(tarefasFiltradas) { t ->
                        FilterChip(selected = succId == t.id, onClick = { succId = t.id }, label = { Text(t.titulo, fontSize = 11.sp) })
                    }
                }
                Button(
                    onClick = { viewModel.criarDependencia(predId ?: "", succId ?: "", "") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCores.NeonCyan)
                ) { Text("VINCULAR BLOQUEIO (ANTI-CICLO)", color = Color.Black, fontWeight = FontWeight.Bold) }
            }
        }

        items(dependencias) { dep ->
            val pred = tarefas.find { it.id == dep.predecessoraId }
            val succ = tarefas.find { it.id == dep.sucessoraId }
            val resolvido = pred?.estado == "Concluída"
            Surface(
                color = CyberCores.BgCard,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, if (resolvido) CyberCores.NeonGreen else CyberCores.CyberYellow)
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${succ?.titulo ?: "?"} AGUARDA ➔ ${pred?.titulo ?: "?"}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(if (resolvido) "✓ Bloqueio Liberado" else "⚠ Bloqueio Ativo", color = if (resolvido) CyberCores.NeonGreen else CyberCores.CyberYellow, fontSize = 11.sp)
                    }
                    TextButton(onClick = { viewModel.removerDependencia(dep.id) }) {
                        Text("Remover", color = CyberCores.NeonMagenta, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranRevisoesEstudo(revisoes: List<Revisao>, viewModel: MainViewModel) {
    val context = LocalContext.current
    var titulo by remember { mutableStateOf("") }
    var intervalo by remember { mutableStateOf("D+1") }
    var dataPersonalizadaIso by remember { mutableStateOf("") }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
        item {
            HudCard("> AGENDAR REVISÃO ESPAÇADA (D+1 ➔ D+3 ➔ D+7 ➔ D+15)") {
                OutlinedTextField(value = titulo, onValueChange = { titulo = it }, label = { Text("Tema / Aula / Capítulo *", color = CyberCores.TextDim) }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOf("D+1", "D+3", "D+7", "D+15").forEach { r ->
                        FilterChip(selected = intervalo == r, onClick = { intervalo = r; dataPersonalizadaIso = "" }, label = { Text(r, fontSize = 11.sp) })
                    }
                }
                OutlinedButton(
                    onClick = { abrirSeletorDataIso(context, dataPersonalizadaIso) { dataPersonalizadaIso = it } },
                    border = BorderStroke(1.dp, CyberCores.NeonPurple),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (dataPersonalizadaIso.isBlank()) "📅 Ou escolher data específica (opcional)" else "📅 Agendada para: $dataPersonalizadaIso",
                        color = CyberCores.NeonPurple,
                        fontSize = 11.sp
                    )
                }
                Button(
                    onClick = {
                        val msCustom = if (dataPersonalizadaIso.isNotBlank()) combinarDataHoraMs(dataPersonalizadaIso, "09:00") else null
                        viewModel.criarRevisao(titulo, intervalo, "", msCustom)
                        titulo = ""
                        dataPersonalizadaIso = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCores.NeonPurple)
                ) { Text("AGENDAR REVISÃO", color = Color.White, fontWeight = FontWeight.Bold) }
            }
        }
        items(revisoes) { rev ->
            val dataFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(rev.dataAgendada))
            Surface(color = CyberCores.BgCard, shape = RoundedCornerShape(6.dp), border = BorderStroke(1.dp, CyberCores.NeonPurple.copy(alpha = 0.5f))) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("📖 ${rev.titulo}", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Ciclo: ${rev.regraIntervalo} • Data: $dataFmt • ${rev.estado}", color = CyberCores.TextDim, fontSize = 12.sp)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.concluirRevisao(rev, agendarProximoCiclo = true) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (rev.estado == "Concluída") "Reabrir" else "Concluir (+Próx)", color = CyberCores.NeonGreen, fontSize = 11.sp)
                        }
                        if (rev.estado != "Concluída") {
                            OutlinedButton(
                                onClick = { viewModel.concluirRevisao(rev, agendarProximoCiclo = false) }
                            ) {
                                Text("Encerrar ciclo", color = CyberCores.TextPrimary, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// 4. INTELIGÊNCIA NÍVEL 2 (PREVISÕES PROBABILÍSTICAS K_CALIB & REVISÃO SEMANAL)
// =============================================================================

@Composable
fun EcranPrevisoesNivel2(
    projetos: List<Projeto>,
    tarefas: List<Tarefa>,
    dependencias: List<Dependencia>,
    disponibilidade: List<Disponibilidade>,
    viewModel: MainViewModel,
    onNavigate: (Int, String) -> Unit
) {
    val (kGlobal, estGlobal, actGlobal) = viewModel.calcularKCalibGlobal(tarefas)

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
        item {
            HudCard("> CALIBRAÇÃO HISTÓRICA GLOBAL (K_CALIB)") {
                Text(
                    text = if (estGlobal > 0) {
                        "Esforço Estimado: ${estGlobal}m vs Real: ${actGlobal}m ➔ K_calib = ${String.format("%.2f", kGlobal)}x"
                    } else {
                        "Sem histórico medido ainda (K_calib padrão = 1.00x). Conclua ou foque em tarefas para calibrar."
                    },
                    color = CyberCores.NeonCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        if (projetos.isEmpty()) {
            item {
                EstadoVazioAcionavel(
                    texto = "> Nenhum projeto ativo para projetar faixas de entrega.",
                    rotuloBotao = "+ Criar Primeiro Projeto",
                    onClick = { onNavigate(2, "projetos") }
                )
            }
        } else {
            items(projetos) { p ->
                val f = viewModel.calcularProjecaoProjeto(p, tarefas, dependencias, disponibilidade)
                Surface(
                    color = CyberCores.BgCard,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, if (f.emRiscoPrazo) CyberCores.NeonMagenta else CyberCores.NeonCyan.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(p.nome, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)

                        when (f.statusProjecao) {
                            "concluido" -> BadgeTag("✓ Todas as tarefas concluídas", CyberCores.NeonGreen)
                            "nivel0" -> {
                                BadgeTag("Incerteza Estrutural: ${f.taxaIncompletudePct}% (${f.tarefasSemEstimativa}/${f.totalPendentes} s/ est.)", CyberCores.CyberYellow)
                                Text(f.motivoNivel0, color = CyberCores.TextDim, fontSize = 12.sp)
                            }
                            else -> {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    BadgeTag("Carga: ${f.horasRestantes}h", CyberCores.NeonCyan)
                                    BadgeTag("⚡ Caminho Crítico: ${f.horasCaminhoCritico}h (${f.nosCaminhoCritico} nós)", CyberCores.NeonCyan)
                                }
                                BadgeTag(
                                    if (f.tarefasSemEstimativa > 0) "Projeção Parcial: ${f.tarefasSemEstimativa}/${f.totalPendentes} s/ est. (${f.taxaIncompletudePct}%)"
                                    else "Cobertura de estimativa: 100%",
                                    if (f.tarefasSemEstimativa > 0) CyberCores.CyberYellow else CyberCores.NeonGreen
                                )
                                Surface(color = CyberCores.BgBase, shape = RoundedCornerShape(4.dp), modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Cenários Estimados:", color = CyberCores.TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text("• Melhor Caso (100% cap.): ${f.dataMelhorCaso}", color = CyberCores.NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text("• Cenário Esperado (20% buffer): ${f.dataEsperada}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text("• Ritmo Histórico (${f.fatorCalibracao}x): ${f.dataHistorica}", color = CyberCores.CyberYellow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EcranRevisaoSemanalBackup(
    tarefas: List<Tarefa>,
    retrospectivas: List<Retrospectiva>,
    mapaBloqueios: Map<String, InfoBloqueioTarefa>,
    viewModel: MainViewModel,
    onOpenEditar: (Tarefa) -> Unit,
    onOpenConcluir: (Tarefa) -> Unit
) {
    val context = LocalContext.current
    var substituirAoImportar by remember { mutableStateOf(true) }
    var confirmarSimulador by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { viewModel.realizarBackupLocal(context, it) }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.restaurarBackupLocal(context, it, substituirAoImportar) }
    }

    val hoje = viewModel.hojeIso()
    val atrasadas = tarefas.filter { it.estado != "Concluída" && !it.prazoData.isNullOrBlank() && it.prazoData < hoje }

    val limite30DiasMs = System.currentTimeMillis() - (30L * 24L * 3600L * 1000L)
    val fantasmas = tarefas.filter { it.estado != "Concluída" && it.alteradoEm < limite30DiasMs }

    val tarefasArq by viewModel.tarefasArquivadas.collectAsState()
    val projetosArq by viewModel.projetosArquivados.collectAsState()
    val areasArq by viewModel.areasArquivadas.collectAsState()

    var avancou by remember { mutableStateOf("") }
    var bloqueou by remember { mutableStateOf("") }
    var mudar by remember { mutableStateOf("") }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
        // Gestão Rápida de Atrasadas (+7 dias)
        item {
            HudCard("> GESTÃO RÁPIDA DE PENDÊNCIAS EM ATRASO (${atrasadas.size})") {
                if (atrasadas.isEmpty()) {
                    Text("✓ Nenhuma tarefa atrasada no momento.", color = CyberCores.NeonGreen, fontSize = 12.sp)
                } else {
                    atrasadas.forEach { t ->
                        Surface(color = CyberCores.BgBase, shape = RoundedCornerShape(4.dp), border = BorderStroke(1.dp, CyberCores.NeonMagenta)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(t.titulo, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Prazo vencido: ${t.prazoData}", color = CyberCores.NeonMagenta, fontSize = 11.sp)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    OutlinedButton(
                                        onClick = { viewModel.adiarTarefaSeteDias(t) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) { Text("+7d", color = CyberCores.NeonCyan, fontSize = 11.sp) }
                                    Button(
                                        onClick = { onOpenConcluir(t) },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyberCores.NeonGreen),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) { Text("✓", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Higiene de Backlog (Tarefas Fantasmas > 30 dias)
        item {
            HudCard("> HIGIENE DE BACKLOG // TAREFAS FANTASMAS (${fantasmas.size})") {
                if (fantasmas.isEmpty()) {
                    Text("✓ Nenhuma tarefa estagnada há mais de 30 dias.", color = CyberCores.NeonGreen, fontSize = 12.sp)
                } else {
                    fantasmas.forEach { fant ->
                        Surface(color = CyberCores.BgBase, shape = RoundedCornerShape(4.dp), border = BorderStroke(1.dp, CyberCores.CyberYellow)) {
                            Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("👻 ${fant.titulo}", color = Color.White, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(onClick = { viewModel.manterTarefaFantasma(fant) }) { Text("Manter", color = CyberCores.NeonCyan, fontSize = 11.sp) }
                                    OutlinedButton(onClick = { onOpenEditar(fant) }) { Text("Renegociar Escopo", color = CyberCores.CyberYellow, fontSize = 11.sp) }
                                    OutlinedButton(onClick = { viewModel.arquivarTarefa(fant) }) { Text("Arquivar", color = CyberCores.NeonMagenta, fontSize = 11.sp) }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Itens Arquivados (Áreas, Projetos e Tarefas com opção de Desarquivar)
        if (areasArq.isNotEmpty() || tarefasArq.isNotEmpty() || projetosArq.isNotEmpty()) {
            item {
                HudCard("> ITENS ARQUIVADOS (${areasArq.size} áreas, ${projetosArq.size} projetos, ${tarefasArq.size} tarefas)") {
                    areasArq.forEach { a ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("🎯 [Área] ${a.nome}", color = CyberCores.TextDim, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            TextButton(onClick = { viewModel.desarquivarArea(a) }) {
                                Text("Desarquivar", color = CyberCores.NeonCyan, fontSize = 11.sp)
                            }
                        }
                    }
                    projetosArq.forEach { p ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("❐ [Projeto] ${p.nome}", color = CyberCores.TextDim, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            TextButton(onClick = { viewModel.desarquivarProjeto(p) }) {
                                Text("Desarquivar", color = CyberCores.NeonCyan, fontSize = 11.sp)
                            }
                        }
                    }
                    tarefasArq.forEach { t ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("• ${t.titulo}", color = CyberCores.TextDim, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            TextButton(onClick = { viewModel.desarquivarTarefa(t) }) {
                                Text("Desarquivar", color = CyberCores.NeonCyan, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Formulário dos 3 Prompts Reflexivos
        item {
            HudCard("> RETROSPECTIVA SEMANAL") {
                OutlinedTextField(value = avancou, onValueChange = { avancou = it }, label = { Text("O que avançou?", color = CyberCores.TextDim) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                OutlinedTextField(value = bloqueou, onValueChange = { bloqueou = it }, label = { Text("O que bloqueou?", color = CyberCores.TextDim) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                OutlinedTextField(value = mudar, onValueChange = { mudar = it }, label = { Text("O que precisa mudar?", color = CyberCores.TextDim) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                Button(
                    onClick = { viewModel.criarRetrospectiva(avancou, bloqueou, mudar); avancou = ""; bloqueou = ""; mudar = "" },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCores.NeonCyan)
                ) { Text("GRAVAR REFLEXÃO SEMANAL", color = Color.Black, fontWeight = FontWeight.Bold) }
            }
        }

        // Backup Portátil JSON (Exportar + Importar)
        item {
            HudCard("> PORTABILIDADE DE DADOS (BACKUP & RESTAURAÇÃO JSON)") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { launcher.launch("se-eduque-backup-${viewModel.hojeIso()}.json") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCores.NeonMagenta)
                    ) { Text("⬇ EXPORTAR JSON", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) }

                    OutlinedButton(
                        onClick = {
                            substituirAoImportar = true
                            importLauncher.launch(arrayOf("application/json", "*/*"))
                        },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, CyberCores.NeonCyan)
                    ) { Text("⬆ RESTAURAR (SUBSTITUIR)", color = CyberCores.NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                }
                OutlinedButton(
                    onClick = {
                        substituirAoImportar = false
                        importLauncher.launch(arrayOf("application/json", "*/*"))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                ) { Text("⬆ IMPORTAR (MESCLAR COM DADOS ATUAIS)", color = CyberCores.TextPrimary, fontSize = 11.sp) }
            }
        }

        // Simulador E2E protegido por confirmação
        item {
            HudCard("> DIAGNÓSTICO E2E // SIMULADOR DE MASSA DE TESTE NÍVEL 2") {
                Text(
                    "Gera ou remove física e exclusivamente os registos de demonstração (IDs de teste).",
                    color = CyberCores.TextDim,
                    fontSize = 11.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { confirmarSimulador = true },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, CyberCores.NeonGreen)
                    ) { Text("🧪 Gerar Teste", color = CyberCores.NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold) }

                    OutlinedButton(
                        onClick = { viewModel.limparMassaDeTeste() },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, CyberCores.NeonMagenta)
                    ) { Text("🧹 Limpar Teste", color = CyberCores.NeonMagenta, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }

        // Histórico Consolidado de Retrospectivas
        items(retrospectivas) { r ->
            val dataStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(r.dataMs))
            Surface(color = CyberCores.BgCard, shape = RoundedCornerShape(6.dp), border = BorderStroke(1.dp, CyberCores.NeonPurple.copy(alpha = 0.4f))) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("🗂 Retrospectiva — $dataStr", color = CyberCores.CyberYellow, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("• O que avançou: ${r.oQueAvancou}", color = Color.White, fontSize = 12.sp)
                    Text("• O que bloqueou: ${r.oQueBloqueou}", color = CyberCores.TextDim, fontSize = 12.sp)
                    Text("• O que mudar: ${r.oQueMudar}", color = CyberCores.NeonCyan, fontSize = 12.sp)
                }
            }
        }
    }

    if (confirmarSimulador) {
        AlertDialog(
            onDismissRequest = { confirmarSimulador = false },
            containerColor = CyberCores.BgSurface,
            title = { Text("Gerar Massa de Teste?", color = CyberCores.NeonCyan, fontWeight = FontWeight.Bold) },
            text = { Text("Isto adicionará o projeto '[TESTE] Compilador C' e as suas tarefas de demonstração. Poderá apagá-los a qualquer momento em 'Limpar Teste'.", color = Color.White, fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = { confirmarSimulador = false; viewModel.injetarMassaDeTeste() },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCores.NeonGreen)
                ) { Text("Confirmar", color = Color.Black, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmarSimulador = false }) { Text("Cancelar", color = CyberCores.TextDim) }
            }
        )
    }
}

// =============================================================================
// COMPONENTES REUTILIZÁVEIS HUD E MODAIS DE CALIBRAÇÃO / EDIÇÃO
// =============================================================================

@Composable
fun CartaoTarefaNivel2(
    tarefa: Tarefa,
    projetoNome: String?,
    infoBloqueio: InfoBloqueioTarefa,
    hojeIso: String,
    onFocar: () -> Unit,
    onEditar: () -> Unit,
    onConcluirOuReabrir: () -> Unit,
    onArquivar: () -> Unit
) {
    val atrasada = !tarefa.prazoData.isNullOrBlank() && tarefa.prazoData < hojeIso && tarefa.estado != "Concluída"
    val corBorda = when {
        tarefa.estado == "Concluída" -> CyberCores.NeonGreen
        atrasada || infoBloqueio.ehGargalo -> CyberCores.NeonMagenta
        infoBloqueio.bloqueada -> CyberCores.CyberYellow
        infoBloqueio.pertenceCaminhoCritico -> CyberCores.NeonCyan
        else -> CyberCores.NeonCyan.copy(alpha = 0.3f)
    }

    Surface(
        color = CyberCores.BgCard,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, corBorda),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEditar() }
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(tarefa.titulo, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                item { BadgeTag(if (infoBloqueio.bloqueada && tarefa.estado != "Concluída") "Bloqueada" else tarefa.estado, CyberCores.NeonCyan) }
                item {
                    BadgeTag(
                        tarefa.prioridade,
                        if (tarefa.prioridade == "Alta" || tarefa.prioridade == "Crítica") CyberCores.NeonMagenta else CyberCores.TextDim
                    )
                }
                item {
                    if (tarefa.estimativaMinutos > 0) {
                        BadgeTag("${tarefa.estimativaMinutos}m est. / ${tarefa.minutosReais}m real", CyberCores.NeonGreen)
                    } else {
                        BadgeTag("Sem estimativa", CyberCores.CyberYellow)
                    }
                }
                if (projetoNome != null) {
                    item { BadgeTag("Proj: $projetoNome", CyberCores.NeonCyan) }
                }
            }

            if (!tarefa.prazoData.isNullOrBlank() || infoBloqueio.pertenceCaminhoCritico || infoBloqueio.ehGargalo || infoBloqueio.bloqueada) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (!tarefa.prazoData.isNullOrBlank()) {
                            BadgeTag(
                                if (atrasada) "Prazo: ${tarefa.prazoData} (ATRASADA)" else "Prazo: ${tarefa.prazoData}",
                                if (atrasada) CyberCores.NeonMagenta else CyberCores.TextDim
                            )
                        }
                        if (infoBloqueio.pertenceCaminhoCritico) BadgeTag("⚡ Caminho Crítico", CyberCores.NeonCyan)
                        if (infoBloqueio.ehGargalo) BadgeTag("🔥 Gargalo (${infoBloqueio.qtdSucessorasTravadas} sucessoras)", CyberCores.NeonMagenta)
                    }
                    if (infoBloqueio.bloqueada) {
                        BadgeTag("⚠ Aguarda: ${infoBloqueio.titulosBloqueadores.joinToString()}", CyberCores.CyberYellow)
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (tarefa.estado != "Concluída" && !infoBloqueio.bloqueada) {
                    OutlinedButton(
                        onClick = onFocar,
                        border = BorderStroke(1.dp, CyberCores.NeonGreen),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) { Text("▶ Focar", color = CyberCores.NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                }
                OutlinedButton(
                    onClick = onEditar,
                    border = BorderStroke(1.dp, CyberCores.NeonCyan),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) { Text("✎ Editar", color = CyberCores.NeonCyan, fontSize = 11.sp) }
                Button(
                    onClick = onConcluirOuReabrir,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCores.NeonCyan),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) { Text(if (tarefa.estado == "Concluída") "Reabrir" else "Concluir", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    onClick = onArquivar,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) { Text("Arquivar", color = CyberCores.NeonMagenta, fontSize = 11.sp) }
            }
        }
    }
}

@Composable
fun ModalConcluirCalibrar(
    tarefa: Tarefa,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val base = if (tarefa.minutosReais > 0) tarefa.minutosReais else if (tarefa.estimativaMinutos > 0) tarefa.estimativaMinutos else 30
    var minutosInput by remember { mutableStateOf(base.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberCores.BgSurface,
        title = { Text("> CONCLUIR & CALIBRAR K_CALIB", color = CyberCores.NeonCyan, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(tarefa.titulo, color = Color.White, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = minutosInput,
                    onValueChange = { minutosInput = it.filter { c -> c.isDigit() } },
                    label = { Text("Tempo Real Executado (minutos)", color = CyberCores.TextDim) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (tarefa.estimativaMinutos > 0) {
                        OutlinedButton(onClick = { minutosInput = tarefa.estimativaMinutos.toString() }, contentPadding = PaddingValues(6.dp)) {
                            Text("Est (${tarefa.estimativaMinutos}m)", color = CyberCores.NeonCyan, fontSize = 11.sp)
                        }
                    }
                    OutlinedButton(onClick = { minutosInput = max(5, (minutosInput.toIntOrNull() ?: 30) - 15).toString() }, contentPadding = PaddingValues(6.dp)) {
                        Text("-15m", color = CyberCores.NeonCyan, fontSize = 11.sp)
                    }
                    OutlinedButton(onClick = { minutosInput = ((minutosInput.toIntOrNull() ?: 30) + 15).toString() }, contentPadding = PaddingValues(6.dp)) {
                        Text("+15m", color = CyberCores.NeonCyan, fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(minutosInput.toIntOrNull() ?: base) },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCores.NeonGreen)
            ) { Text("CONCLUIR", color = Color.Black, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = CyberCores.TextDim) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModalEditarTarefa(
    tarefa: Tarefa,
    projetos: List<Projeto>,
    onDismiss: () -> Unit,
    onConfirm: (String, String?, String, Int, Int, String?, String) -> Unit
) {
    val context = LocalContext.current
    var titulo by remember { mutableStateOf(tarefa.titulo) }
    var projId by remember { mutableStateOf(tarefa.projetoId) }
    var prio by remember { mutableStateOf(tarefa.prioridade) }
    var est by remember { mutableStateOf(tarefa.estimativaMinutos.toString()) }
    var real by remember { mutableStateOf(tarefa.minutosReais.toString()) }
    var prazo by remember { mutableStateOf(tarefa.prazoData ?: "") }
    var estado by remember { mutableStateOf(tarefa.estado) }

    fun dataOffsetIso(dias: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, dias)
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberCores.BgSurface,
        title = { Text("> EDITAR TAREFA", color = CyberCores.NeonCyan, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Título", color = CyberCores.TextDim) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                if (projetos.isNotEmpty()) {
                    Text("Projeto:", color = CyberCores.TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = projId == null,
                                onClick = { projId = null },
                                label = { Text("Sem Projeto", fontSize = 11.sp) }
                            )
                        }
                        items(projetos) { p ->
                            FilterChip(
                                selected = projId == p.id,
                                onClick = { projId = p.id },
                                label = { Text(p.nome, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                Text("Estado:", color = CyberCores.TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val estados = listOf("Não iniciada", "Em andamento", "Aguardando/Bloqueada", "Pausada", "Concluída")
                    items(estados) { st ->
                        FilterChip(
                            selected = estado == st,
                            onClick = { estado = st },
                            label = { Text(st, fontSize = 11.sp) }
                        )
                    }
                }

                Text("Prioridade:", color = CyberCores.TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val prioridades = listOf("Baixa", "Média", "Alta", "Crítica")
                    items(prioridades) { pr ->
                        FilterChip(
                            selected = prio == pr,
                            onClick = { prio = pr },
                            label = { Text(pr, fontSize = 11.sp) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = est,
                        onValueChange = { est = it.filter { c -> c.isDigit() } },
                        label = { Text("Est. (min)", color = CyberCores.TextDim) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = real,
                        onValueChange = { real = it.filter { c -> c.isDigit() } },
                        label = { Text("Real (min)", color = CyberCores.TextDim) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }

                OutlinedButton(
                    onClick = { abrirSeletorDataIso(context, prazo) { prazo = it } },
                    border = BorderStroke(1.dp, CyberCores.NeonCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (prazo.isBlank()) "📅 Escolher Prazo no Calendário" else "📅 Prazo: $prazo",
                        color = CyberCores.NeonCyan,
                        fontSize = 12.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { prazo = dataOffsetIso(0) }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                        Text("Hoje", color = CyberCores.NeonCyan, fontSize = 11.sp)
                    }
                    OutlinedButton(onClick = { prazo = dataOffsetIso(1) }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                        Text("Amanhã", color = CyberCores.NeonCyan, fontSize = 11.sp)
                    }
                    OutlinedButton(onClick = { prazo = dataOffsetIso(7) }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                        Text("+7 Dias", color = CyberCores.NeonCyan, fontSize = 11.sp)
                    }
                    TextButton(onClick = { prazo = "" }) {
                        Text("Limpar", color = CyberCores.TextDim, fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(titulo, projId, prio, est.toIntOrNull() ?: 0, real.toIntOrNull() ?: 0, prazo, estado) },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCores.NeonCyan)
            ) { Text("SALVAR", color = Color.Black, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = CyberCores.TextDim) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModalEditarProjeto(
    projeto: Projeto,
    areas: List<Area>,
    onDismiss: () -> Unit,
    onConfirm: (String, String?, String, String?, String, String, String, Int, Int) -> Unit
) {
    val context = LocalContext.current
    var nome by remember { mutableStateOf(projeto.nome) }
    var areaId by remember { mutableStateOf(projeto.areaId) }
    var obj by remember { mutableStateOf(projeto.objetivo ?: "") }
    var dataAlvo by remember { mutableStateOf(projeto.dataAlvo ?: "") }
    var estado by remember { mutableStateOf(projeto.estado) }
    var tipoProj by remember { mutableStateOf(projeto.tipoProjeto) }
    var unid by remember { mutableStateOf(projeto.unidadeMedida) }
    var vAtual by remember { mutableStateOf(projeto.valorAtual.toString()) }
    var vTotal by remember { mutableStateOf(projeto.valorTotal.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberCores.BgSurface,
        title = { Text("> EDITAR PROJETO", color = CyberCores.NeonCyan, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome do Projeto", color = CyberCores.TextDim) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                OutlinedTextField(
                    value = obj,
                    onValueChange = { obj = it },
                    label = { Text("Objetivo / Entrega", color = CyberCores.TextDim) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                OutlinedButton(
                    onClick = { abrirSeletorDataIso(context, dataAlvo) { dataAlvo = it } },
                    border = BorderStroke(1.dp, CyberCores.NeonCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (dataAlvo.isBlank()) "📅 Escolher Data-Alvo" else "📅 Data-Alvo: $dataAlvo",
                        color = CyberCores.NeonCyan,
                        fontSize = 12.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = vAtual,
                        onValueChange = { vAtual = it.filter { c -> c.isDigit() } },
                        label = { Text("Atual", color = CyberCores.TextDim) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = vTotal,
                        onValueChange = { vTotal = it.filter { c -> c.isDigit() } },
                        label = { Text("Total (0=Tarefas)", color = CyberCores.TextDim) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = unid,
                        onValueChange = { unid = it },
                        label = { Text("Unid.", color = CyberCores.TextDim) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }

                Text("Área Pai:", color = CyberCores.TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(areas) { a ->
                        FilterChip(
                            selected = areaId == a.id,
                            onClick = { areaId = a.id },
                            label = { Text(a.nome, fontSize = 11.sp) }
                        )
                    }
                }

                Text("Estado do Projeto:", color = CyberCores.TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("Ativo", "Pausado", "Concluído")) { st ->
                        FilterChip(
                            selected = estado == st,
                            onClick = { estado = st },
                            label = { Text(st, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        nome,
                        areaId,
                        obj,
                        dataAlvo,
                        estado,
                        tipoProj,
                        unid.ifBlank { "itens" },
                        vAtual.toIntOrNull() ?: 0,
                        vTotal.toIntOrNull() ?: 0
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCores.NeonCyan)
            ) { Text("SALVAR", color = Color.Black, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = CyberCores.TextDim) }
        }
    )
}

@Composable
fun HudCard(titulo: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = CyberCores.BgCard,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, CyberCores.NeonCyan.copy(alpha = 0.22f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(titulo, color = CyberCores.NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
            content()
        }
    }
}

@Composable
fun StatBox(label: String, valor: String, cor: Color, modifier: Modifier = Modifier) {
    Surface(
        color = CyberCores.BgCard,
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, cor.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(label, color = CyberCores.TextDim, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(valor, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun BadgeTag(texto: String, cor: Color) {
    Surface(
        color = cor.copy(alpha = 0.12f),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, cor.copy(alpha = 0.5f))
    ) {
        Text(texto, color = cor, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}

@Composable
fun EstadoVazioAcionavel(texto: String, rotuloBotao: String, onClick: () -> Unit) {
    Surface(
        color = CyberCores.BgCard,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(texto, color = CyberCores.TextDim, fontSize = 13.sp, textAlign = TextAlign.Center)
            OutlinedButton(onClick = onClick, border = BorderStroke(1.dp, CyberCores.NeonCyan)) {
                Text(rotuloBotao, color = CyberCores.NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}