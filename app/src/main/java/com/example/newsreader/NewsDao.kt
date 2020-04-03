package com.example.newsreader

import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort
import java.util.*

class NewsDao(private val realm: Realm) {

    fun SaveNewsData(newsData: NewsData) {
        realm.executeTransaction {
            if (newsData.description.length > 100)
                newsData.description = newsData.description.substring(0..100)

            if (!newsData.isManaged)
                it.copyToRealmOrUpdate(newsData)
        }
    }

    // date 시간을 기준으로 가장 최신 데이터 추출
    fun GetNewsData(): RealmResults<NewsData> {
//        var dbDatas = realm.where(NewsData::class.java)
//            .lessThanOrEqualTo("date", date)
//            .sort("date", Sort.DESCENDING)
//            .findAll()
//        return dbDatas.subList(0,5) as RealmResults<NewsData>
        return realm.where(NewsData::class.java)
            .sort("date", Sort.ASCENDING)
            .findAll()
    }

    // 기존의 데이터 유무 확인
    fun SearchNewsData(link: String): Boolean {
        var newsData = realm.where(NewsData::class.java)
            .equalTo("link", link)
            .findFirst()
        if (newsData == null) return false
        else return true
    }

    // 기존 데이터 시간 갱신
    fun UpdateDataTime(link: String, date: Date) {
        var prevData = realm.where(NewsData::class.java)
            .equalTo("link", link)
            .findFirst()

        realm.executeTransaction {
            prevData!!.let {
                it.date = date
                realm.copyToRealmOrUpdate(it)
            }
        }
    }

    fun RemoveOldData(date: Date) {
        val removeList = realm.where(NewsData::class.java)
            .lessThanOrEqualTo("date", date)
            .findAll()
        removeList.deleteAllFromRealm()
    }

    fun getNumData(): Long {
        return realm.where(NewsData::class.java).count()
    }

}