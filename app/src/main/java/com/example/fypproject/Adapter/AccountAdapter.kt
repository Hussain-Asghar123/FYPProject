package com.example.fypproject.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fypproject.DTO.AccountResponse
import com.example.fypproject.databinding.ItemAccountBinding

class AccountAdapter(
    private val list: List<AccountResponse>,
    private val onClick: (AccountResponse) -> Unit,
    private val onDelete: (AccountResponse) -> Unit,
    private val onEdit: (AccountResponse) -> Unit
) : RecyclerView.Adapter<AccountAdapter.ViewHolder>() {
    inner class ViewHolder(
        private val binding: ItemAccountBinding
    ):RecyclerView.ViewHolder(binding.root){

        fun bind(account: AccountResponse){
            binding.txtArid.text=account.username
            binding.txtName.text=account.name
            binding.btnEdit.setOnClickListener { onEdit(account) }
            binding.btnDelete.setOnClickListener { onDelete(account) }
            binding.root.setOnClickListener { onClick(account) }
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
       val binding=ItemAccountBinding.inflate(LayoutInflater.from(parent.context),parent,false)
       return ViewHolder(binding)
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(list[position])
    }
    override fun getItemCount(): Int {
        return list.size
    }

}