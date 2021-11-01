package com.paperpig.maimaidata.ui.nameplate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.paperpig.maimaidata.databinding.FragmentNamePlateBinding
import com.paperpig.maimaidata.model.Record
import com.paperpig.maimaidata.model.SongData
import com.paperpig.maimaidata.ui.BaseFragment

private const val ARG_PARAM1 = "songList"
private const val ARG_PARAM2 = "record"
private const val ARG_PARAM3 = "position"


class NamePlateFragment : BaseFragment<FragmentNamePlateBinding>() {
    private lateinit var binding: FragmentNamePlateBinding

    companion object {
        fun newInstance(songList: ArrayList<SongData>, record: ArrayList<Record>, position: Int) =
            NamePlateFragment().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList(ARG_PARAM1, songList)
                    putParcelableArrayList(ARG_PARAM2, record)
                    putInt(ARG_PARAM3, position)

                }
            }
    }

    override fun getViewBinding(container: ViewGroup?): FragmentNamePlateBinding {
        binding = FragmentNamePlateBinding.inflate(LayoutInflater.from(context), container, false)
        return binding
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val songList = arguments?.getParcelableArrayList<SongData>(ARG_PARAM1) ?: arrayListOf()
        val record = arguments?.getParcelableArrayList<Record>(ARG_PARAM2) ?: arrayListOf()
        val position = arguments?.getInt(ARG_PARAM3)

        binding.namePlateCheckRecyclerView.apply {
            val list = when (position) {
                0 -> record.filter { it.level_index == 3 }
                1 -> record.filter { it.level_index == 2 }
                2 -> record.filter { it.level_index == 1 }
                3 -> record.filter { it.level_index == 0 }
                else -> emptyList()
            }

            adapter = NamePlateCheckAdapter(songList, list)
            layoutManager = LinearLayoutManager(context)

        }
    }
}