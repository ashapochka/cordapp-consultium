package com.consultium.flows

import com.consultium.states.AgreementState
import com.consultium.states.ProposalState
import net.corda.core.node.services.queryBy
import net.corda.testing.internal.chooseIdentity
import org.junit.Test
import kotlin.test.assertEquals

class ProposalAcceptFlowTests: FlowTestsBase() {
    @Test
    fun `acceptance flow consumes the proposals in both nodes' vaults and replaces them with equivalent agreements`() {
        testAcceptance()
    }

    private fun testAcceptance() {
        val budget = 10000
        val upfrontPayPercentage = 50
        val scopeOfWork = "Technical debt assessment for 1 week"
        val orgA = a.info.chooseIdentity()
        val orgB = b.info.chooseIdentity()

        val proposalId = nodeACreatesProposal(
            budget, upfrontPayPercentage, scopeOfWork, orgB
        )
        nodeBAcceptsProposal(proposalId)

        for (node in listOf(a, b)) {
            node.transaction {
                val proposals = node.services.vaultService.queryBy<ProposalState>().states
                assertEquals(0, proposals.size)

                val agreements = node.services.vaultService.queryBy<AgreementState>().states
                assertEquals(1, agreements.size)
                val agreement = agreements.single().state.data

                assertEquals(budget, agreement.sow.budget)
                assertEquals(upfrontPayPercentage, agreement.sow.upfrontPayPercentage)
                assertEquals(scopeOfWork, agreement.sow.scopeOfWork)
                assertEquals(orgA, agreement.sow.consultancy)
                assertEquals(orgB, agreement.sow.client)
            }
        }
    }
}
