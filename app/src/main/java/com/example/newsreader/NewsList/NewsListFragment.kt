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
import com.example.newsreader.*
import io.realm.RealmList
import kotlinx.android.synthetic.main.fragment_news_list.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.util.*

/**
 * A simple [Fragment] subclass.
 */
class NewsListFragment : Fragment() {

    private var listNewsAdapter: NewsListAdapter? = null
    private var viewModel: MainViewModel? = null
    private var filesdir: File? = null

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
            ViewModelProvider(activity!!.viewModelStore,
                ViewModelProvider.AndroidViewModelFactory(it))
                .get(MainViewModel::class.java)
        }

        btnSet.setOnClickListener {
            htmlTask().execute()
        }

        // 리스트 출력
        viewModel!!.let {
            NewsListView.adapter = listNewsAdapter
            NewsListView.layoutManager = LinearLayoutManager(activity)

            it.listNews.observe(this, Observer {
                UpdateListAdapter(it)
            })
        }
    }

    fun UpdateListAdapter(list: MutableList<NewsData>) {

        listNewsAdapter = NewsListAdapter(list)
        listNewsAdapter.let {
            NewsListView.adapter = listNewsAdapter
            NewsListView.layoutManager = LinearLayoutManager(activity)

            it?.itemClickListener = {
                val intent = Intent(activity, DetailNewsActivity::class.java)
                intent.putExtra("link", it)
                startActivity(intent)
            }
        }
    }

    inner class htmlTask: AsyncTask<Void, Void, MutableList<NewsData>>() {
        override fun doInBackground(vararg params: Void?): MutableList<NewsData> {

            val list = mutableListOf<NewsData>()

            try {
                val doc = Jsoup.connect("https://news.google.com/rss?hl=ko&gl=KR&ceid=KR:ko").get()
                val html = doc.select("item")

                for (element: Element in html)
                {
                    // 기존 데이터(link)가 존재하는가
                    val page = element.select("link").text()
                    val selectedNewsData = viewModel!!.SearchNewsData(page)

                    // 없음
                    if (selectedNewsData == null)
                    {
                        var link: Document
                        try {
                            link = Jsoup.connect(page).get()
                        } catch (e: IOException) {
                            e.printStackTrace()
                            Log.d("httpException : ", page)
                            continue
                        }

                        // 텍스트
                        val title = element.select("title").text()
                        val desc = link.select("meta[property=og:description]").attr("content")

                        // 이미지
                        val imageUrl = link.select("meta[property=og:image]").attr("content")
                        var imagePath = ""
                        if (!imageUrl.isNullOrEmpty()) {
                            var ist: InputStream

                            ist = URL(imageUrl).openStream()
                            var image = ThumbnailLoader.decodeSampledBitmapFromResource(
                                ist,
                                imageUrl,
                                100,
                                100
                            )

                            // 비트맵을 내부 저장소에 저장
                            image?.let {
                                imagePath = ThumbnailLoader.SaveBitmapToJpeg(it, filesdir!!)
                            }

                            ist?.close()
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

            viewModel!!.listNews.value = result
            btnSet.text = result.size.toString()
        }
    }
}
