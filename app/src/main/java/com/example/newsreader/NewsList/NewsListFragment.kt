package com.example.newsreader.NewsList


import android.content.Intent
import android.graphics.Bitmap
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.newsreader.DetailNewsActivity
import com.example.newsreader.ItemNews
import com.example.newsreader.R
import com.example.newsreader.ThumbnailLoader
import kotlinx.android.synthetic.main.fragment_news_list.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.IOException
import java.io.InputStream
import java.net.URL

/**
 * A simple [Fragment] subclass.
 */
class NewsListFragment : Fragment() {

    private var listNewsAdapter: NewsListAdapter? = null
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
            listNewsAdapter = NewsListAdapter(it.listNews)
            NewsListView.adapter = listNewsAdapter
            NewsListView.layoutManager = LinearLayoutManager(activity)
            newsClickListener()
        }
    }

    fun newsClickListener() {
        listNewsAdapter?.itemClickListener = {
            val intent = Intent(activity, DetailNewsActivity::class.java)
            intent.putExtra("link", it)
            startActivity(intent)
        }
    }

    inner class htmlTask: AsyncTask<Void, Void, MutableList<ItemNews>>() {
        override fun doInBackground(vararg params: Void?): MutableList<ItemNews> {

            val list = mutableListOf<ItemNews>()

            try {
                val doc = Jsoup.connect("https://news.google.com/rss?hl=ko&gl=KR&ceid=KR:ko").get()
                val html = doc.select("item")

                for (element: Element in html)
                {
                    // 데이터 존재
                    val page = element.select("link").text()
                    val newsData = SearchCacheNews(page)
                    if (newsData == null)
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
                        var ist: InputStream? = null
                        var image: Bitmap? = null
                        if (!imageUrl.isNullOrEmpty()) {
                            ist = URL(imageUrl).openStream()
                            image = ThumbnailLoader.decodeSampledBitmapFromResource(
                                ist,
                                imageUrl,
                                100,
                                100
                            )
                        }
                        // 뉴스 데이터
                        val itemNews = ItemNews(title, page, desc, image)
                        list.add(itemNews)
                        viewModel!!.listNews.add(itemNews)

                        ist?.close()
                    }
                    else list.add(newsData)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }

            return list
        }

        override fun onPostExecute(result: MutableList<ItemNews>) {
            super.onPostExecute(result)
            listNewsAdapter = NewsListAdapter(result)
            NewsListView.adapter = listNewsAdapter
            NewsListView.layoutManager = LinearLayoutManager(activity)
            newsClickListener()
            // listNewsAdapter?.notifyDataSetChanged()
            btnSet.text = result.size.toString()
        }

        fun SearchCacheNews(link: String): ItemNews? {
            for(news: ItemNews in viewModel!!.listNews)
            {
                if (link == news.link)
                    return news
            }
            return null
        }
    }
}
