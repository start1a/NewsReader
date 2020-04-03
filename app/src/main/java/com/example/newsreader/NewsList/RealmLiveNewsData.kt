package com.example.newsreader.NewsList

import androidx.lifecycle.LiveData
import io.realm.RealmChangeListener
import io.realm.RealmObject
import io.realm.RealmResults

class RealmLiveNewsData<T: RealmObject> (private val realmResults: RealmResults<T>)
    : LiveData<RealmResults<T>>() {

    init {
        value = realmResults
    }

    private val listener = RealmChangeListener<RealmResults<T>> { value = it }

    override fun onActive() {
        super.onActive()
        realmResults.addChangeListener(listener)
    }

    override fun onInactive() {
        super.onInactive()
        realmResults.removeChangeListener(listener)
    }
}