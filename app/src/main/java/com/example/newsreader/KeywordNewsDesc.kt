package com.example.newsreader

import io.realm.RealmObject
import java.io.Serializable

open class KeywordNewsDesc (var keyword: String = "") : RealmObject()