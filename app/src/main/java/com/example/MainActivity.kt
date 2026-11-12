package com.example

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.AuthUiState
import com.example.data.model.UserRole
import com.example.data.repository.LocalFinanceRepository
import com.example.ui.components.RoleSwitcherSheet
import com.example.ui.screens.CollectionExecutiveScreen
import com.example.ui.screens.CompanyAdminScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DeveloperScreen
import com.example.ui.screens.ManagerScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private lateinit var repository: LocalFinanceRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        repository = LocalFinanceRepository(applicationContext)

        setContent {
            val systemDark = isSystemInDarkTheme()
            var isDarkTheme by rememberSaveable { mutableStateOf(systemDark) }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                AppContent(
                    repository = repository,
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = { isDarkTheme = !isDarkTheme }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppContent(
    repository: LocalFinanceRepository,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val context = LocalContext.current
    val authUiState by repository.authManager.uiState.collectAsState()
    val currentProfile by repository.currentProfile.collectAsState()
    val currentCompany by repository.currentCompany.collectAsState()

    var showRoleSheet by remember { mutableStateOf(false) }
    var selectedNavIndex by rememberSaveable { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        if (authUiState is AuthUiState.Idle) {
            repository.authManager.attemptAutoSignIn()
        }
    }

    Crossfade(
        targetState = authUiState,
        label = "auth_screen_transition",
        modifier = Modifier.fillMaxSize()
    ) { state ->
        if (state is AuthUiState.Authenticated) {
            val isCloudSession = state.isCloudSession

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "LOCAL FINANCE",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    // Cloud vs Offline indicator
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                if (isCloudSession) Color(0xFF10B981).copy(alpha = 0.15f)
                                                else MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                            )
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (isCloudSession) "CLOUD SYNC" else "LOCAL",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCloudSession) Color(0xFF10B981) else MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                                Text(
                                    text = currentCompany?.name ?: "No Company Selected",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        },
                        actions = {
                            // Role Switcher Button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                    .clickable { showRoleSheet = true }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .testTag("top_bar_role_switcher_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.SwapHoriz,
                                        contentDescription = "Switch Role",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = currentProfile.role.replace("_", " "),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // Theme Toggle
                            IconButton(
                                onClick = onToggleTheme,
                                modifier = Modifier.testTag("toggle_dark_theme_button")
                            ) {
                                Icon(
                                    if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "Toggle Theme",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Account Sign-Out
                            IconButton(
                                onClick = {
                                    repository.authManager.signOut {
                                        Toast.makeText(context, "Signed out successfully", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.testTag("auth_logout_button")
                            ) {
                                Icon(
                                    Icons.Default.AccountCircle,
                                    contentDescription = "Account Sign Out",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        NavigationBarItem(
                            selected = selectedNavIndex == 0,
                            onClick = { selectedNavIndex = 0 },
                            icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                            label = { Text("Dashboard") },
                            modifier = Modifier.testTag("nav_item_dashboard")
                        )

                        // Role-specific second tab
                        when (currentProfile.role) {
                            UserRole.DEVELOPER.name -> {
                                NavigationBarItem(
                                    selected = selectedNavIndex == 1,
                                    onClick = { selectedNavIndex = 1 },
                                    icon = { Icon(Icons.Default.Business, contentDescription = "Companies") },
                                    label = { Text("Companies") },
                                    modifier = Modifier.testTag("nav_item_developer")
                                )
                            }
                            UserRole.COMPANY_ADMIN.name -> {
                                NavigationBarItem(
                                    selected = selectedNavIndex == 1,
                                    onClick = { selectedNavIndex = 1 },
                                    icon = { Icon(Icons.Default.Security, contentDescription = "Admin") },
                                    label = { Text("Admin") },
                                    modifier = Modifier.testTag("nav_item_admin")
                                )
                            }
                            UserRole.MANAGER.name -> {
                                NavigationBarItem(
                                    selected = selectedNavIndex == 1,
                                    onClick = { selectedNavIndex = 1 },
                                    icon = { Icon(Icons.Default.PointOfSale, contentDescription = "Manager") },
                                    label = { Text("Operations") },
                                    modifier = Modifier.testTag("nav_item_manager")
                                )
                            }
                            else -> { // COLLECTION EXECUTIVE
                                NavigationBarItem(
                                    selected = selectedNavIndex == 1,
                                    onClick = { selectedNavIndex = 1 },
                                    icon = { Icon(Icons.Default.PointOfSale, contentDescription = "Collect") },
                                    label = { Text("Collect") },
                                    modifier = Modifier.testTag("nav_item_executive")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (selectedNavIndex) {
                        0 -> DashboardScreen(repository = repository)
                        1 -> {
                            when (currentProfile.role) {
                                UserRole.DEVELOPER.name -> DeveloperScreen(repository = repository)
                                UserRole.COMPANY_ADMIN.name -> CompanyAdminScreen(repository = repository)
                                UserRole.MANAGER.name -> ManagerScreen(repository = repository)
                                else -> CollectionExecutiveScreen(repository = repository)
                            }
                        }
                    }
                }
            }

            if (showRoleSheet) {
                RoleSwitcherSheet(
                    repository = repository,
                    onDismiss = { showRoleSheet = false }
                )
            }
        } else {
            LoginScreen(
                isLoading = state is AuthUiState.Loading,
                onGoogleSignIn = {
                    repository.authManager.signInWithGoogle(
                        activity = context as Activity,
                        onSuccess = {
                            Toast.makeText(context, "Welcome ${repository.currentProfile.value.name}!", Toast.LENGTH_SHORT).show()
                        },
                        onError = { err ->
                            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                        }
                    )
                },
                onDeveloperLogin = { key ->
                    val ok = repository.authenticateDeveloper(key)
                    if (ok) {
                        selectedNavIndex = 1 // Open Developer Console
                        Toast.makeText(context, "Developer Root Authenticated! Backend Console Active.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Invalid Developer Key. Hint: DEV-ROOT", Toast.LENGTH_SHORT).show()
                    }
                },
                onEnterWorkspace = {
                    repository.authManager.enterEnterpriseWorkspace()
                    Toast.makeText(context, "Logged into Enterprise Workspace", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
fun LoginScreen(
    isLoading: Boolean,
    onGoogleSignIn: () -> Unit,
    onDeveloperLogin: (String) -> Unit,
    onEnterWorkspace: () -> Unit
) {
    var showDevDialog by remember { mutableStateOf(false) }
    var devKeyInput by remember { mutableStateOf("DEV-ROOT") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PointOfSale,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "LOCAL FINANCE",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Firebase Auth Multi-Role Enterprise Management System",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 1. Google Sign-In via Credential Manager
            Button(
                onClick = onGoogleSignIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("google_sign_in_button"),
                enabled = !isLoading,
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Sign in with Google", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Developer Backend Login
            OutlinedButton(
                onClick = { showDevDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("developer_backend_login_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    Icons.Default.Terminal,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Developer Login (Backend Root)",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Enter Enterprise Workspace
            Button(
                onClick = onEnterWorkspace,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("quick_access_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Enter Enterprise Workspace", fontWeight = FontWeight.Medium)
            }
        }
    }

    if (showDevDialog) {
        AlertDialog(
            onDismissRequest = { showDevDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Terminal, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Developer Backend Login", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Enter Developer Master Access Key to authenticate as Developer Root and provision multiple companies in backend.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = devKeyInput,
                        onValueChange = { devKeyInput = it },
                        label = { Text("Developer Master Key") },
                        modifier = Modifier.fillMaxWidth().testTag("developer_key_input"),
                        singleLine = true
                    )
                    Text(
                        "Default Master Key: DEV-ROOT",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDevDialog = false
                        onDeveloperLogin(devKeyInput.trim())
                    },
                    modifier = Modifier.testTag("submit_developer_login_button")
                ) {
                    Text("Authenticate Developer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDevDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
