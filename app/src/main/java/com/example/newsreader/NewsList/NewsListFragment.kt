package com.example.newsreader.NewsList


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.newsreader.CacheDataUpdate
import com.example.newsreader.DetailNewsActivity
import com.example.newsreader.R
import kotlinx.android.synthetic.main.fragment_news_list.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * A simple [Fragment] subclass.
 */
class NewsListFragment : Fragment() {

    private var listAdapter: NewsListAdapter? = null
    private var viewModel: MainViewModel? = null
    private var job: Job? = null

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

        // 밀어서 새로고침
        swipeLayout_newsList.setOnRefreshListener {
            // DB에서 최신 데이터 다시 가져오기
            listAdapter?.notifyDataSetChanged()
            swipeLayout_newsList.isRefreshing = false
        }

        // 최하단 스크롤
//        NewsListView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
//            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
//                super.onScrolled(recyclerView, dx, dy)
//                val lastVisibleItemPosition =
//                    (recyclerView.layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition()
//                val itemTotalCount = recyclerView.adapter?.itemCount?.minus(1)
//
//                if (lastVisibleItemPosition == itemTotalCount) {
//                    Log.d("TAG", "lastVisibleItemPosition!!")
//                    // 마지막 아이템이 최대 개수 미만일 경우 (더 불러올 데이터가 존재)
//                    if (!isFull)
//                        BottomScrollProgressBar.visibility = View.VISIBLE
//                    else
//                        Toast.makeText(activity, "더 이상 불러올 뉴스가 없습니다", Toast.LENGTH_SHORT).show()
//                }
//            }
//        })

        AppFirstExecute()
    }

    // 앱 최초 실행 뉴스 로드
    fun AppFirstExecute() {
        // 저장된 값을 불러오기 위해 같은 네임파일 찾기
        val sharedPreferences = activity!!.getSharedPreferences("sFile", Context.MODE_PRIVATE)
        // 저장된 값이 없으면 "" 반환
        val text = sharedPreferences.getString("text", "")
        // 최초 실행
        if (text.isNullOrEmpty()) {
            // 설정 중 UI
            job = CoroutineScope(Dispatchers.Main).launch {
                // 웹 크롤링
                llProgressBar.visibility = View.VISIBLE
                CacheDataUpdate.WebCrawling(0, 6)
                llProgressBar.visibility = View.GONE
                listAdapter?.notifyDataSetChanged()
                CacheDataUpdate.WebCrawling(6, CacheDataUpdate.MAX_NEWS_DATA_SIZE)
                listAdapter?.notifyDataSetChanged()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 화면 재개 시 코루틴 제거
        job = null
    }

    override fun onStop() {
        super.onStop()

        // 코루틴 제거
        job?.cancel()
        job = null

        // 액티비티 종료 전에 저장
        val sharedPreferences = activity!!.getSharedPreferences("sFile", Context.MODE_PRIVATE)
        // editor에 값 저장
        val editor = sharedPreferences.edit()
        val text = "first executed app"
        editor.putString("text", text)
        // 최종 커밋
        editor.apply()
    }

}
