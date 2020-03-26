package com.example.newsreader.NewsList

import androidx.lifecycle.ViewModel
import com.example.newsreader.ItemNews

class MainViewModel: ViewModel() {

    var listNews = mutableListOf<ItemNews>()

}