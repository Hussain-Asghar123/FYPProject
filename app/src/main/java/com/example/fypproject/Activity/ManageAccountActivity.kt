package com.example.fypproject.Activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fypproject.Adapter.AccountAdapter
import com.example.fypproject.DTO.AccountResponse
import com.example.fypproject.Network.ApiClient.api
import com.example.fypproject.R
import com.example.fypproject.databinding.ActivityManageAccountBinding
import kotlinx.coroutines.launch

class ManageAccountActivity : AppCompatActivity() {
    private lateinit var binding: ActivityManageAccountBinding
    private lateinit var adapter: AccountAdapter
    private val fullList=mutableListOf<AccountResponse>()
    private val filteredList=mutableListOf<AccountResponse>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityManageAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener {
            finish()
        }
        setupRecycler()
        setupSearch()
        setupAddButton()
        getAllAccounts()
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressOverlay.visibility = if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
        binding.btnAdd.isEnabled = !isLoading
        binding.btnBack.isEnabled = !isLoading
        binding.search.isEnabled = !isLoading
    }

    private fun setupRecycler(){
        adapter=AccountAdapter(
            filteredList,
            onEdit={openUpdate(it)},
            onDelete={showDeleteDialog(it)},
            onClick={openUpdate(it)}
        )
        binding.accountRecycler.adapter=adapter
        binding.accountRecycler.layoutManager= LinearLayoutManager(this)
    }
    private fun getAllAccounts(){
        lifecycleScope.launch {
            setLoading(true)
            try {
                val response = api.getAllAccounts()
                if(response.isSuccessful && response.body() != null){
                    fullList.clear()
                    fullList.addAll(response.body()!!)
                    filteredList.clear()
                    filteredList.addAll(fullList)
                    adapter.notifyDataSetChanged()
                }
                checkEmptyState()
            } finally {
                setLoading(false)
            }
        }
    }

    private fun checkEmptyState() {
        val isEmpty = filteredList.isEmpty()
        binding.accountRecycler.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.tvEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    private fun setupSearch(){
        binding.search.addTextChangedListener{
        val query = it.toString().lowercase()
        filteredList.clear()
        filteredList.addAll(
            fullList.filter {
                acc ->
                acc.name.lowercase().contains(query) || acc.username.lowercase().contains(query)
            }
        )
        adapter.notifyDataSetChanged()
        checkEmptyState()
        }
    }
    private fun openUpdate(account: AccountResponse) {
        val intent = Intent(this, UpdateAccountActivity::class.java)
        intent.putExtra("id", account.id)
        intent.putExtra("name", account.name)
        intent.putExtra("username", account.username)
        intent.putExtra("password", account.password)
        intent.putExtra("role", account.role)
        startActivity(intent)
    }
    private fun setupAddButton() {
        binding.btnAdd.setOnClickListener {
            startActivity(Intent(this, AddAccountActivity::class.java))
        }
    }
    private fun showDeleteDialog(account: AccountResponse) {
        AlertDialog.Builder(this)
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to delete this account?")
            .setPositiveButton("Delete") { _, _ ->
                deleteAccount(account.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun deleteAccount(id: Long) {
        lifecycleScope.launch {
            setLoading(true)
            try {
                api.deleteAccount(id.toInt())
                val response = api.getAllAccounts()
                if (response.isSuccessful && response.body() != null) {
                    fullList.clear()
                    fullList.addAll(response.body()!!)
                    filteredList.clear()
                    filteredList.addAll(fullList)
                    adapter.notifyDataSetChanged()
                }
                checkEmptyState()
            } finally {
                setLoading(false)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        getAllAccounts()

    }




}