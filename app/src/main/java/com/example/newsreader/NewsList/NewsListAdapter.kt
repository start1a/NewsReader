package com.example.newsreader.NewsList

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.newsreader.ItemNews
import com.example.newsreader.R
import kotlinx.android.synthetic.main.item_news.view.*

class NewsListAdapter(private val list: MutableList<ItemNews>):
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
            Glide.with(holder.containerView)
                .load(list[position].image)
                .error(R.drawable.icon_earth)
                .override(150)
                .into(holder.containerView.thumbnailNews)
        else holder.containerView.thumbnailNews.visibility = View.GONE

        // 제목
        holder.containerView.textTitle.text = list[position].title
        // 본문
        holder.containerView.textDesc.text = list[position].description
        // 링크 주소
        holder.containerView.tag = list[position].link
    }
}