package com.example.newsreader.NewsList


import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.newsreader.*
import io.realm.RealmList
import kotlinx.android.synthetic.main.fragment_news_list.*
import kotlinx.coroutines.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import java.net.URL
import java.util.*
import java.util.regex.Pattern
import kotlin.coroutines.CoroutineContext

/**
 * A simple [Fragment] subclass.
 */
class NewsListFragment : Fragment(), CoroutineScope {

    private var listData: MutableList<NewsData> = mutableListOf()
    private var listAdapter: NewsListAdapter? = null
    private var viewModel: MainViewModel? = null

    private val NUM_IN_SCREEN = 5
    private var isFull = false

    private var mJob = Job()
    private var handler: CoroutineExceptionHandler =
        CoroutineExceptionHandler { coroutineContext, throwable ->
            Log.e("HandlerException!!", ":" + throwable.printStackTrace())
        }
    override val coroutineContext: CoroutineContext = mJob + Dispatchers.Main + handler
    private var coroutineScope = CoroutineScope(coroutineContext)

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
            viewModel!!.filesdir = activity!!.applicationContext.filesDir
            // 크롤링 실행
            startService()
        }

        // 리스트 출력
        viewModel!!.let {
            it.listNews.observe(this, Observer {
                listAdapter = NewsListAdapter(it)
                setRecyclerView()

                listAdapter.let {
                    it?.itemClickListener = {
                        val intent = Intent(activity, DetailNewsActivity::class.java)
                        intent.putExtra("link", it)
                        startActivity(intent)
                    }
                }
                TextNum.setText(it.size.toString() + " / " + viewModel!!.num_curNews_screen.toString())
            })
        }

        // 밀어서 새로고침
        swipeLayout_newsList.setOnRefreshListener {
            viewModel!!.let {
                it.num_curNews_screen = 0
                it.listNews.value = mutableListOf()
            }
            isFull = false
            startService()
        }

        // 마지막 아이템 스크롤
        NewsListView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                // 웹의 모든 데이터가 보여지면 더 이상 스크롤하지 않음
                if (!isFull) {
                    val lastVisibleItemPosition =
                        (recyclerView.layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition()
                    val itemTotalCount = recyclerView.adapter?.itemCount?.minus(1)

                    if (lastVisibleItemPosition == itemTotalCount) {
                        startService()
                    }
                }
                else Toast.makeText(activity, "더 이상 가져올 데이터가 없습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // 웹 크롤링 + 리스트 업데이트
    fun startService() {
        coroutineScope.launch {
            WebCrawling()
            viewModel!!.let {
                it.num_curNews_screen += NUM_IN_SCREEN
                val list = it.listNews.value
                list?.addAll(listData)
                it.listNews.value = list
            }
            listData = mutableListOf()
            swipeLayout_newsList.isRefreshing = false
        }
    }

    fun setRecyclerView() {
        NewsListView.adapter = listAdapter
        NewsListView.layoutManager = LinearLayoutManager(activity)
    }

    fun ExtractKeyWord(desc: String): RealmList<KeywordNewsDesc> {
        // 3자 이상의 문자나 숫자 조합
        val pattern = Pattern.compile("([^! (),.…·ㆍ~?\"\'“”‘’+-/{}|<>\n\t]{2,})")
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

    suspend fun WebCrawling() = withContext(Dispatchers.IO + handler) {

        val doc = Jsoup.connect("https://news.google.com/rss?hl=ko&gl=KR&ceid=KR:ko").get()
        val html = doc.select("item")
        // db 객체 생성
        viewModel!!.setRealmInsatance()
        // 불러올 데이터 인덱스
        var index = viewModel!!.num_curNews_screen
        var endIndex = index + NUM_IN_SCREEN
        if (endIndex > html.size) {
            endIndex = html.size
            isFull = true
        }
        // 5개 단위로 로드
        while (index < endIndex) {
            // 이미 데이터가 DB에 존재하는지 체크
            val url = html[index].select("link").text()
            val checkExistNewsData = viewModel!!.SearchNewsData(url)

            // 없음
            if (checkExistNewsData == null) {
                // 해당 링크의 크롤링이 가능한가
                var link: Document
                try {
                    link = Jsoup.connect(url).get()
                } catch (e: IOException) {
                    // 불가능할 경우 다음 아이템으로 이동
                    Log.e("TAGSSS", "IOException!! : " + url)
                    // 항상 5개씩 받기 위해 다음 인덱스로 이동
                    ++index
                    if (endIndex < html.size) ++endIndex
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
                        // 이미지 비트맵 리사이즈 작업
                        var ist = URL(imageUrl).openStream()
                        var image = ThumbnailLoader.decodeSampledBitmapFromResource(
                            ist,
                            imageUrl,
                            100,
                            100
                        )
                        // 정상 비트맵이 반환되면 내부 저장소에 저장
                        image?.let {
                            imagePath =
                                ThumbnailLoader.SaveBitmapToJpeg(it, viewModel!!.filesdir)
                        }
                        ist.close()
                    } catch (e: IOException) {
                        Log.e(
                            "TAG",
                            "IOException in Fragment : " + e.printStackTrace()
                        )
                    }
                }
                val newsData = NewsData(url, title, desc, imagePath, Date(), ExtractKeyWord(desc))
                listData.add(newsData)
                // DB에 데이터 저장
                viewModel!!.SaveNewsData(newsData)
            }
            else listData.add(checkExistNewsData)
            ++index
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mJob.cancel()
    }
}
