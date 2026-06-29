package com.project.phonemic

import android.content.Context

/**
 * Lưu tối đa 5 IP gần nhất vào SharedPreferences.
 * Mới nhất lên đầu, trùng thì move lên đầu thay vì thêm.
 */
class IpHistoryStore(context: Context) {

    private val prefs = context.getSharedPreferences("ip_history", Context.MODE_PRIVATE)
    private val key = "ips"
    private val max = 5

    fun getAll(): List<String> {
        return prefs.getString(key, "")!!
            .split(",")
            .filter { it.isNotBlank() }
    }

    fun save(ip: String) {
        if (ip.isBlank()) return
        val list = getAll().toMutableList()
        list.remove(ip)          // bỏ trùng
        list.add(0, ip)          // thêm đầu
        prefs.edit().putString(key, list.take(max).joinToString(",")).apply()
    }

    fun remove(ip: String) {
        val list = getAll().toMutableList()
        list.remove(ip)
        prefs.edit().putString(key, list.joinToString(",")).apply()
    }
}
