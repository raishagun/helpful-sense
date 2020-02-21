package com.andruid.magic.helpfulsense.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.andruid.magic.helpfulsense.database.entity.Action

@Dao
interface ActionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(action: Action)

    @Query("SELECT * FROM actions")
    fun getLiveActions(): LiveData<List<Action>>

    @Query("SELECT * FROM actions")
    fun getAllActions(): List<Action>

    @Delete
    fun delete(action: Action)
}