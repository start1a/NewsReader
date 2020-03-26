package com.example.newsreader

import android.graphics.Bitmap

data class ItemNews (
    val title: String,
    val link: String,
    val description: String,
    val image: Bitmap?
)