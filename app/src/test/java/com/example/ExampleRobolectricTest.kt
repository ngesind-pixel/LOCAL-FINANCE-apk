package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.auth.FirebaseAuthManager
import com.example.data.model.UserRole
import com.example.data.repository.LocalFinanceRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setApplicationId("1:1062042536379:android:cb82054f00dc4e53ba5c9e")
                    .setProjectId("gen-lang-client-0620717298")
                    .setApiKey("AIzaSyCVNze9cDJspWLnTwqKofNr0j1j0muQ9WU")
                    .build()
            )
        }
    }

    @Test
    fun `read string from context verifies LOCAL FINANCE app name`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("LOCAL FINANCE", appName)
    }

    @Test
    fun `auth manager supports developer backend login and role switching`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val authManager = FirebaseAuthManager(context)

        // Test developer authentication
        val isDev = authManager.loginAsDeveloper("DEV-ROOT")
        assertTrue(isDev)
        assertEquals(UserRole.DEVELOPER.name, authManager.currentProfile.value.role)

        // Test role switching
        authManager.switchRoleAndCompany(UserRole.MANAGER, "comp_1")
        assertEquals(UserRole.MANAGER.name, authManager.currentProfile.value.role)
        assertEquals("comp_1", authManager.currentProfile.value.companyId)
    }

    @Test
    fun `repository initial data loads companies and sequences`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = LocalFinanceRepository(context)

        assertNotNull(repo.currentCompany.value)
        assertTrue(repo.companies.value.isNotEmpty())
        val isDev = repo.authenticateDeveloper("DEV-ROOT")
        assertTrue(isDev)
        assertEquals(UserRole.DEVELOPER.name, repo.currentProfile.value.role)
    }
}
