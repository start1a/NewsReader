package com.example.newsreader.NewsList


import android.content.Intent
import android.os.AsyncTask
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
import kotlinx.android.synthetic.main.fragment_news_list.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URL
import java.util.*

/**
 * A simple [Fragment] subclass.
 */
class NewsListFragment : Fragment() {

    private var listAdapter: NewsListAdapter? = null
    private var viewModel: MainViewModel? = null
    private var filesdir: File? = null

    private val NUM_IN_SCREEN = 5
    private var num_curNews_screen = 0

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

        // 뷰모델 생성
        viewModel = activity!!.application!!.let {
            ViewModelProvider(
                activity!!.viewModelStore,
                ViewModelProvider.AndroidViewModelFactory(it)
            )
                .get(MainViewModel::class.java)
        }

        // 리스트 출력
        viewModel!!.listNews.observe(this, Observer {
            listAdapter = NewsListAdapter(it)
            num_curNews_screen = it.size
            setRecyclerView()

            listAdapter.let {
                it?.itemClickListener = {
                    val intent = Intent(activity, DetailNewsActivity::class.java)
                    intent.putExtra("link", it)
                    startActivity(intent)
                }
            }
        })
        setRecyclerView()
        htmlTask().execute()

        swipeLayout_newsList.setOnRefreshListener {
            viewModel!!.listNews.value = mutableListOf()
            htmlTask().execute()
        }

        NewsListView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val lastVisibleItemPosition =
                    (recyclerView.layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition()
                val itemTotalCount = recyclerView.adapter?.itemCount?.minus(1)

                if (lastVisibleItemPosition == itemTotalCount) {
                    htmlTask().execute()
                }
            }
        })
    }

    fun setRecyclerView() {
        NewsListView.adapter = listAdapter
        NewsListView.layoutManager = LinearLayoutManager(activity)
    }

    fun ExtractKeyWord(desc: String): RealmList<KeywordNewsDesc> {
        var i = 0
        var key = ""
        var keylist = mutableListOf<Keyword>()

        while (i < desc.length) {
            if (desc[i] != ' ')
                key += desc[i]
            // 키워드가 3자 이상
            else if (key.length >= 3) {
                // 중복 키워드 탐색
                var j = 0
                while (j < keylist.size && key != keylist[j].key) {}
                // 중복 없음
                if (j == keylist.size)
                    keylist.add(Keyword(key,1))
                else ++keylist[j].num
            }
            key = ""
            ++i
        }

        return RealmList<KeywordNewsDesc>().apply {

        }
    }

    inner class htmlTask : AsyncTask<Void, Void, MutableList<NewsData>>() {
        override fun doInBackground(vararg params: Void?): MutableList<NewsData> {

            val list = mutableListOf<NewsData>()

            try {
                val doc = Jsoup.connect("https://news.google.com/rss?hl=ko&gl=KR&ceid=KR:ko").get()
                val html = doc.select("item")
                // db 객체 생성
                viewModel!!.setRealmInsatance()

                for (index in num_curNews_screen..num_curNews_screen + NUM_IN_SCREEN - 1) {
                    // 기존 데이터(link)가 존재하는가
                    val page = html.get(index).select("link").text()
                    val selectedNewsData = viewModel!!.SearchNewsData(page)

                    // 없음
                    if (selectedNewsData == null) {
                        var link: Document
                        try {
                            link = Jsoup.connect(page).get()
                        } catch (e: IOException) {
                            e.printStackTrace()
                            Log.e("httpException : ", page)
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
                                Log.e("TAG", "FileNotFoundException : " + e.printStackTrace())
                            }
                        }
                        // 뉴스 데이터

                        val keywords = RealmList<KeywordNewsDesc>()
                        keywords.add(KeywordNewsDesc("가"))
                        keywords.add(KeywordNewsDesc("나"))
                        keywords.add(KeywordNewsDesc("다"))
                        val newsData = NewsData(page, title, desc, imagePath, Date(), keywords)
                        viewModel!!.SaveNewsData(newsData)
                        list.add(newsData)
                    }
                    // 있음
                    else list.add(selectedNewsData)
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
            viewModel!!.listNews.value = list
            swipeLayout_newsList.isRefreshing = false
        }
    }
}
