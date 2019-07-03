package com.okta.browser.fragments

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {
    val hint = MutableLiveData<String>()
    val userAndPassword = MutableLiveData<Pair<String, String>>()
}