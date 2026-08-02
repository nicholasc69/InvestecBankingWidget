package com.example.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import platform.Foundation.NSUserDefaults

class IOSKeyValueSettings : KeyValueSettings {
    private val defaults = NSUserDefaults.standardUserDefaults
    private val initialSandbox = if (defaults.objectForKey("use_sandbox") == null) true else defaults.boolForKey("use_sandbox")
    private val sandboxFlow = MutableStateFlow(initialSandbox)

    override fun getString(key: String, defaultValue: String): String {
        // Return default sandbox credentials if not set
        if (key == "default_sandbox_client_id") return BankRepository.DEFAULT_SANDBOX_CLIENT_ID
        if (key == "default_sandbox_client_secret") return BankRepository.DEFAULT_SANDBOX_CLIENT_SECRET
        if (key == "default_sandbox_api_key") return BankRepository.DEFAULT_SANDBOX_API_KEY
        
        return defaults.stringForKey(key) ?: defaultValue
    }

    override fun setString(key: String, value: String) {
        defaults.setObject(value, key)
        defaults.synchronize()
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        if (defaults.objectForKey(key) == null) return defaultValue
        return defaults.boolForKey(key)
    }

    override fun setBoolean(key: String, value: Boolean) {
        defaults.setBool(value, key)
        defaults.synchronize()
        if (key == "use_sandbox") {
            sandboxFlow.value = value
        }
    }

    override fun getBooleanFlow(key: String, defaultValue: Boolean): Flow<Boolean> {
        if (key == "use_sandbox") {
            return sandboxFlow
        }
        return kotlinx.coroutines.flow.flowOf(getBoolean(key, defaultValue))
    }
}
