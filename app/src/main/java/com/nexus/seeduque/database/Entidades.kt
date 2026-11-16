package com.nexus.seeduque.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "areas")
data class Area(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val nome: String,
    val descricao: String? = null,
    val cor: String = "#00F0FF",
    val status: String = "Ativa",
    val criadoEm: Long = System.currentTimeMillis(),
    val alteradoEm: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "projetos",
    indices = [Index(value = ["areaId"]), Index(value = ["estado"])]
)
data class Projeto(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val nome: String,
    val areaId: String? = null,
    val objetivo: String? = null,
    val dataAlvo: String? = null, // Formato YYYY-MM-DD
    val linkExterno: String? = null,
    val estado: String = "Ativo", // Ativo, Pausado, Concluído
    val tipoProjeto: String = "TAREFAS", // TAREFAS, LIVRO, IDIOMA, DISCIPLINA
    val unidadeMedida: String = "tarefas",
    val valorAtual: Int = 0,
    val valorTotal: Int = 0,
    val arquivado: Boolean = false,
    val criadoEm: Long = System.currentTimeMillis(),
    val alteradoEm: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "tarefas",
    indices = [Index(value = ["projetoId", "estado"])]
)
data class Tarefa(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val titulo: String,
    val descricao: String? = null,
    val projetoId: String? = null,
    val objetivoId: String? = null,
    val marcoId: String? = null,
    val tarefaPaiId: String? = null,
    val estado: String = "Não iniciada", // Não iniciada, Em andamento, Aguardando/Bloqueada, Pausada, Concluída
    val prioridade: String = "Média",    // Baixa, Média, Alta, Crítica
    val prazoData: String? = null,       // Formato YYYY-MM-DD
    val estimativaMinutos: Int = 0,      // Essencial para K_calib e Incerteza Estrutural
    val minutosReais: Int = 0,           // Tempo real medido pelo cronómetro ou conclusão
    val arquivada: Boolean = false,
    val criadoEm: Long = System.currentTimeMillis(),
    val alteradoEm: Long = System.currentTimeMillis(),
    val concluidaEm: Long? = null
)

@Entity(tableName = "objetivos")
data class Objetivo(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val titulo: String,
    val descricao: String? = null,
    val areaId: String? = null,
    val projetoId: String? = null,
    val horizonte: String = "Médio Prazo",
    val dataAlvo: String? = null,
    val estado: String = "Ativo", // Ativo, Concluído
    val arquivado: Boolean = false,
    val criadoEm: Long = System.currentTimeMillis()
)

@Entity(tableName = "marcos")
data class Marco(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val projetoId: String? = null,
    val objetivoId: String? = null,
    val titulo: String,
    val dataAlvo: String? = null,
    val estado: String = "Pendente", // Pendente, Concluído
    val arquivado: Boolean = false,
    val criadoEm: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "dependencias",
    indices = [
        Index(value = ["predecessoraId", "sucessoraId"], unique = true),
        Index(value = ["predecessoraId"]),
        Index(value = ["sucessoraId"])
    ]
)
data class Dependencia(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val predecessoraId: String,
    val sucessoraId: String,
    val notas: String? = null,
    val criadoEm: Long = System.currentTimeMillis()
)

@Entity(tableName = "disponibilidade")
data class Disponibilidade(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val diaSemana: Int, // 0 = Domingo .. 6 = Sábado (-1 = Todos os dias na exceção)
    val horaInicio: String = "21:00",
    val horaFim: String = "22:30",
    val ativo: Boolean = true,
    val validoDe: String? = null,  // Exceção temporal YYYY-MM-DD
    val validoAte: String? = null  // Exceção temporal YYYY-MM-DD
)

@Entity(
    tableName = "sessoes",
    indices = [Index(value = ["inicioMs"])]
)
data class Sessao(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val tarefaId: String? = null,
    val projetoId: String? = null,
    val titulo: String,
    val inicioMs: Long,
    val fimMs: Long,
    val duracaoMinutos: Int = 60,
    val estado: String = "Planejada", // Planejada, Concluída
    val sincronizadoCalendar: Boolean = false
)

@Entity(tableName = "revisoes")
data class Revisao(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val titulo: String,
    val tipoOrigem: String = "aula", // aula, prova, projeto, tarefa
    val origemId: String? = null,
    val regraIntervalo: String = "D+7",
    val observacoes: String? = null,
    val dataAgendada: Long = System.currentTimeMillis(),
    val estado: String = "Pendente", // Pendente, Concluída
    val arquivada: Boolean = false
)

@Entity(tableName = "retrospectivas")
data class Retrospectiva(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val dataMs: Long = System.currentTimeMillis(),
    val oQueAvancou: String,
    val oQueBloqueou: String,
    val oQueMudar: String
)