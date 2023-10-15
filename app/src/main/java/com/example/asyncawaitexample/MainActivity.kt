package com.example.asyncawaitexample

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.example.asyncawaitexample.R
import com.example.asyncawaitexample.ui.theme.AsyncAwaitExampleTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val asyncAwaitViewModel = AsyncAwaitViewModel(applicationContext)
        setContent {
            AsyncAwaitExampleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AsyncAwaitScreen(asyncAwaitViewModel)
                }
            }
        }
    }
}

@Composable
fun AsyncAwaitScreen(viewModel: AsyncAwaitViewModel) {
    val data by viewModel.data.collectAsState()
    val error by viewModel.error.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(true) {
        viewModel.fetchDataAsync()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (error) {
            Text("An error occurred")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                scope.launch {
                    viewModel.fetchDataAsync()
                }
            }) {
                Text("Retry")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
                data.forEach { item ->
                    val jsonObject = JSONObject(item)
                    val title = jsonObject.getString("title")
                    val body = jsonObject.getString("body")
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = title,
                                modifier = Modifier.padding(16.dp),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = body,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

class AsyncAwaitViewModel(private val context: Context) : ViewModel() {
    val data = MutableStateFlow<List<String>>(emptyList())
    val error = MutableStateFlow(false)

    suspend fun fetchDataAsync() {
        try {
            withContext(Dispatchers.IO) {
                val data = async { fetchData() }.await() // Execute fetchData asynchronously and await its result
                this@AsyncAwaitViewModel.data.emit(data)
            }
            error.value = false
        } catch (e: IOException) {
            error.value = true
        }
    }

    private suspend fun fetchData(): List<String> {
        val inputStream = context.resources.openRawResource(R.raw.sample)
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        return parseJson(jsonString)
    }

    private fun parseJson(jsonString: String): List<String> {
        val jsonArray = JSONArray(jsonString)
        val stringList = mutableListOf<String>()

        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.getJSONObject(i).toString()
            stringList.add(item)
        }
        return stringList
    }
}
