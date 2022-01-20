package org.cardna.ui.mypage
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.cardna.R
import org.cardna.base.baseutil.BaseViewUtil
import org.cardna.data.remote.api.ApiService
import org.cardna.data.remote.model.mypage.RequestCreateCardYouData
import org.cardna.databinding.ActivityOtherCardWriteBinding
import org.cardna.ui.cardpack.BottomDialogOtherImageFragment
import org.cardna.util.initRootClickEvent
import org.cardna.util.shortToast
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

class OtherCardWriteActivity
    :
    BaseViewUtil.BaseAppCompatActivity<ActivityOtherCardWriteBinding>(R.layout.activity_other_card_write) {

    private var symbolId: Int? = null
    private var uri: Uri? = null
    private var ifChooseImg: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
    }

    override fun initView() {
        setOtherName()
        checkEditTextLength()
        makeCardListener()
        setChooseCardListener()
        setOtherName()
        initRootClickEvent(binding.tvOthercardwriteTitle)
        initRootClickEvent(binding.svOthercardwriteTop)
    }

    private fun setOtherName(){
        val name = intent.getStringExtra("name")
        binding.tvOthercardwriteTitle.text = "${name}는"
    }

    // editText 글자 수에 따라 글자 수 업데이트, 버튼 선택가능하도록
    private fun checkEditTextLength() {
        binding.ivOthercardwriteGalleryImg.clipToOutline = true // imageView 모서리 둥글게
        binding.btnOthercardwriteComplete.isClickable = false;

        binding.etOthercardwriteKeyword.addTextChangedListener {
            checkCompleteBtnClickable()
            binding.tvOthercardwriteCntKeyword.text = "${binding.etOthercardwriteKeyword.length()}/14"
        }

        binding.etOthercardwriteDetail.addTextChangedListener {
            checkCompleteBtnClickable()
            binding.tvOthercardwriteCntDetail.text = "${binding.etOthercardwriteDetail.length()}/200"
        }

    }

    private fun checkCompleteBtnClickable(){
        if (binding.etOthercardwriteKeyword.length() > 0 && binding.etOthercardwriteDetail.length() > 0 && ifChooseImg) {
            with(binding) {
                btnOthercardwriteComplete.isClickable = true
                btnOthercardwriteComplete.isEnabled = true
                btnOthercardwriteComplete.setTextColor(resources.getColor(R.color.black))
                btnOthercardwriteComplete.setBackgroundResource(R.drawable.background_btn_cardyou_abled)
            }
        } else {
            with(binding) {
                btnOthercardwriteComplete.isClickable = false;
                btnOthercardwriteComplete.isEnabled = false;
                btnOthercardwriteComplete.setTextColor(resources.getColor(R.color.white_2))
                btnOthercardwriteComplete.setBackgroundResource(R.drawable.background_btn_card_disabled)
            }
        }
    }

    // bottomDialogOtherImageFragment 생성할 때, 콜백 넘겨주기
    // 이미지 버튼 부분에 있는 constraintLayout 누르면 바텀 다이얼로그 뜨도록 리스너 달기
    private fun setChooseCardListener() {
        binding.clOthercardwriteImg.setOnClickListener {
            val bottomDialogOtherImageFragment: BottomDialogOtherImageFragment =
                BottomDialogOtherImageFragment {
                    // image 선택 dialog에서 심볼이 하나라도 선택이 되어서 완료 버튼을 누르면 dialog가 닫히면 실행되는 함수
                    // 바로 갤러리 접근을 누른다면 이것이 실행되지 않으므로 symbolId는 초기값인 null일 것
                    var img_index: Int = GALLERY
                    when (it) {
                        // 심볼이 하나라도 선택이 되어서 dialog가 닫히면
                        GALLERY -> {  // 일 경우가 없을 듯
                            Toast.makeText(this, "GALLERY", Toast.LENGTH_SHORT).show()
                        }
                        SYMBOL_0 -> {
                            img_index = R.drawable.ic_symbol_cardyou_0
                            symbolId = SYMBOL_0
                        }
                        SYMBOL_1 -> {
                            img_index = R.drawable.ic_symbol_cardyou_1
                            symbolId = SYMBOL_1
                        }
                        SYMBOL_2 -> {
                            img_index = R.drawable.ic_symbol_cardyou_2
                            symbolId = SYMBOL_2
                        }
                        SYMBOL_3 -> {
                            img_index = R.drawable.ic_symbol_cardyou_3
                            symbolId = SYMBOL_3
                        }
                        SYMBOL_4 -> {
                            img_index = R.drawable.ic_symbol_cardyou_4
                            symbolId = SYMBOL_4
                        }
                    }
                    with(binding) {
                        // 이미지 뷰 보이도록
                        ivOthercardwriteGalleryImg.visibility = View.VISIBLE
                        // 각 심볼 이미지 띄워주기
                        ivOthercardwriteGalleryImg.setImageResource(img_index)

                        ifChooseImg = true
                        checkCompleteBtnClickable()
                        clOthercardwriteImg.visibility = View.INVISIBLE
                    }
                }
            bottomDialogOtherImageFragment.show(supportFragmentManager, bottomDialogOtherImageFragment.tag)
        }
    }


    // 카드나 만들기 버튼 눌렀을 때,
    // 1. 서버 통신해서 postCreateCardMe 호출해서 서버에 data 전달
    // 2. OtherCardCreateCompleteActivity로 인텐트로 이동
    private fun makeCardListener() {
        binding.btnOthercardwriteComplete.setOnClickListener {
            // 카드나 만들기 버튼을 눌렀을 때,
            // 1. 서버로 title, content, symbolId, uri 전송

            // MainActivity -> OtherCardCreateActivity -> 지금까지 올 때, id값, relation을 넘겨줬을 거임
            val relation = intent.getStringExtra("relation")
            val userId = intent.getIntExtra("id", 0)

            // symbolId - 카드 이미지 심볼 id, 이미지가 있는 경우 null을 보내주면 됨
            val body = RequestCreateCardYouData(
                binding.etOthercardwriteKeyword.text.toString(), // title
                binding.etOthercardwriteDetail.text.toString(), // content
                relation!!, // relation
                symbolId // 갤러리 이미지를 선택했다면 dialog 완료 버튼을 누르지 않았을 테니까 null 값일 것임
            ).toRequestBody()

            if(uri == null){ // 심볼 선택 시
                lifecycleScope.launch(Dispatchers.IO) {
                    runCatching { ApiService.cardService.postCreateCardYou(userId, body, null) }
                        .onSuccess { Log.d("카드나 작성 성공", it.message) }
                        .onFailure { Log.d("카드나 작성 실패", it.message!!)}
                }
            }
            else{ // 이미지 선택 시
                CoroutineScope(Dispatchers.IO).launch {
                    runCatching { ApiService.cardService.postCreateCardYou(userId, body, makeUriToFile()) }
                        .onSuccess { Log.d("카드나 작성 성공", it.message) }
                        .onFailure { Log.d("카드나 작성 실패", it.message!!)
                            it.printStackTrace() }
                }
            }

            // 2. otherCardCreateCompleteActivity로 인텐트로 이동
            val intent = Intent(this, OtherCardCreateCompleteActivity::class.java)
            startActivity(intent)
        }
    }


    // uri를 서버에 보내기 위해 멀티파트로 바꿔주는 함수
    private fun makeUriToFile(): MultipartBody.Part {
        val options = BitmapFactory.Options()
        val inputStream: InputStream = requireNotNull(contentResolver.openInputStream(uri!!))
        val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap!!.compress(Bitmap.CompressFormat.PNG, 20, byteArrayOutputStream)
        val fileBody = RequestBody.create(
            MediaType.parse("image/png"),
            byteArrayOutputStream.toByteArray()
        )

        val part = MultipartBody.Part.createFormData(
            "image",
            File("${uri.toString()}.png").name,
            fileBody
        )

        return part
    }


    // gallery access
    fun checkPermission() {
        val cameraPermission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        if (cameraPermission == PackageManager.PERMISSION_GRANTED) {
            //프로그램 진행
            startProcess()
        } else {
            //권한요청
            requestPermission()
        }
    }

    private fun startProcess() {
        val intent = Intent().apply {
            setType("image/*")
            setAction(Intent.ACTION_GET_CONTENT)
        }
        getResultText.launch(intent)
    }

    val getResultText = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
    { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            uri = result.data?.data  //Intent를 반환->Intent에서 Uri로 get하기

            // imageView는 보이도록
            binding.ivOthercardwriteGalleryImg.visibility = View.VISIBLE
            binding.clOthercardwriteImg.visibility =
                View.INVISIBLE // 이제 INVISIBLE이니까 한번 이미지 선택하면 다시 선택불가
            Glide.with(this).load(uri).into(binding.ivOthercardwriteGalleryImg)
            ifChooseImg = true
            checkCompleteBtnClickable()
        }
        //else if (result.resultCode == Activity.RESULT_CANCELED) {} =>Activity.RESULT_CANCELED일때 처리코드가 필요하다면
    }

    private fun requestPermission() {
        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission())
        { isGranted: Boolean ->
            when (isGranted) {
                true -> startProcess()
                false -> shortToast("갤러리 권한을 허용해주세요.")
            }
        }

    companion object {
        const val SYMBOL_0 = 0
        const val SYMBOL_1 = 1
        const val SYMBOL_2 = 2
        const val SYMBOL_3 = 3
        const val SYMBOL_4 = 4
        const val GALLERY = 5

        const val CARD_ME = 6
        const val CARD_YOU = 7
    }
}