package com.example.newsreader.NewsList

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.newsreader.NewsDao
import com.example.newsreader.NewsData
import io.realm.Realm

class MainViewModel: ViewModel() {

    private lateinit var mRealm: Realm

    private lateinit var mNewsDao: NewsDao

    var num_curNews_screen = 0

    var listNews: MutableLiveData<MutableList<NewsData>>
            = MutableLiveData<MutableList<NewsData>>().apply { value = mutableListOf() }

    fun setRealmInsatance() {
        mRealm = Realm.getDefaultInstance()
        mNewsDao = NewsDao(mRealm)
    }

    // 캐시할 데이터가 DB에 존재하면 해당 데이터 반환
    fun SearchNewsData(link: String): NewsData? {
        return mNewsDao.SearchNewsData(link)
    }

    fun SaveNewsData(newsData: NewsData) {
        mNewsDao.SaveNewsData(newsData)
    }

    override fun onCleared() {
        super.onCleared()
        mRealm.close()
    }
}