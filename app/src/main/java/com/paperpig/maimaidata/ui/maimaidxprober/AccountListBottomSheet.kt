package com.paperpig.maimaidata.ui.maimaidxprober

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.FragmentAccountListBottomSheetBinding

class AccountListBottomSheet(
    private val accounts: List<String>,
    private val onAccountSelected: (String) -> Unit,
    private val onAccountDeleted: (String) -> Unit
) : BottomSheetDialogFragment() {

    override fun getTheme(): Int {
        return R.style.Theme_Material3_Light_BottomSheetDialog
    }

    private var deleteMode = false
    private lateinit var binding: FragmentAccountListBottomSheetBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentAccountListBottomSheetBinding.inflate(layoutInflater, container, false)

        binding.accountRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = AccountAdapter(
                accounts,
                onItemClick = { selected ->
                    if (!deleteMode) {
                        onAccountSelected(selected)
                        dismiss()
                    } else {
                        onAccountDeleted(selected)
                        dismiss()
                    }
                },
            )
        }

        binding.deleteModeButton.setOnClickListener {
            deleteMode = !deleteMode
            binding.titleText.text = if (deleteMode) "删除账号" else "选择账号"
        }

        return binding.root
    }
}

