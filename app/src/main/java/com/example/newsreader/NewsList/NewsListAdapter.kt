package com.example.newsreader.NewsList

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.newsreader.KeywordNewsDesc
import com.example.newsreader.NewsData
import com.example.newsreader.R
import io.realm.RealmList
import kotlinx.android.synthetic.main.item_news.view.*

class NewsListAdapter(private val list: MutableList<NewsData>):
    RecyclerView.Adapter<NewsListViewHolder>() {

    lateinit var itemClickListener: (list: ArrayList<String>) -> Unit

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsListViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_news, parent, false)
        view.setOnClickListener {
            itemClickListener.run {
                val list = it.tag as ArrayList<String>
                this(list)
            }
        }
        return NewsListViewHolder(view)
    }

    override fun getItemCount(): Int {
        return list.count()
    }

    override fun onBindViewHolder(holder: NewsListViewHolder, position: Int) {

        // 썸네일
        if (!list[position].image.isNullOrEmpty())
            Glide.with(holder.containerView)
                .load(BitmapFactory.decodeFile(list[position].image))
                .error(R.drawable.ic_launcher_background)
                .into(holder.containerView.thumbnailNews)
        else holder.containerView.thumbnailNews.visibility = View.GONE

        // 제목
        if (list[position].title.length > 20)
            holder.containerView.textTitle.text = list[position].title.substring(0..20)
        else holder.containerView.textTitle.text = list[position].title

        // 본문
        if (list[position].description.length > 60)
            holder.containerView.textDesc.text = list[position].description.substring(0..60)
        else holder.containerView.textDesc.text = list[position].description

        // 키워드
        if (list[position].keywords.size == 0) {
            holder.containerView.textKey1.visibility = View.GONE
            holder.containerView.textKey2.visibility = View.GONE
            holder.containerView.textKey3.visibility = View.GONE
        }
        if (list[position].keywords.size > 0)
            holder.containerView.textKey1.text = list[position].keywords[0]?.keyword
        if (list[position].keywords.size > 1)
            holder.containerView.textKey2.text = list[position].keywords[1]?.keyword
        if (list[position].keywords.size > 2)
            holder.containerView.textKey3.text = list[position].keywords[2]?.keyword

        // 상세 보기 데이터
        val listStr = arrayListOf<String>()
        listStr.add(list[position].link)
        for (i in 0..list[position].keywords.size - 1)
            listStr.add(list[position].keywords[i]!!.keyword)
        holder.containerView.tag = listStr
    }
}