package com.seapip.thomas.wearify.di

import com.seapip.thomas.wearify.spotify.SpotifyApi
import com.seapip.thomas.wearify.wearify.WearifyApi
import org.koin.dsl.module.Module
import org.koin.dsl.module.applicationContext


val wearifyModule: Module = applicationContext {
    provide { WearifyApi() }
}

val spotifyModule: Module = applicationContext {
    provide { SpotifyApi() }
}

val appModule = listOf(wearifyModule, spotifyModule)