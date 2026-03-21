package me.leonunes.games.rooksandwalls.di

import me.leonunes.games.common.player.PlayersManagerFactory
import me.leonunes.games.rooksandwalls.model.GameFactory
import me.leonunes.games.rooksandwalls.model.GameManagerFactory
import org.koin.dsl.module

val rooksAndWallsModule = module {
    single<GameFactory> { GameFactory() }

    single { PlayersManagerFactory() }

    single { GameManagerFactory(get(), get()) }
}
