package com.example.newsreader

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.io.Serializable
import java.util.*

open class NewsData (
    @PrimaryKey
    var link: String = "",
    var title: String = "",
    var description: String = "",
    var image: String = "",
    var date: Date = Date(),
    var keywords: RealmList<KeywordNewsDesc> = RealmList()
): RealmObject()