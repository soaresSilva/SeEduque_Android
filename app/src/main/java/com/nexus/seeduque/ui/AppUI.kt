package com.nexus.seeduque.ui

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: MainViewModel) {
    var grupoPai by remember { mutableIntStateOf(0) }
    var abaAtual by remember { mutableStateOf("hoje") }

    val areas by viewModel.areas.collectAsState()
    val areaFiltroId by viewModel.areaFiltroId.collectAsState()
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

    // Estados de Modais HUD
    var tarefaParaConcluir by remember { mutableStateOf<Tarefa?>(null) }
    var tarefaParaEditar by remember { mutableStateOf<Tarefa?>(null) }
    var projetoParaEditar by remember { mutableStateOf<Projeto?>(null) }

    val mapaBloqueios = remember(tarefas, dependencias, projetos) {
        viewModel.calcularMapaBloqueios(tarefas, dependencias, projetos)
    }

    LaunchedEffect(toastMsg) {
        if (toastMsg != null) {
            delay(3200)
            viewModel.limparToast()
        }
    }

    Scaffold(
        containerColor = CyberCores.BgBase,
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
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            // 1. CABEÇALHO HUD
            Surface(
                color = CyberCores.BgSurface,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, CyberCores.NeonCyan.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("SYS.EXEC // NÚCLEO NÍVEL 2", color = CyberCores.NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                        Text("SE EDUQUE", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    }
                    Surface(
                        color = CyberCores.BgBase,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, CyberCores.NeonGreen.copy(alpha = 0.5f))
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(7.dp).background(CyberCores.NeonGreen, CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ONLINE (0ms)", color = CyberCores.TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 2. BARRA GLOBAL DE FOCO POR ÁREA (UNIVERSIDADE, INGLÊS, LIVROS, ETC.)
            Surface(
                color = CyberCores.BgSurface,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, CyberCores.NeonCyan.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                LazyRow(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        Text("🎯 ÁREA:", color = CyberCores.TextDim, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    item {
                        val ativoGlobal = areaFiltroId == null
                        FilterChip(
                            selected = ativoGlobal,
                            onClick = { viewModel.selecionarFiltroArea(null) },
                            label = { Text("🌐 Todas", fontSize = 12.sp) },
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
                            onClick = { viewModel.selecionarFiltroArea(area.id) },
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
            }

            // 3. BARRA DE CRONÓMETRO DE FOCO ATIVO
            if (tarefaEmFoco != null) {
                val mins = String.format("%02d", segundosFoco / 60)
                val segs = String.format("%02d", segundosFoco % 60)
                Surface(
                    color = CyberCores.NeonGreen.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, CyberCores.NeonGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("▶ EM FOCO // K_CALIB", color = CyberCores.NeonGreen, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
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

            // 4. NAVEGAÇÃO HIERÁRQUICA DE 2 NÍVEIS (PAI -> FILHO)
            NavegacaoHierarquica2Niveis(
                grupoPai = grupoPai,
                abaAtual = abaAtual,
                onSelectGrupo = { novoGrupo, abaDefault ->
                    grupoPai = novoGrupo
                    abaAtual = abaDefault
                },
                onSelectAba = { abaAtual = it }
            )

            // 5. CONTEÚDO DA ABA SELECIONADA
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (abaAtual) {
                    "hoje" -> EcranHojeDashboard(
                        tarefas = tarefas,
                        projetos = projetos,
                        revisoes = revisoes,
                        mapaBloqueios = mapaBloqueios,
                        viewModel = viewModel,
                        onNavigate = { grupo, aba -> grupoPai = grupo; abaAtual = aba },
                        onOpenConcluir = { tarefaParaConcluir = it },
                        onOpenEditar = { tarefaParaEditar = it }
                    )
                    "prontas" -> EcranProntasParaAgir(
                        tarefas = tarefas,
                        projetos = projetos,
                        mapaBloqueios = mapaBloqueios,
                        viewModel = viewModel,
                        onNavigate = { grupo, aba -> grupoPai = grupo; abaAtual = aba },
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
                        onNavigate = { grupo, aba -> grupoPai = grupo; abaAtual = aba }
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

        // MODAL HUD DE EDIÇÃO RÁPIDA DE TAREFA (INLINE EDIT)
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
                onConfirm = { nome, areaId, obj, dataAlvo, estado ->
                    viewModel.editarProjeto(projetoParaEditar!!, nome, areaId, obj, dataAlvo, estado)
                    projetoParaEditar = null
                }
            )
        }
    }
}

@Composable
fun NavegacaoHierarquica2Niveis(
    grupoPai: Int,
    abaAtual: String,
    onSelectGrupo: (Int, String) -> Unit,
    onSelectAba: (String) -> Unit
) {
    Surface(
        color = CyberCores.BgSurface,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, CyberCores.NeonCyan.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val grupos = listOf(
                    Triple(0, "⚡ Execução", "hoje"),
                    Triple(1, "🏛 Estrutura", "areas"),
                    Triple(2, "⏱ Fluxo", "agenda"),
                    Triple(3, "📊 Nível 2", "previsoes")
                )
                grupos.forEach { (idx, rotulo, defaultTab) ->
                    val ativo = grupoPai == idx
                    Surface(
                        color = if (ativo) CyberCores.NeonCyan.copy(alpha = 0.16f) else CyberCores.BgBase,
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, if (ativo) CyberCores.NeonCyan else Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier.weight(1f).clickable { onSelectGrupo(idx, defaultTab) }
                    ) {
                        Text(
                            text = rotulo,
                            color = if (ativo) CyberCores.NeonCyan else CyberCores.TextDim,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                        )
                    }
                }
            }

            val subAbas = when (grupoPai) {
                0 -> listOf("hoje" to "Hoje", "prontas" to "Prontas p/ Agir", "tarefas" to "Tarefas", "kanban" to "Kanban")
                1 -> listOf("areas" to "1. Áreas", "projetos" to "2. Projetos", "objetivos" to "3. Objetivos", "marcos" to "4. Marcos")
                2 -> listOf("agenda" to "Agenda & Cap.", "dependencias" to "Dependências", "revisoes" to "Revisões")
                else -> listOf("previsoes" to "Previsões K_calib", "semanal" to "Rev. Semanal & Backup")
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(subAbas) { (chave, titulo) ->
                    val selecionada = abaAtual == chave
                    Surface(
                        color = if (selecionada) CyberCores.NeonCyan else Color.Transparent,
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, if (selecionada) CyberCores.NeonCyan else Color.White.copy(alpha = 0.12f)),
                        modifier = Modifier.clickable { onSelectAba(chave) }
                    ) {
                        Text(
                            text = titulo,
                            color = if (selecionada) CyberCores.BgBase else CyberCores.TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// 1. ECRÃS DE EXECUÇÃO DIÁRIA (HOJE, PRONTAS P/ AGIR, TAREFAS, KANBAN)
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
    val ativas = tarefas.filter { it.estado != "Concluída" }
    val prazoHoje = ativas.filter { it.prazoData == hoje }
    val atrasadas = ativas.filter { !it.prazoData.isNullOrBlank() && it.prazoData < hoje }
    val bloqueadas = ativas.filter { mapaBloqueios[it.id]?.bloqueada == true }
    val emAndamento = ativas.filter { it.estado == "Em andamento" }
    val revPendentes = revisoes.count { it.estado != "Concluída" }

    var quickTitulo by remember { mutableStateOf("") }
    var quickMinutos by remember { mutableStateOf("") }
    var quickProjId by remember { mutableStateOf<String?>(null) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
        item {
            HudCard("> CAPTURA RÁPIDA DE TAREFA (QUICK ADD)") {
                OutlinedTextField(
                    value = quickTitulo,
                    onValueChange = { quickTitulo = it },
                    label = { Text("O que precisa de ser feito?", color = CyberCores.TextDim) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

                if (projetos.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        item {
                            Text("Projeto:", color = CyberCores.TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        item {
                            FilterChip(
                                selected = quickProjId == null,
                                onClick = { quickProjId = null },
                                label = { Text("Auto / Padrão", fontSize = 11.sp) }
                            )
                        }
                        items(projetos) { p ->
                            val selecionado = quickProjId == p.id
                            FilterChip(
                                selected = selecionado,
                                onClick = { quickProjId = if (selecionado) null else p.id },
                                label = { Text(p.nome, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = quickMinutos,
                        onValueChange = { quickMinutos = it.filter { c -> c.isDigit() } },
                        label = { Text("Min. (ex: 45)", color = CyberCores.TextDim) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    Button(
                        onClick = {
                            viewModel.criarTarefaCompleta(
                                titulo = quickTitulo,
                                projetoId = quickProjId ?: projetos.firstOrNull()?.id,
                                estimativaMinutos = quickMinutos.toIntOrNull() ?: 30,
                                prazoData = hoje
                            )
                            quickTitulo = ""
                            quickMinutos = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCores.NeonCyan)
                    ) {
                        Text("+ CAPTURAR", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatBox("HOJE", prazoHoje.size.toString(), CyberCores.NeonCyan, Modifier.weight(1f))
                StatBox("ATRASO", atrasadas.size.toString(), CyberCores.NeonMagenta, Modifier.weight(1f))
                StatBox("BLOQ.", bloqueadas.size.toString(), CyberCores.CyberYellow, Modifier.weight(1f))
                StatBox("ATIVAS", emAndamento.size.toString(), CyberCores.NeonGreen, Modifier.weight(1f))
                StatBox("REV.", revPendentes.toString(), CyberCores.NeonPurple, Modifier.weight(1f))
            }
        }

        val urgentes = (atrasadas + prazoHoje + bloqueadas + emAndamento).distinctBy { it.id }
        if (urgentes.isEmpty()) {
            item {
                EstadoVazioAcionavel(
                    texto = "> Nenhuma pendência urgente ou bloqueada para hoje.",
                    rotuloBotao = "⚡ Ir para Fila Prontas p/ Agir",
                    onClick = { onNavigate(0, "prontas") }
                )
            }
        } else {
            items(urgentes) { t ->
                CartaoTarefaNivel2(
                    tarefa = t,
                    projetoNome = projetos.find { it.id == t.projetoId }?.nome,
                    infoBloqueio = mapaBloqueios[t.id] ?: InfoBloqueioTarefa(),
                    hojeIso = hoje,
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
                    onClick = { onNavigate(0, "tarefas") }
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = estimativa,
                        onValueChange = { estimativa = it.filter { c -> c.isDigit() } },
                        label = { Text("Estimativa (min)", color = CyberCores.TextDim) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = prazo,
                        onValueChange = { prazo = it },
                        label = { Text("Prazo YYYY-MM-DD", color = CyberCores.TextDim) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
                if (projetos.isNotEmpty()) {
                    Text("Vincular ao Projeto:", color = CyberCores.TextDim, fontSize = 11.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                            projetoId = projetoIdSelecionado ?: projetos.firstOrNull()?.id,
                            prioridade = prioridade,
                            estimativaMinutos = estimativa.toIntOrNull() ?: 0,
                            prazoData = prazo
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

@Composable
fun EcranQuadroKanban(
    tarefas: List<Tarefa>,
    mapaBloqueios: Map<String, InfoBloqueioTarefa>,
    viewModel: MainViewModel,
    onOpenConcluir: (Tarefa) -> Unit,
    onOpenEditar: (Tarefa) -> Unit
) {
    val colunas = listOf("Não iniciada", "Em andamento", "Aguardando/Bloqueada", "Pausada", "Concluída")

    LazyRow(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(colunas.indices.toList()) { colIndex ->
            val colStatus = colunas[colIndex]
            val tarefasDaColuna = tarefas.filter { it.estado == colStatus }

            Surface(
                color = CyberCores.BgSurface,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.07f)),
                modifier = Modifier.width(280.dp).fillMaxHeight()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(colStatus.uppercase(), color = CyberCores.NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                        Text("(${tarefasDaColuna.size})", color = CyberCores.TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Divider(color = Color.White.copy(alpha = 0.08f))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(tarefasDaColuna) { t ->
                            val info = mapaBloqueios[t.id] ?: InfoBloqueioTarefa()
                            Surface(
                                color = CyberCores.BgCardAlt,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(
                                    1.dp,
                                    when {
                                        info.ehGargalo -> CyberCores.NeonMagenta
                                        info.bloqueada -> CyberCores.CyberYellow
                                        info.pertenceCaminhoCritico -> CyberCores.NeonCyan
                                        else -> Color.White.copy(alpha = 0.1f)
                                    }
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                                        Text(t.titulo, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                        Text(
                                            "✎ Editar",
                                            color = CyberCores.NeonCyan,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.clickable { onOpenEditar(t) }.padding(start = 8.dp)
                                        )
                                    }
                                    if (info.pertenceCaminhoCritico) Text("⚡ Caminho Crítico", color = CyberCores.NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    if (info.ehGargalo) Text("🔥 Gargalo (${info.qtdSucessorasTravadas} sucessoras)", color = CyberCores.NeonMagenta, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    if (info.bloqueada) Text("⚠ Aguarda: ${info.titulosBloqueadores.joinToString()}", color = CyberCores.CyberYellow, fontSize = 10.sp)

                                    Text(
                                        text = if (t.estimativaMinutos > 0) "${t.estimativaMinutos}m est. / ${t.minutosReais}m real" else "Incerteza: Sem estimativa",
                                        color = CyberCores.TextDim,
                                        fontSize = 11.sp
                                    )

                                    // Linha 1: Ações de Foco e Conclusão (Largura total sem espremer texto)
                                    if (t.estado != "Concluída") {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                            if (!info.bloqueada) {
                                                OutlinedButton(
                                                    onClick = { viewModel.iniciarFoco(t) },
                                                    border = BorderStroke(1.dp, CyberCores.NeonGreen),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) { Text("▶ Focar", color = CyberCores.NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1) }
                                            }
                                            Button(
                                                onClick = { onOpenConcluir(t) },
                                                colors = ButtonDefaults.buttonColors(containerColor = CyberCores.NeonCyan),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier.weight(1f)
                                            ) { Text("✓ Concluir", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1) }
                                        }
                                    }

                                    // Linha 2: Setas de movimentação entre as 5 colunas do Kanban
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                        if (colIndex > 0) {
                                            OutlinedButton(
                                                onClick = { viewModel.atualizarEstadoTarefa(t, colunas[colIndex - 1]) },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.weight(1f)
                                            ) { Text("◀ ${colunas[colIndex - 1].take(10)}", color = CyberCores.TextDim, fontSize = 10.sp, maxLines = 1) }
                                        }
                                        if (colIndex < colunas.lastIndex) {
                                            val proxColuna = colunas[colIndex + 1]
                                            OutlinedButton(
                                                onClick = {
                                                    if (proxColuna == "Concluída") onOpenConcluir(t)
                                                    else viewModel.atualizarEstadoTarefa(t, proxColuna)
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.weight(1f)
                                            ) { Text("${proxColuna.take(10)} ▶", color = CyberCores.NeonCyan, fontSize = 10.sp, maxLines = 1) }
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
}

// =============================================================================
// 2. ECRÃS DE ESTRUTURA HIERÁRQUICA (ÁREAS ➔ PROJETOS ➔ OBJETIVOS ➔ MARCOS)
// =============================================================================

@Composable
fun EcranAreas(areas: List<Area>, viewModel: MainViewModel) {
    var nome by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var corHex by remember { mutableStateOf("#00F0FF") }
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
                                .size(26.dp)
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
                    TextButton(onClick = { viewModel.arquivarArea(a) }) {
                        Text("Arquivar", color = CyberCores.NeonMagenta, fontSize = 12.sp)
                    }
                }
            }
        }
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
    var nome by remember { mutableStateOf("") }
    var objetivo by remember { mutableStateOf("") }
    var dataAlvo by remember { mutableStateOf("") }
    var areaEscolhida by remember { mutableStateOf<String?>(null) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
        item {
            HudCard("> 2. NOVO PROJETO (FILHO DE ÁREA)") {
                OutlinedTextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome do Projeto *", color = CyberCores.TextDim) }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                OutlinedTextField(value = objetivo, onValueChange = { objetivo = it }, label = { Text("Objetivo / Entrega Final", color = CyberCores.TextDim) }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                OutlinedTextField(value = dataAlvo, onValueChange = { dataAlvo = it }, label = { Text("Prazo Final (YYYY-MM-DD)", color = CyberCores.TextDim) }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(areas) { a ->
                        val sel = areaEscolhida == a.id
                        FilterChip(selected = sel, onClick = { areaEscolhida = a.id }, label = { Text(a.nome, fontSize = 11.sp) })
                    }
                }

                Button(
                    onClick = {
                        viewModel.criarProjeto(nome, areaEscolhida ?: areas.firstOrNull()?.id, objetivo, dataAlvo)
                        nome = ""; objetivo = ""; dataAlvo = ""
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
                    onClick = { viewModel.criarObjetivo(titulo, horizonte, projEscolhido ?: projetos.firstOrNull()?.id, null); titulo = "" },
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

@Composable
fun EcranMarcos(marcos: List<Marco>, projetos: List<Projeto>, viewModel: MainViewModel) {
    var titulo by remember { mutableStateOf("") }
    var dataAlvo by remember { mutableStateOf("") }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
        item {
            HudCard("> 4. NOVO MARCO INTERMEDIÁRIO / PROVA") {
                OutlinedTextField(value = titulo, onValueChange = { titulo = it }, label = { Text("Título do Marco *", color = CyberCores.TextDim) }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                OutlinedTextField(value = dataAlvo, onValueChange = { dataAlvo = it }, label = { Text("Data-Alvo (YYYY-MM-DD)", color = CyberCores.TextDim) }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                Button(
                    onClick = { viewModel.criarMarco(titulo, projetos.firstOrNull()?.id, dataAlvo); titulo = ""; dataAlvo = "" },
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
// 3. FLUXO & AGENDA (SESSÕES, EXCEÇÕES DE CAPACIDADE, DEPENDÊNCIAS, REVISÕES)
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
    var duracaoMin by remember { mutableStateOf("60") }
    var tarefaIdEscolhida by remember { mutableStateOf<String?>(null) }

    var excDe by remember { mutableStateOf("") }
    var excAte by remember { mutableStateOf("") }
    var excIni by remember { mutableStateOf("00:00") }
    var excFim by remember { mutableStateOf("00:00") }

    val diasNomes = listOf("Domingo", "Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado")

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
            HudCard("> AGENDAR SESSÃO DE FOCO (+ GOOGLE CALENDAR)") {
                OutlinedTextField(value = tituloSessao, onValueChange = { tituloSessao = it }, label = { Text("Título da Sessão *", color = CyberCores.TextDim) }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                OutlinedTextField(value = duracaoMin, onValueChange = { duracaoMin = it.filter { c -> c.isDigit() } }, label = { Text("Duração (minutos)", color = CyberCores.TextDim) }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))

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
                        viewModel.criarSessao(tituloSessao, duracaoMin.toIntOrNull() ?: 60, tarefaIdEscolhida)
                        tituloSessao = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCores.NeonCyan)
                ) { Text("AGENDAR SESSÃO", color = Color.Black, fontWeight = FontWeight.Bold) }
            }
        }

        items(sessoes) { s ->
            Surface(color = CyberCores.BgCard, shape = RoundedCornerShape(6.dp), border = BorderStroke(1.dp, CyberCores.NeonCyan.copy(alpha = 0.3f))) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("⏱ ${s.titulo}", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("${s.duracaoMinutos}m • ${s.estado}", color = CyberCores.TextDim, fontSize = 11.sp)
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
                            colors = ButtonDefaults.buttonColors(containerColor = if (s.sincronizadoCalendar) CyberCores.NeonGreen else CyberCores.NeonCyan),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(if (s.sincronizadoCalendar) "✓ Calendar" else "Calendar", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                    OutlinedTextField(value = excDe, onValueChange = { excDe = it }, label = { Text("De YYYY-MM-DD", color = CyberCores.TextDim) }, modifier = Modifier.weight(1f), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                    OutlinedTextField(value = excAte, onValueChange = { excAte = it }, label = { Text("Até YYYY-MM-DD", color = CyberCores.TextDim) }, modifier = Modifier.weight(1f), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = excIni, onValueChange = { excIni = it }, label = { Text("Início HH:MM", color = CyberCores.TextDim) }, modifier = Modifier.weight(1f), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                    OutlinedTextField(value = excFim, onValueChange = { excFim = it }, label = { Text("Fim HH:MM", color = CyberCores.TextDim) }, modifier = Modifier.weight(1f), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
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

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
        item {
            HudCard("> VINCULAR DEPENDÊNCIA DIRECIONADA (A ➔ B)") {
                Text("1. Selecione a Tarefa Bloqueadora (Predecessora A):", color = CyberCores.TextDim, fontSize = 11.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(tarefas) { t ->
                        FilterChip(selected = predId == t.id, onClick = { predId = t.id }, label = { Text(t.titulo, fontSize = 11.sp) })
                    }
                }
                Text("2. Selecione a Tarefa que Aguarda (Sucessora B):", color = CyberCores.TextDim, fontSize = 11.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(tarefas) { t ->
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
    var titulo by remember { mutableStateOf("") }
    var intervalo by remember { mutableStateOf("D+1") }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
        item {
            HudCard("> AGENDAR REVISÃO ESPAÇADA (D+1 ➔ D+3 ➔ D+7 ➔ D+15)") {
                OutlinedTextField(value = titulo, onValueChange = { titulo = it }, label = { Text("Tema / Aula / Capítulo *", color = CyberCores.TextDim) }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("D+1", "D+3", "D+7", "D+15").forEach { r ->
                        FilterChip(selected = intervalo == r, onClick = { intervalo = r }, label = { Text(r, fontSize = 11.sp) })
                    }
                }
                Button(
                    onClick = { viewModel.criarRevisao(titulo, intervalo); titulo = "" },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCores.NeonPurple)
                ) { Text("AGENDAR REVISÃO", color = Color.White, fontWeight = FontWeight.Bold) }
            }
        }
        items(revisoes) { rev ->
            val dataFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(rev.dataAgendada))
            Surface(color = CyberCores.BgCard, shape = RoundedCornerShape(6.dp), border = BorderStroke(1.dp, CyberCores.NeonPurple.copy(alpha = 0.5f))) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("📖 ${rev.titulo}", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Ciclo: ${rev.regraIntervalo} • Data: $dataFmt • ${rev.estado}", color = CyberCores.TextDim, fontSize = 12.sp)
                    }
                    OutlinedButton(onClick = { viewModel.concluirRevisao(rev) }) {
                        Text(if (rev.estado == "Concluída") "Reabrir" else "Concluir (+Próx)", color = CyberCores.NeonGreen, fontSize = 11.sp)
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
                    onClick = { onNavigate(1, "projetos") }
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
                                    "Taxa de Incompletude: ${f.tarefasSemEstimativa}/${f.totalPendentes} (${f.taxaIncompletudePct}%)",
                                    if (f.tarefasSemEstimativa > 0) CyberCores.CyberYellow else CyberCores.NeonGreen
                                )
                                Surface(color = CyberCores.BgBase, shape = RoundedCornerShape(4.dp), modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("• Melhor Caso (T_otimista — 100% cap.): ${f.dataMelhorCaso}", color = CyberCores.NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text("• Cenário Esperado (T_esperado — 20% buffer): ${f.dataEsperada}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text("• Variabilidade Histórica (T_pessimista — ${f.fatorCalibracao}x): ${f.dataHistorica}", color = CyberCores.CyberYellow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { viewModel.realizarBackupLocal(context, it) }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.restaurarBackupLocal(context, it) }
    }

    val hoje = viewModel.hojeIso()
    val atrasadas = tarefas.filter { it.estado != "Concluída" && !it.prazoData.isNullOrBlank() && it.prazoData < hoje }

    val limite30DiasMs = System.currentTimeMillis() - (30L * 24L * 3600L * 1000L)
    val fantasmas = tarefas.filter { it.estado != "Concluída" && it.alteradoEm < limite30DiasMs }

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
                        onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, CyberCores.NeonCyan)
                    ) { Text("⬆ IMPORTAR JSON", color = CyberCores.NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }

        // Simulador E2E
        item {
            HudCard("> DIAGNÓSTICO E2E // SIMULADOR DE MASSA DE TESTE NÍVEL 2") {
                Text(
                    "Gera um projeto de teste com Caminho Crítico (A➔B➔D), Gargalo (A), K_calib (1.50x) e Tarefa Fantasma (>45d).",
                    color = CyberCores.TextDim,
                    fontSize = 11.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { viewModel.injetarMassaDeTeste() },
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
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, corBorda),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(tarefa.titulo, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)

            // Linha 1 de Metadados (LazyRow evita que tags longas espremam umas às outras e criem buracos verticais)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                item { BadgeTag(tarefa.estado, CyberCores.NeonCyan) }
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
                        BadgeTag("Incerteza: S/ est.", CyberCores.CyberYellow)
                    }
                }
                if (projetoNome != null) {
                    item { BadgeTag("Proj: $projetoNome", CyberCores.NeonCyan) }
                }
            }

            // Linha 2: Alertas de Prazo, Caminho Crítico, Gargalo e Bloqueio
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

            // Linha de Botões de Ação
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

                OutlinedTextField(
                    value = prazo,
                    onValueChange = { prazo = it },
                    label = { Text("Prazo YYYY-MM-DD", color = CyberCores.TextDim) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

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
            ) { Text("GUARDAR", color = Color.Black, fontWeight = FontWeight.Bold) }
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
    onConfirm: (String, String?, String, String?, String) -> Unit
) {
    var nome by remember { mutableStateOf(projeto.nome) }
    var areaId by remember { mutableStateOf(projeto.areaId) }
    var obj by remember { mutableStateOf(projeto.objetivo ?: "") }
    var dataAlvo by remember { mutableStateOf(projeto.dataAlvo ?: "") }
    var estado by remember { mutableStateOf(projeto.estado) }

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
                OutlinedTextField(
                    value = dataAlvo,
                    onValueChange = { dataAlvo = it },
                    label = { Text("Data-Alvo YYYY-MM-DD", color = CyberCores.TextDim) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )

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
                onClick = { onConfirm(nome, areaId, obj, dataAlvo, estado) },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCores.NeonCyan)
            ) { Text("GUARDAR", color = Color.Black, fontWeight = FontWeight.Bold) }
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