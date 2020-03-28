package com.example.newsreader

import io.realm.DynamicRealm
import io.realm.FieldAttribute
import io.realm.RealmMigration
import java.util.*

class NewsReaderDBMigration : RealmMigration {
    override fun migrate(realm: DynamicRealm, oldVersion: Long, newVersion: Long) {

        var oldVersion = oldVersion
        val schema = realm.schema

        /*
    Migrate to version 1: Add NewsData class.
    open class NewsData (
    var title: String = "",
    @PrimaryKey
    var link: String = "",
    var description: String = "",
    var keywords: RealmList<KeywordNewsDesc> = RealmList(),
    var image: Bitmap?
): RealmObject()
         */
        if (oldVersion == 0L) {
            schema.create("NewsData")
                .addField("link", String::class.javaPrimitiveType, FieldAttribute.PRIMARY_KEY)
                .addField("title", String::class.java)
                .addField("description", String::class.java)
                .addField("image", String::class.java)
                .addField("date", Date::class.java)
                .addRealmListField("keywords", schema.get("MemoImageData"))
            ++oldVersion
        }

    }
}