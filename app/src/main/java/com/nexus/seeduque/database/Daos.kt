package com.nexus.seeduque.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SeEduqueDao {
    // ÁREAS
    @Query("SELECT * FROM areas WHERE status != 'Arquivada' ORDER BY criadoEm ASC")
    fun observarAreas(): Flow<List<Area>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirArea(area: Area)
    @Update
    suspend fun atualizarArea(area: Area)

    // PROJETOS
    @Query("SELECT * FROM projetos WHERE arquivado = 0 ORDER BY criadoEm DESC")
    fun observarProjetos(): Flow<List<Projeto>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirProjeto(projeto: Projeto)
    @Update
    suspend fun atualizarProjeto(projeto: Projeto)

    // TAREFAS
    @Query("SELECT * FROM tarefas WHERE arquivada = 0 ORDER BY criadoEm DESC")
    fun observarTarefas(): Flow<List<Tarefa>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirTarefa(tarefa: Tarefa)
    @Update
    suspend fun atualizarTarefa(tarefa: Tarefa)

    // OBJETIVOS
    @Query("SELECT * FROM objetivos WHERE arquivado = 0 ORDER BY criadoEm DESC")
    fun observarObjetivos(): Flow<List<Objetivo>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirObjetivo(objetivo: Objetivo)
    @Update
    suspend fun atualizarObjetivo(objetivo: Objetivo)

    // MARCOS
    @Query("SELECT * FROM marcos WHERE arquivado = 0 ORDER BY criadoEm DESC")
    fun observarMarcos(): Flow<List<Marco>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirMarco(marco: Marco)
    @Update
    suspend fun atualizarMarco(marco: Marco)

    // DEPENDÊNCIAS
    @Query("SELECT * FROM dependencias ORDER BY criadoEm DESC")
    fun observarDependencias(): Flow<List<Dependencia>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirDependencia(dep: Dependencia)
    @Query("DELETE FROM dependencias WHERE id = :depId")
    suspend fun removerDependencia(depId: String)

    // DISPONIBILIDADE & EXCEÇÕES
    @Query("SELECT * FROM disponibilidade ORDER BY diaSemana ASC")
    fun observarDisponibilidade(): Flow<List<Disponibilidade>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirDisponibilidade(disp: Disponibilidade)
    @Update
    suspend fun atualizarDisponibilidade(disp: Disponibilidade)
    @Query("DELETE FROM disponibilidade WHERE id = :dispId")
    suspend fun removerDisponibilidade(dispId: String)

    // SESSÕES
    @Query("SELECT * FROM sessoes ORDER BY inicioMs ASC")
    fun observarSessoes(): Flow<List<Sessao>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirSessao(sessao: Sessao)
    @Update
    suspend fun atualizarSessao(sessao: Sessao)

    // REVISÕES DE ESTUDO
    @Query("SELECT * FROM revisoes WHERE arquivada = 0 ORDER BY dataAgendada ASC")
    fun observarRevisoes(): Flow<List<Revisao>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirRevisao(revisao: Revisao)
    @Update
    suspend fun atualizarRevisao(revisao: Revisao)

    // RETROSPECTIVAS SEMANAIS
    @Query("SELECT * FROM retrospectivas ORDER BY dataMs DESC")
    fun observarRetrospectivas(): Flow<List<Retrospectiva>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirRetrospectiva(retro: Retrospectiva)

    // LEITURA ESTÁTICA PARA BACKUP PORTÁTIL E SEMENTEIRA INICIAL
    @Query("SELECT * FROM areas") suspend fun getTodasAreas(): List<Area>
    @Query("SELECT * FROM projetos") suspend fun getTodosProjetos(): List<Projeto>
    @Query("SELECT * FROM tarefas") suspend fun getTodasTarefas(): List<Tarefa>
    @Query("SELECT * FROM objetivos") suspend fun getTodosObjetivos(): List<Objetivo>
    @Query("SELECT * FROM marcos") suspend fun getTodosMarcos(): List<Marco>
    @Query("SELECT * FROM dependencias") suspend fun getTodasDependencias(): List<Dependencia>
    @Query("SELECT * FROM disponibilidade") suspend fun getTodaDisponibilidade(): List<Disponibilidade>
    @Query("SELECT * FROM sessoes") suspend fun getTodasSessoes(): List<Sessao>
    @Query("SELECT * FROM revisoes") suspend fun getTodasRevisoes(): List<Revisao>
    @Query("SELECT * FROM retrospectivas") suspend fun getTodasRetrospectivas(): List<Retrospectiva>
}