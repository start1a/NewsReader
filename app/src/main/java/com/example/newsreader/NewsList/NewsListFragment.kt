package com.example.newsreader.NewsList


import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
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
import kotlinx.android.synthetic.main.fragment_news_list.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.File
import java.io.FileNotFoundException
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
    private var filesdir: File? = null

    private val NUM_IN_SCREEN = 5
    private var handler: Handler? = null
    private var isFull = false
    private var htmlTasks: htmlTask? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_news_list, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        filesdir = activity!!.applicationContext.filesDir

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
            // 크롤링 실행
            executeHTML()
        }

        handler = Handler()

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
        setRecyclerView()

        swipeLayout_newsList.setOnRefreshListener {
            viewModel!!.let{
                it.listNews.value = mutableListOf()
                it.num_curNews_screen = 0
            }
            isFull = false
            executeHTML()
        }

        NewsListView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (!isFull) {
                    val lastVisibleItemPosition =
                        (recyclerView.layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition()
                    val itemTotalCount = recyclerView.adapter?.itemCount?.minus(1)

                    if (lastVisibleItemPosition == itemTotalCount) {
                        htmlTask().execute()
                    }
                }
            }
        })
    }

    override fun onStop() {
        super.onStop()
        htmlTasks?.cancel(true)
    }

    fun setRecyclerView() {
        NewsListView.adapter = listAdapter
        NewsListView.layoutManager = LinearLayoutManager(activity)
    }

    fun executeHTML() {
        htmlTasks = htmlTask()
        htmlTasks?.execute()
    }

    fun ExtractKeyWord(desc: String): RealmList<KeywordNewsDesc> {
        // 3자 이상의 문자나 숫자 조합
        val pattern = Pattern.compile("([^! (),.?\"\'‘’{}|<>]{2,})")
        val matcher = pattern.matcher(desc)
        val keyList = mutableListOf<Keyword>()

        var key: String
        // 키워드를 하나씩 반환
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

        var aa = 0
        for (item: Keyword in keyList)
        {
            Log.d("TAG1", aa++.toString() + " : (" + item.key + "  " + item.num + ")")
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

        var a = 0
        for (item: Keyword in keyList)
        {
            Log.d("TAG2", a++.toString() + " : (" + item.key + "  " + item.num + ")")
        }

        return RealmList<KeywordNewsDesc>().apply {
            var i = 0
            while (i < keyList.size && i < 3) {
                try {
                    this.add(KeywordNewsDesc(keyList[i].key))
                } catch (e: Exception) {
                    Log.d("TAG", "undefinedException : " + e.printStackTrace().toString())
                }
            }
        }
    }

    inner class htmlTask : AsyncTask<Void, Void, MutableList<NewsData>>() {
        override fun doInBackground(vararg params: Void?): MutableList<NewsData> {

            val list = mutableListOf<NewsData>()

            try {
                val doc = Jsoup.connect("https://news.google.com/rss?hl=ko&gl=KR&ceid=KR:ko").get()
                val html = doc.select("item")
//                handler?.post(Runnable {
//                    Toast.makeText(activity, html.size.toString(), Toast.LENGTH_LONG).show()
//                })
                // db 객체 생성
                viewModel!!.setRealmInsatance()
                // 불러올 데이터 인덱스
                var index = viewModel!!.num_curNews_screen
                var endIndex = index + NUM_IN_SCREEN
                if (endIndex > html.size) {
                    endIndex = html.size
                    isFull = true
                }
                // 5개씩 불러옴
                while (index < endIndex) {
                    // 기존 데이터(link)가 존재하는가
                    val page = html.get(index).select("link").text()
                    val selectedNewsData = viewModel!!.SearchNewsData(page)

                    // 없음
                    if (selectedNewsData == null) {
                        var link: Document
                        // 웹 크롤링이 가능한 사이트인지
                        try {
                            link = Jsoup.connect(page).get()
                        } catch (e: IOException) {
                            // 불가능할 시 해당 사이트 크롤링 취소
                            // 해당 아이템을 가져올 수 없어 다음 아이템으로 개수를 채움
                            Log.e("httpException : ", page + "\n" + e.printStackTrace())
                            if (endIndex < html.size) ++endIndex
                            continue
                        }

                        // 텍스트
                        val title = html.get(index).select("title").text()
                        val desc = link.select("meta[property=og:description]").attr("content")

                        // 이미지
                        val imageUrl = link.select("meta[property=og:image]").attr("content")
                        var imagePath = ""
                        if (!imageUrl.isNullOrEmpty()) {
                            try {
                                var ist = URL(imageUrl).openStream()
                                var image = ThumbnailLoader.decodeSampledBitmapFromResource(
                                    ist,
                                    imageUrl,
                                    100,
                                    100
                                )
                                // 정상 비트맵이 반환되면 내부 저장소에 저장
                                image?.let {
                                    imagePath = ThumbnailLoader.SaveBitmapToJpeg(it, filesdir!!)
                                }
                                ist.close()
                            } catch (e: FileNotFoundException) {
                                Log.e(
                                    "TAG",
                                    "FileNotFoundException in Fragment : " + e.printStackTrace()
                                )
                            }
                        }
                         //키워드
                        val keywords = RealmList<KeywordNewsDesc>()
                        keywords.add(KeywordNewsDesc("가"))
                        keywords.add(KeywordNewsDesc("나"))
                        keywords.add(KeywordNewsDesc("다"))
                        // 썸네일 이미지
                        //val bitmap = BitmapFactory.decodeFile(imagePath)
                        // 뉴스 데이터
                        val newsData = NewsData(page, title, desc, imagePath, Date(), keywords)
                        viewModel!!.SaveNewsData(newsData)
                        list.add(newsData)
                    }
                    // 있음
                    else list.add(selectedNewsData)

                    ++index
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }

            return list
        }

        override fun onPostExecute(result: MutableList<NewsData>) {
            super.onPostExecute(result)
            val list = viewModel!!.listNews.value
            list?.addAll(result)
            viewModel!!.let {
                it.num_curNews_screen += NUM_IN_SCREEN
                it.listNews.value = list
            }
            swipeLayout_newsList.isRefreshing = false
        }
    }
}
