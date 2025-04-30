package com.paperpig.maimaidata.ui.rating

import android.content.Context
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.DialogProberUpdateBinding

class ProberUpdateDialog(context: Context) :
    BottomSheetDialog(context, R.style.Theme_Material3_Light_BottomSheetDialog) {
    private val binding: DialogProberUpdateBinding =
        DialogProberUpdateBinding.inflate(layoutInflater)

    init {
        setContentView(binding.root)
        setCanceledOnTouchOutside(true)
        setCancelable(true)
    }


    fun clearText() {
        binding.proberUpdateStatusText.text = ""
    }

    fun appendText(text: String) {
        binding.proberUpdateStatusText.append(text)
    }

    override fun show() {
        super.show()
        binding.proberUpdateStatusScroll.post {
            binding.proberUpdateStatusScroll.scrollTo(0, binding.proberUpdateStatusText.bottom)
        }
    }


    override fun onStart() {
        super.onStart()
        val window = window
        if (window != null) {
            val layoutParams = window.attributes
            layoutParams.width = context.resources.displayMetrics.widthPixels
            window.attributes = layoutParams
        }
    }
}