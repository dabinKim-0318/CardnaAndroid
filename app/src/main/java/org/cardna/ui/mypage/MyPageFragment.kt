package org.cardna.ui.mypage

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.cardna.base.baseutil.BaseViewUtil
import org.cardna.data.remote.api.ApiService
import org.cardna.data.remote.model.mypage.ResponseMyPageFriendData
import org.cardna.databinding.FragmentMyPageBinding
import org.cardna.ui.maincard.MainCardFragment
import org.cardna.ui.mypage.adapter.MyPageFriendAdapter
import org.cardna.util.SpacesItemDecoration
import org.cardna.util.initRootClickEvent
import org.cardna.util.shortToast
import kotlin.math.roundToInt
import android.view.MotionEvent
import android.view.View.OnTouchListener
import android.widget.Toast


class MyPageFragment :
    BaseViewUtil.BaseFragment<FragmentMyPageBinding>(org.cardna.R.layout.fragment_my_page) {
    private lateinit var list: MutableList<ResponseMyPageFriendData>

    override fun onResume() {
        super.onResume()
        binding.etMypageSearchBackground.setText("")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    override fun initView() {
        initScrollView()
        initClickEvent()
        initNetwork()
        initRootClickEvent(binding.ctlMypageTop)
        initRootClickEvent(binding.ctlMypageHeader)
    }

    private fun initScrollView() {
        binding.scMypage.run {
            header = binding.ctlMypageHeader
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun myPageRecyclerViewAdapter(dataList: MutableList<ResponseMyPageFriendData>) {
        binding.ivMypageSetting.setOnTouchListener { _, _ -> true }
        list = dataList
        val myPageFriendAdapter = MyPageFriendAdapter(dataList) { item ->
            val bundle = Bundle()
            bundle.putInt("id", item.id)
            bundle.putString("name", item.name)
            bundle.putString("userImg", item.userImg)
            bundle.putString("sentence", item.sentence)

            val mainCardFragment = MainCardFragment()
            mainCardFragment.arguments = bundle

            val transaction = requireActivity().supportFragmentManager.beginTransaction()
                .addToBackStack(null)
                .add(org.cardna.R.id.fcv_main, mainCardFragment)
            transaction.commit()

        }
        val gridLayoutManager = GridLayoutManager(requireContext(), 2)
        with(binding) {
            rvMypage.layoutManager = gridLayoutManager
            rvMypage.addItemDecoration(SpacesItemDecoration((16 * resources.displayMetrics.density).roundToInt()))
            rvMypage.adapter = myPageFriendAdapter
        }
        myPageFriendAdapter.notifyDataSetChanged()
    }

    private fun initClickEvent() {
        with(binding) {
            var friendName = ""
            etMypageSearchBackground.setOnEditorActionListener { textView, action, event ->
                var handled = false
                if (action == EditorInfo.IME_ACTION_DONE) {
                    //검색완료 키보드 이벤트 완료 시 리사이클러뷰 item 새로 띄우기
                    friendName = etMypageSearchBackground.text.toString()
                    lifecycleScope.launch {
                        try {
                            val result = ApiService.friendService.getFriendName(friendName)
                            if (result.success && list.isNotEmpty()) {
                                list.clear()
                                list = mutableListOf(ResponseMyPageFriendData(result.data.id, result.data.name, result.data.userImg, result.data.sentence ?: ""))
                                myPageRecyclerViewAdapter(list)
                            }
                        } catch (e: Exception) {
                            Toast.makeText(requireContext(), "존재하지 않는 친구입니다.",Toast.LENGTH_SHORT).show()
                            Log.d("실패", e.message.toString())
                        }
                    }
                    handled = true
                }
                handled
            }

            ivMypageGotoEmailsearch.setOnClickListener {
                startActivity(Intent(requireContext(), SearchEmailActivity::class.java))
            }
            ivMypageOtherWrote.setOnClickListener {
                startActivity(Intent(requireContext(), OtherWriteActivity::class.java))
            }
            ivMypageSetting.setOnClickListener {
                startActivity(Intent(requireContext(), SettingActivity::class.java))
            }
        }
    }

    private fun initNetwork() {
        lifecycleScope.launch {
            try {
                val dataContainer = ApiService.myPageService.getMyPage().data
                val myData = listOf(
                    dataContainer.name,
                    dataContainer.email,
                    dataContainer.userImg,
                    dataContainer.friendList.size.toString()
                )
                list = dataContainer.friendList
                setMyPage(myData)
                myPageRecyclerViewAdapter(list)
            } catch (e: Exception) {
                Log.d("실패", e.message.toString())
            }
        }
    }

    private fun setMyPage(myData: List<String>) {
        with(binding) {
            tvMypageName.text = myData[0]
            tvMypageEmail.text = myData[1]
            tvMypageFriendCount.text = myData[3]
        }
        Glide
            .with(this@MyPageFragment)
            .load(myData[2])
            .circleCrop()
            .into(binding.ivMypageProfile)
    }
}