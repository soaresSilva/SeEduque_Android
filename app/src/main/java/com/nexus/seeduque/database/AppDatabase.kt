package com.nexus.seeduque.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Area::class,
        Projeto::class,
        Tarefa::class,
        Objetivo::class,
        Marco::class,
        Dependencia::class,
        Disponibilidade::class,
        Sessao::class,
        Revisao::class,
        Retrospectiva::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(Conversores::class)
abstract class SeEduqueDatabase : RoomDatabase() {
    abstract fun seEduqueDao(): SeEduqueDao

    companion object {
        @Volatile
        private var INSTANCE: SeEduqueDatabase? = null

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE projetos ADD COLUMN tipoProjeto TEXT NOT NULL DEFAULT 'TAREFAS'")
                db.execSQL("ALTER TABLE projetos ADD COLUMN unidadeMedida TEXT NOT NULL DEFAULT 'tarefas'")
                db.execSQL("ALTER TABLE projetos ADD COLUMN valorAtual INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE projetos ADD COLUMN valorTotal INTEGER NOT NULL DEFAULT 0")

                db.execSQL("CREATE INDEX IF NOT EXISTS index_projetos_areaId ON projetos(areaId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_projetos_estado ON projetos(estado)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tarefas_projetoId_estado ON tarefas(projetoId, estado)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_dependencias_predecessoraId_sucessoraId ON dependencias(predecessoraId, sucessoraId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_dependencias_predecessoraId ON dependencias(predecessoraId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_dependencias_sucessoraId ON dependencias(sucessoraId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sessoes_inicioMs ON sessoes(inicioMs)")
            }
        }

        fun getDatabase(context: Context): SeEduqueDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SeEduqueDatabase::class.java,
                    "seeduque_database"
                )
                    .addMigrations(MIGRATION_5_6)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}