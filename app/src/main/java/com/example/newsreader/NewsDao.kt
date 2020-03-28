package com.example.newsreader

import io.realm.Realm

class NewsDao(private val realm: Realm) {

    fun SaveNewsData(newsData: NewsData) {
        realm.executeTransaction {
            if (newsData.description.length > 100)
                newsData.description = newsData.description.substring(0..100)

            if (!newsData.isManaged)
                it.copyToRealmOrUpdate(newsData)
        }
    }

    fun SearchNewsData(link: String): NewsData? {
        var newsData: NewsData? =  null
        realm.executeTransactionAsync {
            newsData = it.where(NewsData::class.java)
                .equalTo("link", link)
                .findFirst()
        }
        if (newsData == null) return null
        else return realm.copyFromRealm(newsData)
    }

}