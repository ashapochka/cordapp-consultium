package com.consultium.flows

import co.paralleluniverse.fibers.Suspendable
import com.consultium.contracts.ProposalContract
import com.consultium.states.AgreementState
import com.consultium.states.ProposalState
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

object ProposalAcceptFlow {
    @InitiatingFlow
    @StartableByRPC
    class Initiator(private val proposalId: UniqueIdentifier) : FlowLogic<Unit>() {
        override val progressTracker = ProgressTracker()

        @Suspendable
        override fun call() {
            // Retrieving the Proposal input from the vault.
            val proposalInputCriteria = QueryCriteria.LinearStateQueryCriteria(
                linearId = listOf(proposalId)
            )
            val proposalInputStateAndRef = serviceHub.vaultService.queryBy<ProposalState>(
                proposalInputCriteria
            ).states.single()
            val proposal = proposalInputStateAndRef.state.data

            if (ourIdentity != proposal.sow.client) {
                throw FlowException("Only the client can initiate proposal acceptance.")
            }

            // Creating the Agreement output. The SOW terms remain the same
            val agreement = AgreementState(
                sow = proposal.sow, linearId = proposal.linearId
            )

            // Creating the Accept command. Both consultancy and client need to sign
            val requiredSigners = listOf(
                proposal.sow.consultancy.owningKey, proposal.sow.client.owningKey
            )
            val command = Command(ProposalContract.Commands.Accept(), requiredSigners)

            // Building the transaction.
            // Notary should be the same as in prior proposal transactions
            val notary = proposalInputStateAndRef.state.notary
            val txBuilder = TransactionBuilder(notary)
            // proposal => agreement
            txBuilder.addInputState(proposalInputStateAndRef)
            txBuilder.addOutputState(agreement, ProposalContract.ID)
            txBuilder.addCommand(command)

            // Signing the transaction ourselves (client).
            val partStx = serviceHub.signInitialTransaction(txBuilder)

            // Gathering the counterparty's (consultancy) signature.
            val counterparty = proposal.sow.consultancy
            val counterpartySession = initiateFlow(counterparty)
            val fullyStx = subFlow(CollectSignaturesFlow(partStx, listOf(counterpartySession)))

            // Finalising the transaction.
            subFlow(FinalityFlow(fullyStx, listOf(counterpartySession)))
        }
    }

    @InitiatedBy(Initiator::class)
    class Responder(private val session: FlowSession) : FlowLogic<Unit>() {
        @Suspendable
        override fun call() {
            val signTxFlow = object : SignTransactionFlow(session) {
                // check the responder is a client
                override fun checkTransaction(stx: SignedTransaction) {
                    val ledgerTx = stx.toLedgerTransaction(serviceHub, false)
                    val consultancy = ledgerTx.inputsOfType<ProposalState>().single().sow.consultancy
                    if (ourIdentity != consultancy) {
                        throw FlowException("Only the consultancy can confirm proposal acceptance.")
                    }
                }
            }

            // first, sign the transaction, if the responder is a client
            val txId = subFlow(signTxFlow).id

            // secondly, finalize the transaction from the responder's side
            subFlow(ReceiveFinalityFlow(session, txId))
        }
    }
}
