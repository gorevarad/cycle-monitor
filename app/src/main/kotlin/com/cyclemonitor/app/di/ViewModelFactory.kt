package com.cyclemonitor.app.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Minimal generic ViewModel factory so each screen can construct its ViewModel from [AppContainer]
 * without pulling in a DI framework. */
class ViewModelFactory<T : ViewModel>(private val create: () -> T) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <U : ViewModel> create(modelClass: Class<U>): U = create() as U
}
