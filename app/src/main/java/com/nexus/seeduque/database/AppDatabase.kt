package com.nexus.seeduque.database



import android.content.Context

import androidx.room.Database

import androidx.room.Room

import androidx.room.RoomDatabase

import androidx.room.TypeConverters



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

    version = 5,

    exportSchema = false

)

@TypeConverters(Conversores::class)

abstract class SeEduqueDatabase : RoomDatabase() {

    abstract fun seEduqueDao(): SeEduqueDao



    companion object {

        @Volatile

        private var INSTANCE: SeEduqueDatabase? = null



        fun getDatabase(context: Context): SeEduqueDatabase {

            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(

                    context.applicationContext,

                    SeEduqueDatabase::class.java,

                    "seeduque_database"

                )

                    .fallbackToDestructiveMigration()

                    .build()

                INSTANCE = instance

                instance

            }

        }

    }

}