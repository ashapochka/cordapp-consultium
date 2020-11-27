package com.consultium.flows

import com.consultium.states.ProposalState
import net.corda.core.node.services.queryBy
import net.corda.testing.internal.chooseIdentity
import org.junit.Test
import kotlin.test.assertEquals

class ProposalCreateTests: FlowTestsBase() {

    @Test
    fun `proposal flow creates the correct proposals in both nodes' vaults`() {
        testProposal()
    }

    private fun testProposal() {
        val budget = 50000
        val upfrontPayPercentage = 50
        val scopeOfWork = "Assess technical debt in a product B"
        val orgA = a.info.chooseIdentity()
        val orgB = b.info.chooseIdentity()

        nodeACreatesProposal(budget, upfrontPayPercentage, scopeOfWork, orgB)

        for (node in listOf(a, b)) {
            node.transaction {
                val proposals = node.services.vaultService.queryBy<ProposalState>().states
                assertEquals(1, proposals.size)
                val proposal = proposals.single().state.data

                assertEquals(budget, proposal.sow.budget)
                assertEquals(upfrontPayPercentage, proposal.sow.upfrontPayPercentage)
                assertEquals(scopeOfWork, proposal.sow.scopeOfWork)
                assertEquals(orgA, proposal.sow.consultancy)
                assertEquals(orgB, proposal.sow.client)
            }
        }
    }
}
