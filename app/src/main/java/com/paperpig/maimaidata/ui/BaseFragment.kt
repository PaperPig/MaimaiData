package com.paperpig.maimaidata.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment

open class BaseFragment : Fragment() {


    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        super.onCreate(savedInstanceState)
    }

    fun hideKeyboard(view: View?) {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE)
        if (imm is InputMethodManager) {
            imm.hideSoftInputFromWindow(view?.windowToken, 0)
        }
    }


}