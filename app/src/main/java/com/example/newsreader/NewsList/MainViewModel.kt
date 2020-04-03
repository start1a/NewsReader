package com.example.newsreader.NewsList

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.newsreader.NewsDao
import com.example.newsreader.NewsData
import io.realm.Realm
import java.io.File
import java.util.*

class MainViewModel: ViewModel() {

    private val mRealm: Realm by lazy {
        Realm.getDefaultInstance()
    }

    private val mNewsDao: NewsDao by lazy {
        NewsDao(mRealm)
    }

    val newsListLiveData: RealmLiveNewsData<NewsData> by lazy {
        RealmLiveNewsData<NewsData>(mNewsDao.GetNewsData())
    }

    fun getNumData(): Long {
        return mNewsDao.getNumData()
    }

    override fun onCleared() {
        super.onCleared()
        mRealm.close()
    }
}