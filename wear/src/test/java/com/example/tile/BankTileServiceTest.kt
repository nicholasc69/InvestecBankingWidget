package com.example.tile

import com.example.data.model.BankAccountEntity
import com.example.data.model.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class BankTileServiceTest {

    @Test
    fun testBuildTileWithAccount() {
        val service = BankTileService()
        val account = BankAccountEntity(
            accountId = "acc-1",
            accountNumber = "1234567890",
            accountName = "Private Banking Account",
            referenceName = "Private Banking",
            productName = "Private Bank Account",
            kycCompliant = true,
            profileId = "prof-1",
            profileName = "John Doe",
            currency = "ZAR",
            availableBalance = 54321.00,
            currentBalance = 54321.00,
            lastUpdated = System.currentTimeMillis()
        )
        val tx = TransactionEntity(
            id = 1L,
            accountId = "acc-1",
            type = "DEBIT",
            transactionType = "Card Purchase",
            status = "POSTED",
            description = "Coffee Shop",
            amount = 150.00,
            runningBalance = 54171.00,
            postingDate = "2026-08-01",
            transactionDate = "2026-08-01",
            uuid = "uuid-1"
        )

        val tile = service.buildTile(account, 1, tx)

        assertNotNull(tile)
        assertEquals("1", tile.resourcesVersion)
        assertNotNull(tile.tileTimeline)
        assertEquals(1, tile.tileTimeline?.timelineEntries?.size)
    }

    @Test
    fun testBuildTileEmpty() {
        val service = BankTileService()
        val tile = service.buildTile(null, 0, null)

        assertNotNull(tile)
        assertEquals("1", tile.resourcesVersion)
        assertNotNull(tile.tileTimeline)
        assertEquals(1, tile.tileTimeline?.timelineEntries?.size)
    }
}
