package com.consultium.contracts

import net.corda.core.contracts.Contract
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.transactions.LedgerTransaction

// ************
// * Contract *
// ************
class ProposalContract : Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "com.consultium.contracts.ProposalContract"
    }

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        // Verification logic goes here.
    }

    // Used to indicate the transaction's intent.
    sealed class Commands : TypeOnlyCommandData() {
        class Propose : Commands()
        class Accept: Commands()
        class Update: Commands()
    }
}
