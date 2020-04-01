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

    lateinit var itemClickListener: (link: String) -> Unit

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsListViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_news, parent, false)
        view.setOnClickListener {
            itemClickListener.run {
                val link = it.tag as String
                this(link)
            }
        }
        return NewsListViewHolder(view)
    }

    override fun getItemCount(): Int {
        return list.count()
    }

    override fun onBindViewHolder(holder: NewsListViewHolder, position: Int) {

        // 썸네일
        if (list[position].image != null)
            holder.containerView.thumbnailNews.setImageBitmap(BitmapFactory.decodeFile(list[position].image))
//            Glide.with(holder.containerView)
//                .load(BitmapFactory.decodeFile(list[position].image))
//                .error(R.drawable.icon_earth)
//                .override(150)
//                .into(holder.containerView.thumbnailNews)
        else holder.containerView.thumbnailNews.visibility = View.GONE

        // 제목
        holder.containerView.textTitle.text = list[position].title
        // 본문
        if (list[position].description.length > 60)
            holder.containerView.textDesc.text = list[position].description.substring(0..60)
        else holder.containerView.textDesc.text = list[position].description
        // 링크 주소
        holder.containerView.tag = list[position].link
        // 키워드
        holder.containerView.textKeyword.text = "키워드 : "
        for (text: KeywordNewsDesc in list[position].keywords)
            holder.containerView.textKeyword.append(text.keyword + "  ")
    }
}