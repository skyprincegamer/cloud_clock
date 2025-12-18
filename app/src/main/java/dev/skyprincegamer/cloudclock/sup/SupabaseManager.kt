package dev.skyprincegamer.cloudclock.sup

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.ktor.client.engine.okhttp.OkHttp

object SupabaseManager {
    private var _client: SupabaseClient? = null

    fun getClient(): SupabaseClient {
        if (_client == null) {
            val supabase = createSupabaseClient(
                supabaseUrl = "https://dvoquibudfcvelmgyisr.supabase.co",
                supabaseKey ="sb_publishable_VpvAKa1D7yNjYlHsVuyzVw_eIZcEpf0"
            ) {
                install(Auth){
                    alwaysAutoRefresh = true
                    autoSaveToStorage = true
                    autoLoadFromStorage = true
                }
                install(Postgrest)
                install(Realtime)
                httpEngine = OkHttp.create()
            }
        }
        return _client!!
    }

    fun cleanup() {
        _client = null
    }
}