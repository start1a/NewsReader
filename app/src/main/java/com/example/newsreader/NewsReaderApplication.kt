package com.example.newsreader

import android.app.Application
import io.realm.Realm
import io.realm.RealmConfiguration

class NewsReaderApplication: Application() {

    override fun onCreate() {
        super.onCreate()

        Realm.init(this)
        val realmConfiguration = RealmConfiguration.Builder()
            .schemaVersion(0)
            .migration(NewsReaderDBMigration())
            .deleteRealmIfMigrationNeeded()
            .build()
        Realm.setDefaultConfiguration(realmConfiguration)
    }

}