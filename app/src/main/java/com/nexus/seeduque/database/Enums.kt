package com.nexus.seeduque.database



import androidx.room.TypeConverter



enum class EstadoTarefa(val rotulo: String) {

    NAO_INICIADA("Não iniciada"),

    EM_ANDAMENTO("Em andamento"),

    AGUARDANDO_BLOQUEADA("Aguardando/Bloqueada"),

    PAUSADA("Pausada"),

    CONCLUIDA("Concluída")

}



enum class EstadoProjeto(val rotulo: String) {

    ATIVO("Ativo"),

    PAUSADO("Pausado"),

    CONCLUIDO("Concluído")

}



enum class Prioridade(val rotulo: String) {

    BAIXA("Baixa"),

    MEDIA("Média"),

    ALTA("Alta"),

    CRITICA("Crítica")

}



class Conversores {

    @TypeConverter fun deEstadoTarefa(v: EstadoTarefa): String = v.name

    @TypeConverter fun paraEstadoTarefa(v: String): EstadoTarefa = runCatching { EstadoTarefa.valueOf(v) }.getOrDefault(EstadoTarefa.NAO_INICIADA)



    @TypeConverter fun deEstadoProjeto(v: EstadoProjeto): String = v.name

    @TypeConverter fun paraEstadoProjeto(v: String): EstadoProjeto = runCatching { EstadoProjeto.valueOf(v) }.getOrDefault(EstadoProjeto.ATIVO)



    @TypeConverter fun dePrioridade(v: Prioridade?): String? = v?.name

    @TypeConverter fun paraPrioridade(v: String?): Prioridade? = v?.let { runCatching { Prioridade.valueOf(it) }.getOrNull() }

}