package com.example.newsreader.NewsList


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.newsreader.CacheDataUpdate
import com.example.newsreader.DetailNewsActivity
import com.example.newsreader.R
import kotlinx.android.synthetic.main.fragment_news_list.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * A simple [Fragment] subclass.
 */
class NewsListFragment : Fragment() {

    private var listAdapter: NewsListAdapter? = null
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

            // 데이터 초기화
            viewModel!!.let {
                it.newsListLiveData.value?.let {
                    listAdapter = NewsListAdapter(it)
                    NewsListView.adapter = listAdapter
                    NewsListView.layoutManager = LinearLayoutManager(activity)
                }
                listAdapter?.let {
                    it.itemClickListener = {
                        val intent = Intent(activity, DetailNewsActivity::class.java)
                        intent.putStringArrayListExtra("data", it)
                        startActivity(intent)
                    }
                }
            }

            // 저장된 값을 불러오기 위해 같은 네임파일 찾기
            val sharedPreferences = activity!!.getSharedPreferences("sFile", Context.MODE_PRIVATE)
            // 저장된 값이 없으면 "" 반환
            val text = sharedPreferences.getString("text", "")
            // 최초 실행
            if (text.isNullOrEmpty()) {
                // 설정 중 UI
                CoroutineScope(Dispatchers.Main).launch {
                    // 웹 크롤링
                    llProgressBar.visibility = View.VISIBLE
                    CacheDataUpdate.WebCrawling(6)
                    llProgressBar.visibility = View.GONE
                    listAdapter?.notifyDataSetChanged()
                    CacheDataUpdate.WebCrawling(34)
                }
            }
        }

        // 밀어서 새로고침
        swipeLayout_newsList.setOnRefreshListener {
            // DB에서 최신 데이터 다시 가져오기
            listAdapter?.notifyDataSetChanged()
            swipeLayout_newsList.isRefreshing = false
        }
    }

    override fun onStop() {
        super.onStop()
        // 액티비티 종료 전에 저장
        val sharedPreferences = activity!!.getSharedPreferences("sFile", Context.MODE_PRIVATE)
        // editor에 값 저장
        val editor = sharedPreferences.edit()
        val text = "first executed app"
        editor.putString("text", text)
        // 최종 커밋
        editor.commit()
    }

}
