package com.example.chugger.database

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData

class DbUserModel(application: Application): AndroidViewModel(application) {
    private val users: LiveData<List<User>> = UserDB.get(getApplication()).userDao().getAll()

    fun getUsers() = users

    fun insertNew(user: User) {
        UserDB.get(getApplication()).userDao().insert(user)
    }
}