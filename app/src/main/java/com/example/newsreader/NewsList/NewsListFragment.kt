package com.example.newsreader.NewsList


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.newsreader.*
import io.realm.RealmList
import io.realm.RealmResults
import kotlinx.android.synthetic.main.fragment_news_list.*
import kotlinx.coroutines.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import java.net.URL
import java.util.*
import java.util.regex.Pattern

/**
 * A simple [Fragment] subclass.
 */
class NewsListFragment : Fragment() {

    private var listAdapter: NewsListAdapter? = null
    private var viewModel: MainViewModel? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_news_list, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // 액티비티를 처음 시작
        if (viewModel == null) {

            // 뷰모델 생성
            viewModel = activity!!.application!!.let {
                ViewModelProvider(
                    activity!!.viewModelStore,
                    ViewModelProvider.AndroidViewModelFactory(it)
                )
                    .get(MainViewModel::class.java)
            }

            // 데이터 초기화
            viewModel!!.let {
                it.newsListLiveData.value?.let {
                    listAdapter = NewsListAdapter(it)
                    NewsListView.adapter = listAdapter
                    NewsListView.layoutManager = LinearLayoutManager(activity)
                }
                listAdapter?.let {
                    it.itemClickListener = {
                        val intent = Intent(activity, DetailNewsActivity::class.java)
                        intent.putStringArrayListExtra("data", it)
                        startActivity(intent)
                    }
                }
            }

            // 저장된 값을 불러오기 위해 같은 네임파일 찾기
            val sharedPreferences = activity!!.getSharedPreferences("sFile", Context.MODE_PRIVATE)
            // 저장된 값이 없으면 "" 반환
            val text = sharedPreferences.getString("text", "")
            // 최초 실행
            if (text.isNullOrEmpty()) {
                // 설정 중 UI
                CoroutineScope(Dispatchers.Main).launch {
                    // 웹 크롤링
                    llProgressBar.visibility = View.VISIBLE
                    CacheDataUpdate.WebCrawling()
                    llProgressBar.visibility = View.GONE
                    listAdapter?.notifyDataSetChanged()
                }
            }
        }

        // 밀어서 새로고침
        swipeLayout_newsList.setOnRefreshListener {
            // DB에서 최신 데이터 다시 가져오기
            listAdapter?.notifyDataSetChanged()
            swipeLayout_newsList.isRefreshing = false
        }

    }

    override fun onStop() {
        super.onStop()
        // 액티비티 종료 전에 저장
        val sharedPreferences = activity!!.getSharedPreferences("sFile", Context.MODE_PRIVATE)
        // editor에 값 저장
        val editor = sharedPreferences.edit()
        val text = "first executed app"
        editor.putString("text", text)
        // 최종 커밋
        editor.commit()
    }


//    fun ExtractKeyWord(desc: String): RealmList<KeywordNewsDesc> {
//        // 3자 이상의 문자나 숫자 조합
//        val pattern = Pattern.compile("([^! (),.…·ㆍ~?=\\-\"\'“”‘’`+-/{}\\[\\]|<>\n\t]{2,})")
//        val matcher = pattern.matcher(desc)
//        val keyList = mutableListOf<Keyword>()
//
//        // 키워드를 하나씩 반환
//        var key: String
//        while (matcher.find()) {
//            key = matcher.group()
//            // 중복 데이터 탐색
//            var i = 0
//            while (i < keyList.size && key != keyList[i].key) {
//                ++i
//            }
//            // 중복 없음
//            if (i == keyList.size)
//                keyList.add(Keyword(key, 1))
//            else ++keyList[i].num
//        }
//
//        // 개수로 내림차순 정렬 후
//        // 개수가 동일한 데이터끼리는 텍스트 오름차순 정렬
//        keyList.sortWith(object : Comparator<Keyword> {
//            override fun compare(o1: Keyword, o2: Keyword): Int = when {
//                o1.num < o2.num -> 1
//                o1.num == o2.num -> {
//                    when {
//                        o1.key > o2.key -> 1
//                        o1.key == o2.key -> 0
//                        else -> -1
//                    }
//                }
//                else -> -1
//            }
//        })
//
//        if (keyList.size < 3)
//            return getTopKeyword(keyList.subList(0, keyList.size))
//        else return getTopKeyword(keyList.subList(0, 3))
//    }
//
//    fun getTopKeyword(list: MutableList<Keyword>): RealmList<KeywordNewsDesc> {
//        return list.run {
//            val realmList = RealmList<KeywordNewsDesc>()
//            for (keyword: Keyword in this)
//                realmList.add(KeywordNewsDesc(keyword.key))
//
//            realmList
//        }
//    }
//
//    suspend fun WebCrawling() = withContext(Dispatchers.IO) {
//
//        val doc = Jsoup.connect("https://news.google.com/rss?hl=ko&gl=KR&ceid=KR:ko").get()
//        val html = doc.select("item")
//        // db 객체 생성
//        viewModel!!.setRealmInsatance()
//        // 불러올 데이터 인덱스
//        var index = viewModel!!.num_curNews_screen
//        var endIndex = index + NUM_IN_SCREEN
//        if (endIndex > html.size) {
//            endIndex = html.size
//            isFull = true
//        }
//        num_next_crwalIndex = index
//        // 5개 단위로 로드
//        while (index < endIndex && isActive) {
//            // 이미 데이터가 DB에 존재하는지 체크
//            val url = html[index].select("link").text()
//            val checkExistNewsData = viewModel!!.SearchNewsData(url)
//
//            // 없음
//            if (checkExistNewsData == null) {
//                // 해당 링크의 크롤링이 가능한가
//                var link: Document
//                try {
//                    link = Jsoup.connect(url).get()
//                } catch (e: IOException) {
//                    // 불가능할 경우 다음 아이템으로 이동
//                    Log.e("link connect failed", "IOException!! : " + index.toString() + "  " + url)
//                    // 항상 5개씩 받기 위해 다음 인덱스로 이동
//                    ++index
//                    if (endIndex < html.size) ++endIndex
//                    continue
//                }
//
//                // 텍스트
//                val title = html[index].select("title").text()
//                var desc = link.select("meta[property=og:description]").attr("content")
//
//                // 이미지
//                var imageUrl = link.select("meta[property=og:image]").attr("content")
//                var imagePath = ""
//                if (!imageUrl.isNullOrEmpty()) {
//                    try {
//                        // 이미지 url -> 비트맵 : 리사이즈 작업
//                        val ist = URL(imageUrl).openStream()
//                        val image = ThumbnailLoader.decodeSampledBitmapFromResource(
//                            ist,
//                            imageUrl,
//                            100,
//                            100
//                        )
//                        // 정상 비트맵이 반환되면 내부 저장소에 저장
//                        image?.let {
//                            imagePath =
//                                ThumbnailLoader.SaveBitmapToJpeg(it, viewModel!!.filesdir)
//                        }
//                        ist.close()
//                    } catch (e: IOException) {
//                        Log.d("imageURL", index.toString() + "  :  " + imageUrl)
//                        Log.e(
//                            "image stream error",
//                            "IOException : " + e.printStackTrace()
//                        )
//                    }
//                }
//                val newsData = NewsData(url, title, desc, imagePath, Date(), ExtractKeyWord(desc))
//                listData.add(newsData)
//                // DB에 데이터 저장
//                viewModel!!.SaveNewsData(newsData)
//            } else listData.add(checkExistNewsData)
//            ++index
//        }
//        // 스크롤 당 크롤링한 데이터 개수
//        num_next_crwalIndex = index - num_next_crwalIndex
//    }
}
