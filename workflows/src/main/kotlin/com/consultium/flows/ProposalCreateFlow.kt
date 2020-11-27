package com.consultium.flows

import co.paralleluniverse.fibers.Suspendable
import com.consultium.contracts.ProposalContract
import com.consultium.states.AgreementState
import com.consultium.states.ProposalState
import com.consultium.states.StatementOfWork
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

object ProposalCreateFlow {
    @InitiatingFlow
    @StartableByRPC
    class Initiator(
        private val budget: Int,
        private val upfrontPayPercentage: Int,
        private val scopeOfWork: String,
        private val client: Party) : FlowLogic<UniqueIdentifier>() {

        override val progressTracker = ProgressTracker()

        @Suspendable
        override fun call(): UniqueIdentifier {
            // Creating the Proposal output.
            val sow = StatementOfWork(
                budget = budget,
                upfrontPayPercentage = upfrontPayPercentage,
                scopeOfWork = scopeOfWork,
                consultancy = ourIdentity,
                client = client
            )
            val proposal = ProposalState(sow)

            // Creating the Create command. Both consultancy and client need to sign
            val requiredSigners = listOf(
                sow.consultancy.owningKey, sow.client.owningKey
            )
            val command = Command(ProposalContract.Commands.Create(), requiredSigners)

            // Building the transaction.
            // Any notary will do for this demo
            val notary = serviceHub.networkMapCache.notaryIdentities.single()
            val txBuilder = TransactionBuilder(notary)
            txBuilder.addOutputState(proposal, ProposalContract.ID)
            txBuilder.addCommand(command)

            // Signing the transaction ourselves (consultancy).
            val partStx = serviceHub.signInitialTransaction(txBuilder)

            // Gathering the counterparty's (client) signature.
            val counterparty = proposal.sow.client
            val counterpartySession = initiateFlow(counterparty)
            val fullyStx = subFlow(CollectSignaturesFlow(partStx, listOf(counterpartySession)))

            // Finalising the transaction.
            val finalizedTx = subFlow(FinalityFlow(fullyStx, listOf(counterpartySession)))
            return finalizedTx.tx.outputsOfType<ProposalState>().single().linearId
        }
    }

    @InitiatedBy(Initiator::class)
    class Responder(private val session: FlowSession) : FlowLogic<Unit>() {
        @Suspendable
        override fun call() {
            val signTxFlow = object : SignTransactionFlow(session) {
                override fun checkTransaction(stx: SignedTransaction) {
                    // nothing to check for the proposal reception
                }
            }

            // first, sign the transaction
            val txId = subFlow(signTxFlow).id

            // secondly, finalize the transaction from the responder's side
            subFlow(ReceiveFinalityFlow(session, txId))
        }
    }
}
