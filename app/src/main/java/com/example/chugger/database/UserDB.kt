package com.example.chugger.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * @author Nikojoel
 * Abstract database class
 */
@Database(entities = [(User::class)], version = 1)
abstract class UserDB: RoomDatabase() {
    abstract fun userDao(): UserDao

    companion object {
        private var syncInstance: UserDB? = null

        /**
         * Builds a synchronized database and returns it
         * @param context Interface to global information about an application environment
         * @return UserDB
         */
        @Synchronized
        fun get(context: Context): UserDB {
            // Check if null
            if (syncInstance == null) {
                syncInstance = Room.databaseBuilder(context.applicationContext, UserDB::class.java, "user.db").build()
            }
            return syncInstance!!
        }
    }
}
/* EOF */