package com.example.chugger.database

import androidx.room.*

@Entity
data class User(@PrimaryKey(autoGenerate = true) val uid: Long, val userName: String, val time: String) {
    override fun toString(): String {
        return "($uid) $userName $time"
    }
}