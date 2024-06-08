package com.paperpig.maimaidata.ui.checklist

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.paperpig.maimaidata.R


class LevelArrayAdapter(context: Context?, resource: Int, val list: List<String>?) :
    ArrayAdapter<String>(context!!, resource, list!!) {


    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return initView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return initView(position, convertView, parent)
    }

    private fun initView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        if (view == null) {
            view =
                LayoutInflater.from(context).inflate(R.layout.item_spinner_level, parent, false)

        }
        val levelName = view!!.findViewById<TextView>(R.id.levelName)
        levelName.text = getItem(position)
        return view
    }


}
