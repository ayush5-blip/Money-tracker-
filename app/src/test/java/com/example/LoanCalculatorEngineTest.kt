package com.example

import com.example.model.LoanCalculatorEngine
import com.example.model.LoanDisbursementInput
import com.example.model.LoanScheme
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class LoanCalculatorEngineTest {

    @Test
    fun purchaseSchemeIgnoresForcloseAndHasZeroNocHold() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.MARCH, 15, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val input = LoanDisbursementInput(
            customerName = "Rohan Mehta",
            scheme = LoanScheme.P,
            loanAmount = 500_000.0,
            loanInsuranceSk = 25_000.0,
            pf = 5_000.0,
            rto = 12_000.0,
            stp = 2_000.0,
            vc = 1_000.0,
            forcloseFc = 40_000.0, // must be ignored for Purchase (P)
            rtoCash = 8_000.0,
            reducingRateIt = 14.0,
            tenureMonths = 36,
            disbursementDateMillis = cal.timeInMillis
        )

        val result = LoanCalculatorEngine.computeSheet(input)

        assertEquals(525_000.0, result.totalLoan, 0.01)
        assertEquals(20_000.0, result.bankCharges, 0.01)
        assertEquals(480_000.0, result.disbursementAmount, 0.01)
        assertEquals(0.0, result.nocHold, 0.01)
        assertEquals(0.0, result.forcloseAmount, 0.01)
        assertEquals(480_000.0, result.firstPaymentByBank, 0.01)
        assertEquals(472_000.0, result.inHand, 0.01)
        assertEquals(7.0, result.flatRate, 0.001)
        assertEquals(7_000.0, result.perLacPerYear, 0.01)
        assertEquals(7_000.0 / 12.0, result.perMonth, 0.01)

        val expectedEmi = LoanCalculatorEngine.calculateReducingEmi(525_000.0, 14.0, 36)
        assertEquals(expectedEmi, result.emiReducing, 0.01)

        val startCal = Calendar.getInstance().apply { timeInMillis = result.emiStartDateMillis }
        assertEquals(2026, startCal.get(Calendar.YEAR))
        assertEquals(Calendar.APRIL, startCal.get(Calendar.MONTH))
        assertEquals(15, startCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun balanceTransferAppliesTenPercentNocHoldAndForclose() {
        val input = LoanDisbursementInput(
            customerName = "Anita Sharma",
            scheme = LoanScheme.BT,
            loanAmount = 400_000.0,
            loanInsuranceSk = 10_000.0,
            pf = 4_000.0,
            rto = 6_000.0,
            stp = 1_500.0,
            vc = 500.0,
            forcloseFc = 50_000.0,
            rtoCash = 5_000.0,
            reducingRateIt = 12.0,
            tenureMonths = 24
        )

        val result = LoanCalculatorEngine.computeSheet(input)

        assertEquals(410_000.0, result.totalLoan, 0.01)
        assertEquals(12_000.0, result.bankCharges, 0.01)
        assertEquals(388_000.0, result.disbursementAmount, 0.01)
        assertEquals(40_000.0, result.nocHold, 0.01)
        assertEquals(50_000.0, result.forcloseAmount, 0.01)
        assertEquals(298_000.0, result.firstPaymentByBank, 0.01)
        assertEquals(293_000.0, result.inHand, 0.01)
        assertEquals(6.0, result.flatRate, 0.001)
    }
}
