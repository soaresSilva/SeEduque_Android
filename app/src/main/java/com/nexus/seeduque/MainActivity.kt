package com.nexus.seeduque



import android.os.Bundle

import androidx.activity.ComponentActivity

import androidx.activity.compose.setContent

import androidx.lifecycle.ViewModel

import androidx.lifecycle.ViewModelProvider

import com.nexus.seeduque.database.SeEduqueDatabase

import com.nexus.seeduque.repository.SeEduqueRepository

import com.nexus.seeduque.ui.MainAppScreen

import com.nexus.seeduque.ui.MainViewModel



class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)



        val database = SeEduqueDatabase.getDatabase(applicationContext)

        val repository = SeEduqueRepository(database.seEduqueDao())



        val factory = object : ViewModelProvider.Factory {

            @Suppress("UNCHECKED_CAST")

            override fun <T : ViewModel> create(modelClass: Class<T>): T {

                return MainViewModel(repository) as T

            }

        }

        val viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]



        setContent {

            MainAppScreen(viewModel = viewModel)

        }

    }

}

