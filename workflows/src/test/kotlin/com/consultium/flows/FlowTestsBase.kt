package com.consultium.flows

import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before

abstract class FlowTestsBase {
    protected lateinit var network: MockNetwork
    protected lateinit var a: StartedMockNode
    protected lateinit var b: StartedMockNode

    @Before
    fun setup() {
        network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
                TestCordapp.findCordapp("com.consultium.flows"),
                TestCordapp.findCordapp("com.consultium.contracts"))))
        a = network.createPartyNode()
        b = network.createPartyNode()

        val responseFlows = listOf(ProposalCreateFlow.Responder::class.java, ProposalAcceptFlow.Responder::class.java)
        listOf(a, b).forEach {
            for (flow in responseFlows) {
                it.registerInitiatedFlow(flow)
            }
        }

        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    fun nodeACreatesProposal(
        budget: Int, upfrontPayPercentage: Int, scopeOfWork: String, client: Party
    ): UniqueIdentifier {
        val flow = ProposalCreateFlow.Initiator(
            budget = budget,
            upfrontPayPercentage = upfrontPayPercentage,
            scopeOfWork = scopeOfWork,
            client = client
        )
        val future = a.startFlow(flow)
        network.runNetwork()
        return future.get()
    }

    fun nodeBAcceptsProposal(proposalId: UniqueIdentifier) {
        val flow = ProposalAcceptFlow.Initiator(proposalId)
        val future = b.startFlow(flow)
        network.runNetwork()
        future.get()

    }

//    fun nodeBModifiesProposal(proposalId: UniqueIdentifier, newAmount: Int) {
//        val flow = ModificationFlow.Initiator(proposalId, newAmount)
//        val future = b.startFlow(flow)
//        network.runNetwork()
//        future.get()
//    }
}
