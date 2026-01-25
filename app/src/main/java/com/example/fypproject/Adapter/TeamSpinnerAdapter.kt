package com.example.fypproject.Adapter

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.fypproject.DTO.TeamDTO

class TeamSpinnerAdapter(
    context: Context,
    private val teamList: List<TeamDTO>
    ) : ArrayAdapter<TeamDTO>(context, android.R.layout.simple_spinner_item, teamList) {
    init{
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

    }
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val label = super.getView(position, convertView, parent) as TextView
        label.text = teamList[position].name
        return label
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val label = super.getDropDownView(position, convertView, parent) as TextView
        label.text = teamList[position].name
        label.setPadding(16, 16, 16, 16)
        return label
    }
}


