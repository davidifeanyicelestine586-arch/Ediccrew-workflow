package com.example

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.repository.WorkflowRepository
import com.example.ui.DashboardScreen
import com.example.ui.DashboardViewModel
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Ediccrew", appName)
  }

  @Test
  fun testDashboardScreenStartup() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    val repository = WorkflowRepository(db.workflowDao())
    val viewModel = DashboardViewModel(repository)

    composeTestRule.setContent {
      MyApplicationTheme {
        DashboardScreen(viewModel = viewModel)
      }
    }
    
    composeTestRule.waitForIdle()
  }

  @Test
  fun testVariableHighlightTransformation() {
    val transformation = com.example.ui.VariableHighlightTransformation()
    val inputText = androidx.compose.ui.text.AnnotatedString("Generate content about {{topic}} inside {platform}")
    val result = transformation.filter(inputText)
    
    // Check lengths match (offset identity mapping)
    assertEquals(inputText.text.length, result.text.length)
    
    // Check styled parts exist
    val styles = result.text.spanStyles
    assertEquals(2, styles.size)
  }
}
