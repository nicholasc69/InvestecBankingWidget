package com.example.data.ai

import com.example.data.repository.BankRepository
import com.google.ai.edge.litertlm.ToolSet
import com.squareup.moshi.Moshi

class BankingToolSet(val repository: BankRepository) : ToolSet {

    private val moshi = Moshi.Builder().build()

}