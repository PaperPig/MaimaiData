package com.paperpig.maimaidata.ui.checklist

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.model.Version


class VersionArrayAdapter(context: Context?, resource: Int, val list: List<Version>?) :
    ArrayAdapter<Version>(context!!, resource, list!!) {


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
                LayoutInflater.from(context).inflate(R.layout.item_spinner_version, parent, false)

        }
        val versionImage = view!!.findViewById<ImageView>(R.id.versionImage)
        val versionName = view.findViewById<TextView>(R.id.versionName)
        Glide.with(context).load(getItem(position)?.res).into(versionImage)
        versionName.text = getItem(position)?.versionName
        return view
    }


}
