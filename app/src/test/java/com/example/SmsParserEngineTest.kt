package com.example

import com.example.model.PaymentInstrument
import com.example.model.TransactionType
import com.example.scanner.SmsParserEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SmsParserEngineTest {

    @Test
    fun parsesDebitCardExpenseSms() {
        val msg = "Chase: You spent $48.90 on your Debit Card ending in 4821 at Whole Foods Market on 11/24. Avl Bal: $1,240.10"
        val parsed = SmsParserEngine.parseMessage(msg, sender = "CHASE")
        assertNotNull(parsed)
        assertEquals(TransactionType.EXPENSE, parsed!!.type)
        assertEquals(PaymentInstrument.DEBIT_CARD, parsed.instrument)
        assertEquals(4890L, parsed.amountCents)
        assertEquals("USD", parsed.currency)
        assertEquals("4821", parsed.accountLast4)
        assertEquals("Chase", parsed.bankName)
        assertEquals("Groceries", parsed.category)
    }

    @Test
    fun parsesCreditCardExpenseSms() {
        val msg = "HDFC Bank: Rs. 1,299.00 spent on Credit Card XX9012 at Starbucks Coffee on 24-11-26. Avl Limit: Rs. 85,000"
        val parsed = SmsParserEngine.parseMessage(msg, sender = "AD-HDFCBK")
        assertNotNull(parsed)
        assertEquals(TransactionType.EXPENSE, parsed!!.type)
        assertEquals(PaymentInstrument.CREDIT_CARD, parsed.instrument)
        assertEquals(129900L, parsed.amountCents)
        assertEquals("INR", parsed.currency)
        assertEquals("9012", parsed.accountLast4)
        assertEquals("Food & Dining", parsed.category)
    }

    @Test
    fun parsesSalaryCreditIncomeSms() {
        val msg = "Your A/C XX3344 is credited with USD 3,250.00 towards Salary Deposit from Acme Corp. Avl Bal USD 5,410.22"
        val parsed = SmsParserEngine.parseMessage(msg, sender = "CITIBANK")
        assertNotNull(parsed)
        assertEquals(TransactionType.INCOME, parsed!!.type)
        assertEquals(PaymentInstrument.BANK_ACCOUNT, parsed.instrument)
        assertEquals(325000L, parsed.amountCents)
        assertEquals("Salary", parsed.category)
    }

    @Test
    fun ignoresOtpMessages() {
        val otp = "Your OTP for a card transaction of $250.00 at Amazon is 849201. Do not share with anyone."
        val parsed = SmsParserEngine.parseMessage(otp, sender = "CHASE")
        assertNull(parsed)
    }

    @Test
    fun parsesMultiMessageBatchBlock() {
        val block = """
            Your Debit Card XX1122 was charged $19.50 at Uber Trip.
            
            Your A/C XX1122 is credited by $75.00 for Refund from Amazon.
        """.trimIndent()
        val list = SmsParserEngine.parseBatchText(block)
        assertEquals(2, list.size)
        assertEquals(TransactionType.EXPENSE, list[0].type)
        assertEquals(TransactionType.INCOME, list[1].type)
    }
}
