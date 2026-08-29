package com.example.data.repository

import kotlinx.cinterop.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Foundation.*
import platform.Security.*

@OptIn(ExperimentalForeignApi::class)
private object IOSKeychain {
    private const val SERVICE_NAME = "com.example.investec.keychain"

    fun save(key: String, value: String): Boolean {
        val data = (value as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return false
        delete(key)

        val query = NSMutableDictionary().apply {
            setObject(kSecClassGenericPassword, kSecClass)
            setObject(SERVICE_NAME, kSecAttrService)
            setObject(key, kSecAttrAccount)
            setObject(data, kSecValueData)
            setObject(kSecAttrAccessibleAfterFirstUnlock, kSecAttrAccessible)
        }

        val status = SecItemAdd(query, null)
        return status == errSecSuccess
    }

    fun get(key: String): String? {
        val query = NSMutableDictionary().apply {
            setObject(kSecClassGenericPassword, kSecClass)
            setObject(SERVICE_NAME, kSecAttrService)
            setObject(key, kSecAttrAccount)
            setObject(kCFBooleanTrue, kSecReturnData)
            setObject(kSecMatchLimitOne, kSecMatchLimit)
        }

        return memScoped {
            val dataTypeRef = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query, dataTypeRef.ptr)
            if (status == errSecSuccess) {
                val data = CFBridgingRelease(dataTypeRef.value) as? NSData
                data?.let {
                    NSString.create(data = it, encoding = NSUTF8StringEncoding)?.toString()
                }
            } else {
                null
            }
        }
    }

    fun delete(key: String) {
        val query = NSMutableDictionary().apply {
            setObject(kSecClassGenericPassword, kSecClass)
            setObject(SERVICE_NAME, kSecAttrService)
            setObject(key, kSecAttrAccount)
        }
        SecItemDelete(query)
    }
}

class IOSKeyValueSettings : KeyValueSettings {
    private val defaults = NSUserDefaults.standardUserDefaults
    private val initialSandbox = if (defaults.objectForKey("use_sandbox") == null) true else defaults.boolForKey("use_sandbox")
    private val sandboxFlow = MutableStateFlow(initialSandbox)
    private val stringFlows = mutableMapOf<String, MutableStateFlow<String>>()
    private val sensitiveKeys = setOf("client_id", "client_secret", "api_key")

    override fun getString(key: String, defaultValue: String): String {
        if (key == "default_sandbox_client_id") return BankRepository.DEFAULT_SANDBOX_CLIENT_ID
        if (key == "default_sandbox_client_secret") return BankRepository.DEFAULT_SANDBOX_CLIENT_SECRET
        if (key == "default_sandbox_api_key") return BankRepository.DEFAULT_SANDBOX_API_KEY

        if (key in sensitiveKeys) {
            return IOSKeychain.get(key) ?: defaultValue
        }
        return defaults.stringForKey(key) ?: defaultValue
    }

    override fun setString(key: String, value: String) {
        if (key in sensitiveKeys) {
            if (value.isEmpty()) {
                IOSKeychain.delete(key)
            } else {
                IOSKeychain.save(key, value)
            }
        } else {
            defaults.setObject(value, key)
            defaults.synchronize()
        }
        stringFlows[key]?.value = value
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

    override fun getStringFlow(key: String, defaultValue: String): Flow<String> {
        return stringFlows.getOrPut(key) {
            MutableStateFlow(getString(key, defaultValue))
        }.asStateFlow()
    }
}
