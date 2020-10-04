package com.example.chugger.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * @author Nikojoel
 * Database dao interface
 * Used in pair with view model to query the database
 */
@Dao
interface UserDao {
    /**
     * Gets all users as a live data
     */
    @Query("SELECT * FROM user")
    fun getAll(): LiveData<List<User>>

    /**
     * Inserts a new user to the database
     * replaces duplicate user if conflicted
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(user: User): Long
}
/* EOF */