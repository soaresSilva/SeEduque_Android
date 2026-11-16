package com.nexus.seeduque.repository

import com.nexus.seeduque.database.*
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class SeEduqueRepository(private val dao: SeEduqueDao) {
    fun observarAreas(): Flow<List<Area>> = dao.observarAreas()
    fun observarAreasArquivadas(): Flow<List<Area>> = dao.observarAreasArquivadas()
    fun observarProjetos(): Flow<List<Projeto>> = dao.observarProjetos()
    fun observarProjetosArquivados(): Flow<List<Projeto>> = dao.observarProjetosArquivados()
    fun observarTarefas(): Flow<List<Tarefa>> = dao.observarTarefas()
    fun observarTarefasArquivadas(): Flow<List<Tarefa>> = dao.observarTarefasArquivadas()
    fun observarObjetivos(): Flow<List<Objetivo>> = dao.observarObjetivos()
    fun observarMarcos(): Flow<List<Marco>> = dao.observarMarcos()
    fun observarDependencias(): Flow<List<Dependencia>> = dao.observarDependencias()
    fun observarDisponibilidade(): Flow<List<Disponibilidade>> = dao.observarDisponibilidade()
    fun observarSessoes(): Flow<List<Sessao>> = dao.observarSessoes()
    fun observarRevisoes(): Flow<List<Revisao>> = dao.observarRevisoes()
    fun observarRetrospectivas(): Flow<List<Retrospectiva>> = dao.observarRetrospectivas()

    suspend fun inserirArea(area: Area) = dao.inserirArea(area)
    suspend fun atualizarArea(area: Area) = dao.atualizarArea(area)

    suspend fun inserirProjeto(projeto: Projeto) = dao.inserirProjeto(projeto)
    suspend fun atualizarProjeto(projeto: Projeto) = dao.atualizarProjeto(projeto)
    suspend fun atualizarProgressoProjeto(projetoId: String, novoValor: Int) =
        dao.atualizarProgressoProjetoAtomico(projetoId, novoValor, System.currentTimeMillis())

    suspend fun inserirTarefa(tarefa: Tarefa) = dao.inserirTarefa(tarefa)
    suspend fun atualizarTarefa(tarefa: Tarefa) = dao.atualizarTarefa(tarefa)
    suspend fun somarMinutosTarefaAtomico(tarefaId: String, minutos: Int, estado: String) =
        dao.somarMinutosTarefaAtomico(tarefaId, minutos, estado, System.currentTimeMillis())

    suspend fun inserirObjetivo(objetivo: Objetivo) = dao.inserirObjetivo(objetivo)
    suspend fun atualizarObjetivo(objetivo: Objetivo) = dao.atualizarObjetivo(objetivo)

    suspend fun inserirMarco(marco: Marco) = dao.inserirMarco(marco)
    suspend fun atualizarMarco(marco: Marco) = dao.atualizarMarco(marco)

    suspend fun inserirDependencia(dep: Dependencia) = dao.inserirDependencia(dep)
    suspend fun removerDependencia(depId: String) = dao.removerDependencia(depId)

    suspend fun inserirDisponibilidade(disp: Disponibilidade) = dao.inserirDisponibilidade(disp)
    suspend fun atualizarDisponibilidade(disp: Disponibilidade) = dao.atualizarDisponibilidade(disp)
    suspend fun removerDisponibilidade(dispId: String) = dao.removerDisponibilidade(dispId)

    suspend fun inserirSessao(sessao: Sessao) = dao.inserirSessao(sessao)
    suspend fun atualizarSessao(sessao: Sessao) = dao.atualizarSessao(sessao)

    suspend fun inserirRevisao(revisao: Revisao) = dao.inserirRevisao(revisao)
    suspend fun atualizarRevisao(revisao: Revisao) = dao.atualizarRevisao(revisao)

    suspend fun inserirRetrospectiva(retro: Retrospectiva) = dao.inserirRetrospectiva(retro)

    suspend fun garantirDadosIniciais() {
        if (dao.getTodasAreas().isEmpty()) {
            dao.inserirArea(Area(nome = "Universidade", descricao = "Disciplinas, aulas, entregas e provas", cor = "#FF0055"))
            dao.inserirArea(Area(nome = "Programação", descricao = "Sistemas, repositórios e projetos práticos", cor = "#00F0FF"))
            dao.inserirArea(Area(nome = "Inglês", descricao = "Prática contínua, sessões e vocabulário", cor = "#00FF66"))
            dao.inserirArea(Area(nome = "Livros", descricao = "Leituras, capítulos, fichamentos e obras", cor = "#FFAA00"))
        }
        if (dao.getTodaDisponibilidade().isEmpty()) {
            val padrao = listOf(
                Disponibilidade(diaSemana = 1, horaInicio = "21:00", horaFim = "22:30", ativo = true),
                Disponibilidade(diaSemana = 2, horaInicio = "21:00", horaFim = "22:30", ativo = true),
                Disponibilidade(diaSemana = 3, horaInicio = "21:00", horaFim = "22:30", ativo = true),
                Disponibilidade(diaSemana = 4, horaInicio = "21:00", horaFim = "22:30", ativo = true),
                Disponibilidade(diaSemana = 5, horaInicio = "21:00", horaFim = "00:00", ativo = true),
                Disponibilidade(diaSemana = 6, horaInicio = "14:00", horaFim = "16:00", ativo = true),
                Disponibilidade(diaSemana = 0, horaInicio = "14:00", horaFim = "16:00", ativo = true)
            )
            padrao.forEach { dao.inserirDisponibilidade(it) }
        }
    }

    suspend fun getDadosBackup(): String {
        val root = JSONObject()
        root.put("appName", "Se Eduque Android Nível 2")
        root.put("version", 6)
        root.put("exportedAt", System.currentTimeMillis())

        root.put("areas", JSONArray().apply {
            dao.getTodasAreas().forEach { a ->
                put(JSONObject().apply {
                    put("id", a.id); put("nome", a.nome); put("descricao", a.descricao ?: "")
                    put("cor", a.cor); put("status", a.status)
                    put("criadoEm", a.criadoEm); put("alteradoEm", a.alteradoEm)
                })
            }
        })

        root.put("projetos", JSONArray().apply {
            dao.getTodosProjetos().forEach { p ->
                put(JSONObject().apply {
                    put("id", p.id); put("nome", p.nome); put("areaId", p.areaId ?: "")
                    put("objetivo", p.objetivo ?: ""); put("dataAlvo", p.dataAlvo ?: "")
                    put("linkExterno", p.linkExterno ?: ""); put("estado", p.estado)
                    put("tipoProjeto", p.tipoProjeto); put("unidadeMedida", p.unidadeMedida)
                    put("valorAtual", p.valorAtual); put("valorTotal", p.valorTotal)
                    put("arquivado", p.arquivado); put("criadoEm", p.criadoEm); put("alteradoEm", p.alteradoEm)
                })
            }
        })

        root.put("tarefas", JSONArray().apply {
            dao.getTodasTarefas().forEach { t ->
                put(JSONObject().apply {
                    put("id", t.id); put("titulo", t.titulo); put("descricao", t.descricao ?: "")
                    put("projetoId", t.projetoId ?: ""); put("objetivoId", t.objetivoId ?: "")
                    put("marcoId", t.marcoId ?: ""); put("tarefaPaiId", t.tarefaPaiId ?: "")
                    put("estado", t.estado); put("prioridade", t.prioridade)
                    put("prazoData", t.prazoData ?: ""); put("estimativaMinutos", t.estimativaMinutos)
                    put("minutosReais", t.minutosReais); put("arquivada", t.arquivada)
                    put("criadoEm", t.criadoEm); put("alteradoEm", t.alteradoEm)
                    put("concluidaEm", t.concluidaEm ?: 0L)
                })
            }
        })

        root.put("objetivos", JSONArray().apply {
            dao.getTodosObjetivos().forEach { o ->
                put(JSONObject().apply {
                    put("id", o.id); put("titulo", o.titulo); put("descricao", o.descricao ?: "")
                    put("areaId", o.areaId ?: ""); put("projetoId", o.projetoId ?: "")
                    put("horizonte", o.horizonte); put("dataAlvo", o.dataAlvo ?: "")
                    put("estado", o.estado); put("arquivado", o.arquivado); put("criadoEm", o.criadoEm)
                })
            }
        })

        root.put("marcos", JSONArray().apply {
            dao.getTodosMarcos().forEach { m ->
                put(JSONObject().apply {
                    put("id", m.id); put("projetoId", m.projetoId ?: ""); put("objetivoId", m.objetivoId ?: "")
                    put("titulo", m.titulo); put("dataAlvo", m.dataAlvo ?: "")
                    put("estado", m.estado); put("arquivado", m.arquivado); put("criadoEm", m.criadoEm)
                })
            }
        })

        root.put("dependencias", JSONArray().apply {
            dao.getTodasDependencias().forEach { d ->
                put(JSONObject().apply {
                    put("id", d.id); put("predecessoraId", d.predecessoraId)
                    put("sucessoraId", d.sucessoraId); put("notas", d.notas ?: "")
                    put("criadoEm", d.criadoEm)
                })
            }
        })

        root.put("disponibilidade", JSONArray().apply {
            dao.getTodaDisponibilidade().forEach { disp ->
                put(JSONObject().apply {
                    put("id", disp.id); put("diaSemana", disp.diaSemana)
                    put("horaInicio", disp.horaInicio); put("horaFim", disp.horaFim)
                    put("ativo", disp.ativo); put("validoDe", disp.validoDe ?: "")
                    put("validoAte", disp.validoAte ?: "")
                })
            }
        })

        root.put("sessoes", JSONArray().apply {
            dao.getTodasSessoes().forEach { s ->
                put(JSONObject().apply {
                    put("id", s.id); put("tarefaId", s.tarefaId ?: ""); put("projetoId", s.projetoId ?: "")
                    put("titulo", s.titulo); put("inicioMs", s.inicioMs); put("fimMs", s.fimMs)
                    put("duracaoMinutos", s.duracaoMinutos); put("estado", s.estado)
                    put("sincronizadoCalendar", s.sincronizadoCalendar)
                })
            }
        })

        root.put("revisoes", JSONArray().apply {
            dao.getTodasRevisoes().forEach { r ->
                put(JSONObject().apply {
                    put("id", r.id); put("titulo", r.titulo); put("tipoOrigem", r.tipoOrigem)
                    put("origemId", r.origemId ?: ""); put("regraIntervalo", r.regraIntervalo)
                    put("observacoes", r.observacoes ?: ""); put("dataAgendada", r.dataAgendada)
                    put("estado", r.estado); put("arquivada", r.arquivada)
                })
            }
        })

        root.put("retrospectivas", JSONArray().apply {
            dao.getTodasRetrospectivas().forEach { ret ->
                put(JSONObject().apply {
                    put("id", ret.id); put("dataMs", ret.dataMs)
                    put("oQueAvancou", ret.oQueAvancou); put("oQueBloqueou", ret.oQueBloqueou)
                    put("oQueMudar", ret.oQueMudar)
                })
            }
        })

        return root.toString(2)
    }

    suspend fun restaurarDadosBackup(jsonStr: String, substituirTudo: Boolean = true): Int {
        val root = JSONObject(jsonStr)

        val areas = mutableListOf<Area>()
        root.optJSONArray("areas")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                areas.add(
                    Area(
                        id = o.getString("id"),
                        nome = o.getString("nome"),
                        descricao = o.optString("descricao").takeIf { it.isNotBlank() },
                        cor = o.optString("cor", "#00F0FF"),
                        status = o.optString("status", "Ativa"),
                        criadoEm = o.optLong("criadoEm", System.currentTimeMillis()),
                        alteradoEm = o.optLong("alteradoEm", System.currentTimeMillis())
                    )
                )
            }
        }

        val projetos = mutableListOf<Projeto>()
        root.optJSONArray("projetos")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                projetos.add(
                    Projeto(
                        id = o.getString("id"),
                        nome = o.getString("nome"),
                        areaId = o.optString("areaId").takeIf { it.isNotBlank() },
                        objetivo = o.optString("objetivo").takeIf { it.isNotBlank() },
                        dataAlvo = o.optString("dataAlvo").takeIf { it.isNotBlank() },
                        linkExterno = o.optString("linkExterno").takeIf { it.isNotBlank() },
                        estado = o.optString("estado", "Ativo"),
                        tipoProjeto = o.optString("tipoProjeto", "TAREFAS"),
                        unidadeMedida = o.optString("unidadeMedida", "tarefas"),
                        valorAtual = o.optInt("valorAtual", 0),
                        valorTotal = o.optInt("valorTotal", 0),
                        arquivado = o.optBoolean("arquivado", false),
                        criadoEm = o.optLong("criadoEm", System.currentTimeMillis()),
                        alteradoEm = o.optLong("alteradoEm", System.currentTimeMillis())
                    )
                )
            }
        }

        val tarefas = mutableListOf<Tarefa>()
        root.optJSONArray("tarefas")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val conc = o.optLong("concluidaEm", 0L)
                tarefas.add(
                    Tarefa(
                        id = o.getString("id"),
                        titulo = o.getString("titulo"),
                        descricao = o.optString("descricao").takeIf { it.isNotBlank() },
                        projetoId = o.optString("projetoId").takeIf { it.isNotBlank() },
                        objetivoId = o.optString("objetivoId").takeIf { it.isNotBlank() },
                        marcoId = o.optString("marcoId").takeIf { it.isNotBlank() },
                        tarefaPaiId = o.optString("tarefaPaiId").takeIf { it.isNotBlank() },
                        estado = o.optString("estado", "Não iniciada"),
                        prioridade = o.optString("prioridade", "Média"),
                        prazoData = o.optString("prazoData").takeIf { it.isNotBlank() },
                        estimativaMinutos = o.optInt("estimativaMinutos", 0),
                        minutosReais = o.optInt("minutosReais", 0),
                        arquivada = o.optBoolean("arquivada", false),
                        criadoEm = o.optLong("criadoEm", System.currentTimeMillis()),
                        alteradoEm = o.optLong("alteradoEm", System.currentTimeMillis()),
                        concluidaEm = if (conc > 0L) conc else null
                    )
                )
            }
        }

        val objetivos = mutableListOf<Objetivo>()
        root.optJSONArray("objetivos")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                objetivos.add(
                    Objetivo(
                        id = o.getString("id"),
                        titulo = o.getString("titulo"),
                        descricao = o.optString("descricao").takeIf { it.isNotBlank() },
                        areaId = o.optString("areaId").takeIf { it.isNotBlank() },
                        projetoId = o.optString("projetoId").takeIf { it.isNotBlank() },
                        horizonte = o.optString("horizonte", "Médio Prazo"),
                        dataAlvo = o.optString("dataAlvo").takeIf { it.isNotBlank() },
                        estado = o.optString("estado", "Ativo"),
                        arquivado = o.optBoolean("arquivado", false),
                        criadoEm = o.optLong("criadoEm", System.currentTimeMillis())
                    )
                )
            }
        }

        val marcos = mutableListOf<Marco>()
        root.optJSONArray("marcos")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                marcos.add(
                    Marco(
                        id = o.getString("id"),
                        projetoId = o.optString("projetoId").takeIf { it.isNotBlank() },
                        objetivoId = o.optString("objetivoId").takeIf { it.isNotBlank() },
                        titulo = o.getString("titulo"),
                        dataAlvo = o.optString("dataAlvo").takeIf { it.isNotBlank() },
                        estado = o.optString("estado", "Pendente"),
                        arquivado = o.optBoolean("arquivado", false),
                        criadoEm = o.optLong("criadoEm", System.currentTimeMillis())
                    )
                )
            }
        }

        val dependencias = mutableListOf<Dependencia>()
        root.optJSONArray("dependencias")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                dependencias.add(
                    Dependencia(
                        id = o.getString("id"),
                        predecessoraId = o.getString("predecessoraId"),
                        sucessoraId = o.getString("sucessoraId"),
                        notas = o.optString("notas").takeIf { it.isNotBlank() }
                    )
                )
            }
        }

        val disponibilidade = mutableListOf<Disponibilidade>()
        root.optJSONArray("disponibilidade")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                disponibilidade.add(
                    Disponibilidade(
                        id = o.getString("id"),
                        diaSemana = o.optInt("diaSemana", 1),
                        horaInicio = o.optString("horaInicio", "21:00"),
                        horaFim = o.optString("horaFim", "22:30"),
                        ativo = o.optBoolean("ativo", true),
                        validoDe = o.optString("validoDe").takeIf { it.isNotBlank() },
                        validoAte = o.optString("validoAte").takeIf { it.isNotBlank() }
                    )
                )
            }
        }

        val sessoes = mutableListOf<Sessao>()
        root.optJSONArray("sessoes")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                sessoes.add(
                    Sessao(
                        id = o.getString("id"),
                        tarefaId = o.optString("tarefaId").takeIf { it.isNotBlank() },
                        projetoId = o.optString("projetoId").takeIf { it.isNotBlank() },
                        titulo = o.getString("titulo"),
                        inicioMs = o.optLong("inicioMs", System.currentTimeMillis()),
                        fimMs = o.optLong("fimMs", System.currentTimeMillis() + 3600000L),
                        duracaoMinutos = o.optInt("duracaoMinutos", 60),
                        estado = o.optString("estado", "Planejada"),
                        sincronizadoCalendar = o.optBoolean("sincronizadoCalendar", false)
                    )
                )
            }
        }

        val revisoes = mutableListOf<Revisao>()
        root.optJSONArray("revisoes")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                revisoes.add(
                    Revisao(
                        id = o.getString("id"),
                        titulo = o.getString("titulo"),
                        tipoOrigem = o.optString("tipoOrigem", "aula"),
                        origemId = o.optString("origemId").takeIf { it.isNotBlank() },
                        regraIntervalo = o.optString("regraIntervalo", "D+7"),
                        observacoes = o.optString("observacoes").takeIf { it.isNotBlank() },
                        dataAgendada = o.optLong("dataAgendada", System.currentTimeMillis()),
                        estado = o.optString("estado", "Pendente"),
                        arquivada = o.optBoolean("arquivada", false)
                    )
                )
            }
        }

        val retrospectivas = mutableListOf<Retrospectiva>()
        root.optJSONArray("retrospectivas")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                retrospectivas.add(
                    Retrospectiva(
                        id = o.getString("id"),
                        dataMs = o.optLong("dataMs", System.currentTimeMillis()),
                        oQueAvancou = o.optString("oQueAvancou", ""),
                        oQueBloqueou = o.optString("oQueBloqueou", ""),
                        oQueMudar = o.optString("oQueMudar", "")
                    )
                )
            }
        }

        if (substituirTudo) {
            dao.substituirBancoCompleto(areas, projetos, tarefas, objetivos, marcos, dependencias, disponibilidade, sessoes, revisoes, retrospectivas)
        } else {
            dao.mesclarBancoCompleto(areas, projetos, tarefas, objetivos, marcos, dependencias, disponibilidade, sessoes, revisoes, retrospectivas)
        }

        return areas.size + projetos.size + tarefas.size + objetivos.size + marcos.size +
                dependencias.size + disponibilidade.size + sessoes.size + revisoes.size + retrospectivas.size
    }

    suspend fun gerarCenarioTesteNivel2() {
        val areaProg = dao.getTodasAreas().find { it.nome.contains("Programação", ignoreCase = true) }
            ?: dao.getTodasAreas().firstOrNull()

        val projId = "proj_teste_nivel2"
        dao.inserirProjeto(
            Projeto(
                id = projId,
                nome = "[TESTE] Compilador C — Entrega Final",
                areaId = areaProg?.id,
                objetivo = "Validar K_calib, Gargalos e Caminho Crítico",
                estado = "Ativo"
            )
        )

        val agora = System.currentTimeMillis()
        val ha45Dias = agora - (45L * 24L * 3600L * 1000L)

        dao.inserirTarefa(
            Tarefa(
                id = "t_teste_calibracao",
                titulo = "[TESTE] Analisador Léxico (60m est ➔ 90m real)",
                projetoId = projId,
                estado = "Concluída",
                prioridade = "Alta",
                estimativaMinutos = 60,
                minutosReais = 90,
                concluidaEm = agora
            )
        )

        dao.inserirTarefa(
            Tarefa(
                id = "t_teste_a",
                titulo = "[TESTE] Gargalo A: Árvore Sintática AST",
                projetoId = projId,
                estado = "Em andamento",
                prioridade = "Crítica",
                estimativaMinutos = 120
            )
        )

        dao.inserirTarefa(
            Tarefa(
                id = "t_teste_b",
                titulo = "[TESTE] Sucessora B: Tabela de Símbolos",
                projetoId = projId,
                estado = "Aguardando/Bloqueada",
                prioridade = "Alta",
                estimativaMinutos = 90
            )
        )

        dao.inserirTarefa(
            Tarefa(
                id = "t_teste_c",
                titulo = "[TESTE] Sucessora C: Tratamento de Erros",
                projetoId = projId,
                estado = "Aguardando/Bloqueada",
                prioridade = "Média",
                estimativaMinutos = 45
            )
        )

        dao.inserirTarefa(
            Tarefa(
                id = "t_teste_d",
                titulo = "[TESTE] Entrega D: Geração de Código Assembly",
                projetoId = projId,
                estado = "Aguardando/Bloqueada",
                prioridade = "Crítica",
                estimativaMinutos = 60
            )
        )

        dao.inserirTarefa(
            Tarefa(
                id = "t_teste_incerteza",
                titulo = "[TESTE] Documentação Técnica (Sem estimativa)",
                projetoId = projId,
                estado = "Não iniciada",
                prioridade = "Baixa",
                estimativaMinutos = 0
            )
        )

        dao.inserirTarefa(
            Tarefa(
                id = "t_teste_fantasma",
                titulo = "[TESTE] Otimizador Experimental (Parada há 45 dias)",
                projetoId = projId,
                estado = "Pausada",
                prioridade = "Baixa",
                estimativaMinutos = 60,
                criadoEm = ha45Dias,
                alteradoEm = ha45Dias
            )
        )

        dao.inserirDependencia(Dependencia(id = "dep_teste_1", predecessoraId = "t_teste_a", sucessoraId = "t_teste_b"))
        dao.inserirDependencia(Dependencia(id = "dep_teste_2", predecessoraId = "t_teste_a", sucessoraId = "t_teste_c"))
        dao.inserirDependencia(Dependencia(id = "dep_teste_3", predecessoraId = "t_teste_b", sucessoraId = "t_teste_d"))
    }

    suspend fun limparCenarioTesteNivel2() {
        dao.deletarDependenciasTeste()
        dao.deletarTarefasTeste()
        dao.deletarProjetosTeste()
    }
}