package com.example.newsreader.NewsList

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.newsreader.NewsDao
import com.example.newsreader.NewsData
import io.realm.Realm

class MainViewModel: ViewModel() {

    private val mRealm: Realm by lazy {
        Realm.getDefaultInstance()
    }

    private val mNewsDao: NewsDao by lazy {
        NewsDao(mRealm)
    }

    var listNews: MutableLiveData<MutableList<NewsData>>
            = MutableLiveData<MutableList<NewsData>>().apply { value = mutableListOf() }

    // 캐시할 데이터가 DB에 존재하는 지의 여부
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