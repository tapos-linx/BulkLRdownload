package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.LocationRepository
import com.example.data.MouzaInfo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LocationRepositoryTest {

    private lateinit var context: Context
    private lateinit var locationRepo: LocationRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        locationRepo = LocationRepository(context)
    }

    @Test
    fun `getMouzasForUpazila returns all mouzas and never restricts to 4 or 5`() {
        val testUpazilas = listOf("Savar", "Tejgaon", "Mirpur", "Keraniganj", "Dhamrai", "Amtali", "Bogra Sadar", "Hathazari", "Patiya", "Sonargaon")

        for (upazila in testUpazilas) {
            val mouzas = locationRepo.getMouzasForUpazila(upazila)
            assertNotNull("Mouzas for $upazila should not be null", mouzas)
            assertTrue(
                "Upazila $upazila should return comprehensive mouzas from bd_locations.json (found ${mouzas.size}), never only 4 or 5",
                mouzas.size >= 10
            )

            // Verify all items have non-blank name and JL number
            for (mouza in mouzas) {
                assertTrue("Mouza name should be non-blank in $upazila", mouza.name.isNotBlank())
                assertTrue("Mouza JL No should be non-blank in $upazila", mouza.jlNo.isNotBlank())
            }
        }
    }

    @Test
    fun `getMouzasForUpazila with district filter correctly returns all mouzas from bd_locations dataset`() {
        val barishalMouzas = locationRepo.getMouzasForUpazila(
            upazilaName = "Barishal Sadar",
            districtName = "Barishal",
            divisionName = "Barishal"
        )
        assertTrue(barishalMouzas.size >= 12)
        assertTrue(barishalMouzas.any { it.name == "Band Road" && it.jlNo == "JL 01" })

        val savarMouzas = locationRepo.getMouzasForUpazila(
            upazilaName = "Savar",
            districtName = "Dhaka",
            divisionName = "Dhaka"
        )
        assertTrue(savarMouzas.size >= 16)
        assertTrue(savarMouzas.any { it.name == "Ashulia" })
    }

    @Test
    fun `custom mouzas are preserved and added to all existing mouzas for upazila`() {
        val upazila = "Savar"
        val initialMouzas = locationRepo.getMouzasForUpazila(upazila)
        val initialCount = initialMouzas.size

        val custom = MouzaInfo("New Custom Mouza", "নতুন কাস্টম মৌজা", "JL 999")
        locationRepo.addCustomMouza(upazila, custom)

        val updatedMouzas = locationRepo.getMouzasForUpazila(upazila)
        assertTrue(updatedMouzas.size > initialCount)
        assertTrue(updatedMouzas.any { it.name == "New Custom Mouza" && it.jlNo == "JL 999" })
    }
}
