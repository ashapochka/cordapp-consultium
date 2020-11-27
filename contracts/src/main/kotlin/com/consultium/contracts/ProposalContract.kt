package com.consultium.contracts

import com.consultium.states.ProposalState
import net.corda.core.contracts.Contract
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
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
        val cmd = tx.commands.requireSingleCommand<Commands>()

        when (cmd.value) {
            is Commands.Create -> requireThat {
                "Inputs are empty" using (tx.inputStates.isEmpty())
                "There is a single proposal as an output" using (
                        tx.outputsOfType<ProposalState>().size == 1)

                val proposal = tx.outputsOfType<ProposalState>().single()

                "The consultancy is a required signer" using (
                        cmd.signers.contains(proposal.sow.consultancy.owningKey))
                "The client is a required signer" using (
                        cmd.signers.contains(proposal.sow.client.owningKey))
                "The budget amount is >= 0" using (proposal.sow.budget >= 0)
                "The upfront payment % is between 0 and 100" using (
                        proposal.sow.upfrontPayPercentage in 0..100)
                "The scope of work is not blank" using (proposal.sow.scopeOfWork.isNotBlank())
                "The consultancy and the client are different parties" using (
                        proposal.sow.consultancy != proposal.sow.client)
            }

            is Commands.Accept -> requireThat {
                "Single proposal must be accepted at a time" using (
                        tx.inputsOfType<ProposalState>().size == 1)

            }

            is Commands.Update -> requireThat { }
        }
    }

    // Used to indicate the transaction's intent.
    sealed class Commands : TypeOnlyCommandData() {
        class Create : Commands()
        class Accept : Commands()
        class Update : Commands()
    }
}
