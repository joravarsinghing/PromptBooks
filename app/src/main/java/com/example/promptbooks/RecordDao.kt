package com.example.promptbooks

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface RecordDao {

    @Insert
    suspend fun insertRecord(record: Record)

    @Query("SELECT * FROM records ORDER BY id DESC")
    suspend fun getAllRecords(): List<Record>

    @Query("DELETE FROM records WHERE id = (SELECT id FROM records ORDER BY id ASC LIMIT 1)")
    suspend fun deleteFirst()

    @Query("DELETE FROM records WHERE description LIKE '%' || :keyword || '%'")
    suspend fun deleteByDescription(keyword: String)

}
