package com.consultium.states

import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class StatementOfWork(
    val budget: Int,
    val upfrontPayPercentage: Int,
    val scopeOfWork: String,
    val consultancy: Party,
    val client: Party
)
