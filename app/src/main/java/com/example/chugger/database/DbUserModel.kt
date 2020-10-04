package com.example.chugger.database

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData

/**
 * @author Nikojoel
 * Class that holds functionality to control the database
 */
class DbUserModel(application: Application): AndroidViewModel(application) {

    // All users as live data
    private val users: LiveData<List<User>> = UserDB.get(getApplication()).userDao().getAll()

    // Returns users
    fun getUsers() = users

    /**
     * Inserts a new user to the database
     * @param user
     */
    fun insertNew(user: User) {
        UserDB.get(getApplication()).userDao().insert(user)
    }
}
/* EOF */