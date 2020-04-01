package com.example.newsreader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.util.Log
import java.io.*
import java.net.URL
import java.util.*

class ThumbnailLoader {

    companion object {

        fun calculateInSampleSize(
            options: BitmapFactory.Options,
            reqWidth: Int,
            reqHeight: Int
        ): Int {
            // Raw height and width of image
            val (height: Int, width: Int) = options.run { outHeight to outWidth }
            var inSampleSize = 1

            if (height > reqHeight || width > reqWidth) {

                val halfHeight: Int = height / 2
                val halfWidth: Int = width / 2

                // Calculate the largest inSampleSize value that is a power of 2 and keeps both
                // height and width larger than the requested height and width.
                while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                    inSampleSize *= 2
                }
            }

            return inSampleSize
        }

        fun decodeSampledBitmapFromResource(
            ist: InputStream,
            url: String,
            reqWidth: Int,
            reqHeight: Int
        ): Bitmap? {
            // First decode with inJustDecodeBounds=true to check dimensions
            return BitmapFactory.Options().run {
                inJustDecodeBounds = true
                BitmapFactory.decodeStream(ist, null,this)
                // Calculate inSampleSize
                inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)

                // Decode bitmap with inSampleSize set
                inJustDecodeBounds = false

                BitmapFactory.decodeStream(URL(url).openStream(), null,this)
            }
        }

        fun SaveBitmapToJpeg(bitmap: Bitmap, filesDir: File): String {

            // 파일 객체 생성
            val storage: File = filesDir
            val fileName = UUID.randomUUID().toString() + ".jpg"
            val tempFile = File(storage, fileName)

            // 파일 저장
            try {
                tempFile.createNewFile()
                val out = FileOutputStream(tempFile)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                out.close()

                return filesDir.toString() + "/" + fileName

            } catch (e: FileNotFoundException) {
                Log.e("TAG","Bitmap FileNotFoundException :  " + e.printStackTrace())
            } catch (e: IOException) {
                Log.e("TAG","Bitmap IOException : " + e.printStackTrace())
            }

            return ""
        }
    }
}