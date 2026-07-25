package com.example.data.repository

import kotlinx.coroutines.flow.Flow

interface KeyValueSettings {
    fun getString(key: String, defaultValue: String): String
    fun setString(key: String, value: String)
    fun getBoolean(key: String, defaultValue: Boolean): Boolean
    fun setBoolean(key: String, value: Boolean)
    fun getBooleanFlow(key: String, defaultValue: Boolean): Flow<Boolean>
}
