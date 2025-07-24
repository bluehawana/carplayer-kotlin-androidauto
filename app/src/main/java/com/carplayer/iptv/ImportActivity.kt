package com.carplayer.iptv

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.carplayer.iptv.storage.M3UFileManager
import kotlinx.coroutines.launch

class ImportActivity : AppCompatActivity() {
    
    private lateinit var urlEditText: EditText
    private lateinit var nameEditText: EditText
    private lateinit var importButton: Button
    private lateinit var statusTextView: TextView
    private lateinit var testButton: Button
    
    private val m3uFileManager = M3UFileManager(this)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create simple layout programmatically
        setContentView(createLayout())
        
        supportActionBar?.title = "Import M3U Files"
        
        setupButtons()
    }
    
    private fun createLayout(): android.widget.LinearLayout {
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        
        // URL input
        layout.addView(TextView(this).apply {
            text = "M3U URL:"
            textSize = 18f
        })
        
        urlEditText = EditText(this).apply {
            hint = "Enter M3U playlist URL"
            setText("https://raw.githubusercontent.com/YueChan/Live/refs/heads/main/Test.m3u")
        }
        layout.addView(urlEditText)
        
        // Name input
        layout.addView(TextView(this).apply {
            text = "Playlist Name:"
            textSize = 18f
        })
        
        nameEditText = EditText(this).apply {
            hint = "Enter playlist name"
            setText("Test Playlist")
        }
        layout.addView(nameEditText)
        
        // Import button
        importButton = Button(this).apply {
            text = "Import M3U Playlist"
        }
        layout.addView(importButton)
        
        // Test button
        testButton = Button(this).apply {
            text = "Test Built-in M3U"
        }
        layout.addView(testButton)
        
        // Status text
        statusTextView = TextView(this).apply {
            text = "Ready to import"
            textSize = 16f
        }
        layout.addView(statusTextView)
        
        return layout
    }
    
    private fun setupButtons() {
        importButton.setOnClickListener {
            val url = urlEditText.text.toString().trim()
            val name = nameEditText.text.toString().trim()
            
            if (url.isEmpty()) {
                Toast.makeText(this, "Please enter a URL", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            importFromUrl(url, name)
        }
        
        testButton.setOnClickListener {
            testBuiltInM3U()
        }
    }
    
    private fun importFromUrl(url: String, name: String) {
        statusTextView.text = "Importing..."
        importButton.isEnabled = false
        
        lifecycleScope.launch {
            try {
                val metadata = m3uFileManager.downloadAndSaveM3U(url, name)
                if (metadata != null) {
                    statusTextView.text = "Success! Imported ${metadata.channelCount} channels"
                    Toast.makeText(this@ImportActivity, "Import successful!", Toast.LENGTH_SHORT).show()
                } else {
                    statusTextView.text = "Import failed"
                    Toast.makeText(this@ImportActivity, "Import failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                statusTextView.text = "Error: ${e.message}"
                Toast.makeText(this@ImportActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
            
            importButton.isEnabled = true
        }
    }
    
    private fun testBuiltInM3U() {
        statusTextView.text = "Testing built-in M3U..."
        testButton.isEnabled = false
        
        lifecycleScope.launch {
            try {
                val importer = M3UImporter(this@ImportActivity)
                val importedFiles = importer.importProjectM3UFiles()
                
                if (importedFiles.isNotEmpty()) {
                    val file = importedFiles.first()
                    statusTextView.text = "Built-in M3U loaded: ${file.channelCount} channels"
                    Toast.makeText(this@ImportActivity, "Built-in M3U working!", Toast.LENGTH_SHORT).show()
                } else {
                    statusTextView.text = "Built-in M3U not found"
                    Toast.makeText(this@ImportActivity, "Built-in M3U not found", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                statusTextView.text = "Built-in M3U error: ${e.message}"
                Toast.makeText(this@ImportActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
            
            testButton.isEnabled = true
        }
    }
}