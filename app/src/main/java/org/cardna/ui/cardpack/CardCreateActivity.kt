package org.cardna.ui.cardpack

import android.Manifest
import android.app.Activity
import android.content.ContentResolver
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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.cardna.R
import org.cardna.base.baseutil.BaseViewUtil
import org.cardna.data.remote.api.ApiService.cardService
import org.cardna.data.remote.model.cardpack.RequestCreateCardMeData
import org.cardna.databinding.ActivityCardCreateBinding
import org.cardna.util.initRootClickEvent
import org.cardna.util.shortToast
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

class CardCreateActivity :
    BaseViewUtil.BaseAppCompatActivity<ActivityCardCreateBinding>(R.layout.activity_card_create) {
    private var symbolId: Int? = null // 이미지가 있는 경우 null로 보내줘야 함
    private var uri: Uri? = null // 이를 multipart로 변환해서 서버에 img로 보내줄 것임
    private var ifChooseImg: Boolean = false // 갤러리 이미지를 선택했는지 확인해주는 변수 => 나중에 버튼 enable 할때 사용

    private var status:Int = 0
    private var success:Boolean ? = null
    private var message:String ? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
    }

    override fun initView() {
        checkEditTextLength()
        setChooseCardListener()
        makeCardListener()
        initRootClickEvent(binding.tvCardcreateTitle)
        initRootClickEvent(binding.svCardcreateTop)
        // initRootClickEvent()
    }


    //로그인 화면 전체 아무곳이나 눌러도 키보드 내려가도록
    /*   private fun initRootClickEvent() {
           binding.tvCardcreateTitle.setOnClickListener {
               ViewCompat.getWindowInsetsController(it)?.hide(WindowInsetsCompat.Type.ime())
           }
       }*/


    // editText 글자 수에 따라 글자 수 업데이트, 버튼 선택가능하도록
    private fun checkEditTextLength() {
        binding.ivCardcreateGalleryImg.clipToOutline = true // imageView 모서리 둥글게
        binding.btnCardcreateComplete.isClickable = false;

        binding.etCardcreateKeyword.addTextChangedListener {
            checkCompleteBtnClickable()
            binding.tvCardcreateCntKeyword.text = "${binding.etCardcreateKeyword.length()}/14"
        }

        binding.etCardcreateDetail.addTextChangedListener {
            checkCompleteBtnClickable()
            binding.tvCardcreateCntDetail.text = "${binding.etCardcreateDetail.length()}/200"
        }
    }

    private fun checkCompleteBtnClickable() {
        if (binding.etCardcreateKeyword.length() > 0 && binding.etCardcreateDetail.length() > 0 && ifChooseImg) {
            with(binding) {
                btnCardcreateComplete.isClickable = true
                btnCardcreateComplete.isEnabled = true
                btnCardcreateComplete.setTextColor(resources.getColor(R.color.black))
                btnCardcreateComplete.setBackgroundResource(R.drawable.background_btn_cardme_abled)
            }
        } else {
            with(binding) {
                btnCardcreateComplete.isClickable = false;
                btnCardcreateComplete.isEnabled = false;
                btnCardcreateComplete.setTextColor(resources.getColor(R.color.white_2))
                btnCardcreateComplete.setBackgroundResource(R.drawable.background_btn_card_disabled)
            }
        }
    }

    private fun setChooseCardListener() {
        binding.clCardcreateImg.setOnClickListener {
            val bottomDialogImageFragment: BottomDialogImageFragment = BottomDialogImageFragment {
                // image 선택 dialog에서 심볼이 하나라도 선택이 되어서 완료 버튼을 누르면 dialog가 닫히면 실행되는 함수
                // 바로 갤러리 접근을 누른다면 이것이 실행되지 않으므로 symbolId는 초기값인 null일 것
                var img_index: Int = GALLERY
                when (it) {
                    GALLERY -> {  // 일 경우가 없을 듯
                    }
                    SYMBOL_0 -> {
                        img_index = R.drawable.ic_symbol_cardme_0
                        symbolId = SYMBOL_0
                    }
                    SYMBOL_1 -> {
                        img_index = R.drawable.ic_symbol_cardme_1
                        symbolId = SYMBOL_1
                    }
                    SYMBOL_2 -> {
                        img_index = R.drawable.ic_symbol_cardme_2
                        symbolId = SYMBOL_2
                    }
                    SYMBOL_3 -> {
                        img_index = R.drawable.ic_symbol_cardme_3
                        symbolId = SYMBOL_3
                    }
                    SYMBOL_4 -> {
                        img_index = R.drawable.ic_symbol_cardme_4
                        symbolId = SYMBOL_4
                    }
                }
                with(binding) {
                    ivCardcreateGalleryImg.visibility = View.VISIBLE
                    ivCardcreateGalleryImg.setImageResource(img_index)

                    ifChooseImg = true
                    checkCompleteBtnClickable()
                    // 각 심볼 이미지 띄워주기
                    clCardcreateImg.visibility = View.INVISIBLE // visibility말고 background를 검정으로 바꾸면 계속 선택가능하지 않을까
                }
            }
            bottomDialogImageFragment.show(supportFragmentManager, bottomDialogImageFragment.tag)
        }
    }

    private fun resToUri(resId: Int): Uri =
        Uri.Builder().scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(resources.getResourcePackageName(resId))
            .appendPath(resources.getResourceTypeName(resId))
            .appendPath(resources.getResourceEntryName(resId))
            .build()


    // 카드나 만들기 버튼 눌렀을 때,
    // 1. 서버 통신해서 postCreateCardMe 호출해서 서버에 data 전달
    // 2. cardCreateCompleteActivity로 인텐트로 이동
    private fun makeCardListener() {
        binding.btnCardcreateComplete.setOnClickListener {
            // 카드나 만들기 버튼을 눌렀을 때
            // 1. 서버로 title, content, symbolId, uri 전송
            // symbolId - 카드 이미지 심볼 id, 이미지가 있는 경우 null을 보내주면 됨
            val body = RequestCreateCardMeData(
                binding.etCardcreateKeyword.text.toString(),
                binding.etCardcreateDetail.text.toString(),
                symbolId // 갤러리 이미지를 선택했다면 dialog 완료 버튼을 누르지 않았을 테니까 null 값일 것임
            ).toRequestBody()

            if(uri == null){ // 심볼 선택
                lifecycleScope.launch(Dispatchers.IO) {
                    runCatching { cardService.postCreateCardMe(body, null) }
                        .onSuccess { Log.d("카드나 작성 성공", it.message) }
                        .onFailure { Log.d("카드나 작성 실패", it.message!!)}
                }
            }
            else{ // 이미지 선택
                CoroutineScope(Dispatchers.IO).launch {
                    runCatching { cardService.postCreateCardMe(body, makeUriToFile()) }
                        .onSuccess { Log.d("카드나 작성 성공", it.message) }
                        .onFailure { Log.d("카드나 작성 실패", it.message!!)
                            it.printStackTrace() }
                }
            }

//            lifecycleScope.launch(Dispatchers.IO) {
//                runCatching { cardService.postCreateCardMe(body, makeUriToFile()) }
//                    .onSuccess {
//                        // Log.d("카드나 작성 성공", it.message)
//                    }
//                    .onFailure {
//                        it.printStackTrace()
//                    }
//            }

            // if(파일이 잘 들어갔을 때)
            // 2. cardCreateCompleteActivity로 인텐트로 이동

            val intent = Intent(this@CardCreateActivity, CardCreateCompleteActivity::class.java)
            intent.putExtra("meOrYou", CARD_ME) // 현재는 카드나 작성이므로 CARD_ME를 보내줌
            intent.putExtra("symbolId", symbolId) // 심볼 - 2, 갤러리 - null
            intent.putExtra("cardImg", uri.toString()) // 심볼 - null, 갤러리 - adflkadlfaf
            intent.putExtra("cardTitle", binding.etCardcreateKeyword.text.toString())

            startActivity(intent)
        }
    }


    // uri를 서버에 보내기 위해 멀티파트로 바꿔주는 함수
    private fun makeUriToFile(): MultipartBody.Part {
        val options = BitmapFactory.Options()
        val inputStream: InputStream =
            requireNotNull(contentResolver.openInputStream(uri!!)) // 여기서 문제 인가
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
            binding.ivCardcreateGalleryImg.visibility = View.VISIBLE
            binding.clCardcreateImg.visibility =
                View.INVISIBLE // 이제 INVISIBLE이니까 한번 이미지 선택하면 다시 선택불가
            Glide.with(this).load(uri).into(binding.ivCardcreateGalleryImg)
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