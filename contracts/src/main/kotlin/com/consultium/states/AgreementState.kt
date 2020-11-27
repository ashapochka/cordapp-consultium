package com.consultium.states

import com.consultium.contracts.ProposalContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier

@BelongsToContract(ProposalContract::class)
data class AgreementState(
    val sow: StatementOfWork,
    override val linearId: UniqueIdentifier = UniqueIdentifier()
) : LinearState {
    override val participants = listOf(
        sow.consultancy, sow.client
    )
}
