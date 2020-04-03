package com.example.newsreader

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import io.realm.Realm
import io.realm.RealmList
import kotlinx.coroutines.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.File
import java.io.IOException
import java.net.URL
import java.util.*
import java.util.regex.Pattern

class CacheDataUpdate: BroadcastReceiver() {

    companion object {

        private lateinit var mRealm: Realm
        private lateinit var mNewsDao: NewsDao
        lateinit var filesdir: File
        private var dateNews: Date? = null

        fun setRealmInsatance() {
            mRealm = Realm.getDefaultInstance()
            mNewsDao = NewsDao(mRealm)
        }

        suspend fun WebCrawling() = withContext(Dispatchers.IO) {

            val doc = Jsoup.connect("https://news.google.com/rss?hl=ko&gl=KR&ceid=KR:ko").get()
            val html = doc.select("item")
            // db 객체 생성
            setRealmInsatance()
            // 불러올 데이터 인덱스
            var index = 0
            while (index < html.size) {
                // 이미 데이터가 DB에 존재하는지 체크
                val url = html[index].select("link").text()
                val checkExistNewsData = mNewsDao.SearchNewsData(url)
                if (dateNews == null) dateNews = Date()
                // 있으면 저장 시간만 변경
                if (checkExistNewsData) mNewsDao.UpdateDataTime(url, Date())
                // 없으면 크롤링
                else
                {
                    // 해당 링크의 크롤링이 가능한가
                    var link: Document
                    try {
                        link = Jsoup.connect(url).get()
                    } catch (e: IOException) {
                        // 불가능할 경우 다음 아이템으로 이동
                        Log.e("link connect failed", "IOException!! : " + index.toString() + "  " + url)
                        // 항상 5개씩 받기 위해 다음 인덱스로 이동
                        ++index
                        continue
                    }

                    // 텍스트
                    val title = html[index].select("title").text()
                    var desc = link.select("meta[property=og:description]").attr("content")

                    // 이미지
                    var imageUrl = link.select("meta[property=og:image]").attr("content")
                    var imagePath = ""
                    if (!imageUrl.isNullOrEmpty()) {
                        try {
                            // 이미지 url -> 비트맵 : 리사이즈 작업
                            val ist = URL(imageUrl).openStream()
                            val image = ThumbnailLoader.decodeSampledBitmapFromResource(
                                ist,
                                imageUrl,
                                100,
                                100
                            )
                            // 정상 비트맵이 반환되면 내부 저장소에 저장
                            image?.let {
                                imagePath =
                                    ThumbnailLoader.SaveBitmapToJpeg(it, filesdir)
                            }
                            ist.close()
                        } catch (e: IOException) {
                            Log.d("imageURL", index.toString() + "  :  " + imageUrl)
                            Log.e(
                                "image stream error",
                                "IOException : " + e.printStackTrace()
                            )
                        }
                    }
                    val newsData = NewsData(url, title, desc, imagePath, Date(), ExtractKeyWord(desc))
                    // DB에 데이터 저장
                    mNewsDao.SaveNewsData(newsData)
                }
                ++index
            }
        }

        fun ExtractKeyWord(desc: String): RealmList<KeywordNewsDesc> {
            // 3자 이상의 문자나 숫자 조합
            val pattern = Pattern.compile("([^! (),.…·ㆍ~?=\\-\"\'“”‘’`+-/{}\\[\\]|<>\n\t]{2,})")
            val matcher = pattern.matcher(desc)
            val keyList = mutableListOf<Keyword>()

            // 키워드를 하나씩 반환
            var key: String
            while (matcher.find()) {
                key = matcher.group()
                // 중복 데이터 탐색
                var i = 0
                while (i < keyList.size && key != keyList[i].key) {
                    ++i
                }
                // 중복 없음
                if (i == keyList.size)
                    keyList.add(Keyword(key, 1))
                else ++keyList[i].num
            }

            // 개수로 내림차순 정렬 후
            // 개수가 동일한 데이터끼리는 텍스트 오름차순 정렬
            keyList.sortWith(object : Comparator<Keyword> {
                override fun compare(o1: Keyword, o2: Keyword): Int = when {
                    o1.num < o2.num -> 1
                    o1.num == o2.num -> {
                        when {
                            o1.key > o2.key -> 1
                            o1.key == o2.key -> 0
                            else -> -1
                        }
                    }
                    else -> -1
                }
            })

            if (keyList.size < 3)
                return getTopKeyword(keyList.subList(0, keyList.size))
            else return getTopKeyword(keyList.subList(0, 3))
        }

        fun getTopKeyword(list: MutableList<Keyword>): RealmList<KeywordNewsDesc> {
            return list.run {
                val realmList = RealmList<KeywordNewsDesc>()
                for (keyword: Keyword in this)
                    realmList.add(KeywordNewsDesc(keyword.key))

                realmList
            }
        }

        fun RemoveOldData(date: Date) {
            mNewsDao.RemoveOldData(date)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action)
        {
            "android.intent.action.BOOT_COMPLETED" -> {
                AlarmTool.setAlarm(context)
                filesdir = context.filesDir
            }
            AlarmTool.ACTION_UPDATE_CACHE_DATA -> {
                GlobalScope.launch {
                    WebCrawling()
                    RemoveOldData(dateNews!!)
                    dateNews = null
                }
            }
        }
    }
}