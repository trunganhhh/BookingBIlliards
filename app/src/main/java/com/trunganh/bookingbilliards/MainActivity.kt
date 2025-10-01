package com.trunganh.bookingbilliards

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.trunganh.bookingbilliards.databinding.ActivityMainBinding
import com.trunganh.bookingbilliards.manager.UserManager

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var drawerLayout: DrawerLayout

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted
        } else {
            // Permission denied
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup toolbar
        setSupportActionBar(binding.toolbar)

        // Setup navigation
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Setup drawer layout
        drawerLayout = binding.drawerLayout
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.navigation_home, R.id.navigation_tables, R.id.navigation_account),
            drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navigationView.setupWithNavController(navController)
        binding.bottomNavigationView.setupWithNavController(navController)

        // Setup navigation view listener
        binding.navigationView.setNavigationItemSelectedListener(this)

        // Update UI based on login status
        updateNavigationView()

        // Request notification permission for Android 13+
        requestNotificationPermission()
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_logout -> {
                UserManager.clearUserData()
                updateNavigationView()
                val navOptions = NavOptions.Builder()
                    .setPopUpTo(R.id.nav_graph, true)
                    .build()
                navController.navigate(R.id.loginFragment, null, navOptions)
                drawerLayout.closeDrawers()
                return true
            }
            R.id.action_login -> {
                val navOptions = NavOptions.Builder()
                    .setPopUpTo(R.id.nav_graph, false)
                    .build()
                navController.navigate(R.id.loginFragment, null, navOptions)
                drawerLayout.closeDrawers()
                return true
            }
            R.id.navigation_tables -> {
                if (!UserManager.isLoggedIn()) {
                    val navOptions = NavOptions.Builder()
                        .setPopUpTo(R.id.nav_graph, false)
                        .build()
                    navController.navigate(R.id.loginFragment, null, navOptions)
                } else {
                    navController.navigate(R.id.navigation_tables)
                }
                drawerLayout.closeDrawers()
                return true
            }
            R.id.navigation_home -> {
                navController.navigate(R.id.navigation_home)
                drawerLayout.closeDrawers()
                return true
            }
        }
        return false
    }

    fun updateNavigationView() {
        val navigationView = binding.navigationView
        val headerView = navigationView.getHeaderView(0)
        val tvUserName = headerView.findViewById<View>(R.id.tvUserName)
        val tvUserEmail = headerView.findViewById<View>(R.id.tvUserEmail)

        if (UserManager.isLoggedIn()) {
            val user = UserManager.getUser()
            tvUserName.visibility = View.VISIBLE
            tvUserEmail.visibility = View.VISIBLE
            tvUserName.findViewById<android.widget.TextView>(R.id.tvUserName).text = user?.login ?: ""
            tvUserEmail.findViewById<android.widget.TextView>(R.id.tvUserEmail).text = user?.email ?: ""
            
            // Show logout menu item
            navigationView.menu.findItem(R.id.group_logged_in)?.isVisible = true
            navigationView.menu.findItem(R.id.group_logged_out)?.isVisible = false
        } else {
            tvUserName.visibility = View.VISIBLE
            tvUserEmail.visibility = View.GONE
            tvUserName.findViewById<android.widget.TextView>(R.id.tvUserName).text = "Chưa đăng nhập"
            
            // Show login menu item
            navigationView.menu.findItem(R.id.group_logged_in)?.isVisible = false
            navigationView.menu.findItem(R.id.group_logged_out)?.isVisible = true
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Show rationale if needed
                }
                else -> {
                    // Request permission
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
}

