package com.paperpig.maimaidata.ui.maimaidxprober

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.paperpig.maimaidata.R

class AccountListBottomSheet(
    private val accounts: List<String>,
    private val onAccountSelected: (String) -> Unit,
    private val onAccountDeleted: (String) -> Unit
) : BottomSheetDialogFragment() {

    private var deleteMode = false
    private lateinit var adapter: AccountAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_account_list_bottom_sheet, container, false)
        val recyclerView = view.findViewById<RecyclerView>(R.id.accountRecyclerView)
        val deleteButton = view.findViewById<ImageButton>(R.id.deleteModeButton)
        val titleText = view.findViewById<TextView>(R.id.titleText)

        adapter = AccountAdapter(accounts,
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

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        deleteButton.setOnClickListener {
            deleteMode = !deleteMode
            titleText.text = if (deleteMode) "删除账号" else "选择账号"
        }

        return view
    }
}

