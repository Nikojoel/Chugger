package com.example.chugger.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.example.chugger.R
import com.example.chugger.database.DbUserModel
import com.example.chugger.database.User
import kotlinx.android.synthetic.main.fragment_edit_user.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditUserFragment : Fragment() {
    companion object {
        private lateinit var time: String
        private lateinit var city: String
        fun newInstance(time: String, city: String?, alt: String): EditUserFragment {
            this.time = time
            if (city != null) {
                this.city = city
            } else {
                this.city = alt
            }
            return EditUserFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_edit_user, container, false)
        val ump = ViewModelProvider(this).get(DbUserModel::class.java)
        view.addUserButton.setOnClickListener {
            GlobalScope.launch {
                var name = view.editUserName.text.toString()
                if (name.isEmpty()) {
                    name = getString(R.string.noNameText)
                }
                val user = User(0, name, time, city)
                ump.insertNew(user)
                withContext(Dispatchers.Main) {
                    Toast.makeText(activity, getString(R.string.savedString), Toast.LENGTH_SHORT).show()
                    activity?.onBackPressed()
                }
            }
        }
        return view
    }
}