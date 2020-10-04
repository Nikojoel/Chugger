package com.example.chugger.database

import androidx.room.*

/**
 * @author Nikojoel
 * Data class User used in the database
 * @param uid Unique identifier of an user
 * @param userName Non unique username
 * @param time Users drinking time
 * @param city Users city location
 */
@Entity
data class User(@PrimaryKey(autoGenerate = true) val uid: Long, val userName: String, val time: String, val city: String) {
    override fun toString(): String {
        return "($uid) $userName $time $city"
    }
}
/* EOF */