package fr.leafvibe.leafactu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LeafGreen = Color(0xFF206B4D)
private val LeafBrown = Color(0xFF6B4935)
private val LeafGold = Color(0xFFC5A46D)
private val LeafCream = Color(0xFFDCCBB1)
private val Page = Color(0xFFF7F4ED)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LeafActuApp() }
    }
}

private enum class Period { PAST, NOW, FUTURE }
private data class NewsItem(val title: String, val category: String, val region: String,
    val period: Period, val description: String, val dateLabel: String)

private val demoNews = listOf(
    NewsItem("Les grandes étapes de la construction européenne", "Histoire", "Europe", Period.PAST,
        "Un dossier chronologique pour retrouver les dates et les événements majeurs.", "Archives"),
    NewsItem("Les actualités à suivre aujourd'hui", "Monde", "Monde", Period.NOW,
        "Cette zone accueillera les articles récents issus des sources choisies.", "Aujourd'hui"),
    NewsItem("Les prochains rendez-vous scientifiques", "Sciences", "Monde", Period.FUTURE,
        "Retrouve les événements annoncés, conférences et missions planifiées.", "À venir"),
    NewsItem("Innovations et nouvelles technologies", "Technologie", "France", Period.NOW,
        "Les articles technologiques seront regroupés ici par date et par source.", "Récent")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeafActuApp() {
    var tab by remember { mutableStateOf("Accueil") }
    var category by remember { mutableStateOf("Toutes") }
    var region by remember { mutableStateOf("Toutes") }
    var settings by remember { mutableStateOf(false) }
    val categories = listOf("Toutes", "Politique", "Monde", "Technologie", "Sciences", "Sport",
        "Économie", "Environnement", "Culture", "Histoire")
    val regions = listOf("Toutes", "France", "Alsace", "Europe", "Allemagne", "Japon", "Monde")
    MaterialTheme(colorScheme = lightColorScheme(primary = LeafGreen, secondary = LeafGold,
        tertiary = LeafBrown, background = Page, surface = Color.White)) {
        Scaffold(
            topBar = { TopAppBar(title = {
                Column {
                    Text("🍃 LeafActu", fontWeight = FontWeight.Bold, color = LeafGreen)
                    Text("Le monde, à travers le temps", fontSize = 11.sp, color = LeafBrown)
                }
            }, actions = { IconButton(onClick = { settings = true }) {
                Icon(Icons.Default.Tune, contentDescription = "Personnaliser")
            } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Page)) },
            bottomBar = { NavigationBar(containerColor = Color.White) {
                listOf("Accueil", "Passé", "Présent", "Futur", "Explorer").forEach { name ->
                    val icon = when (name) {
                        "Passé" -> Icons.Default.History
                        "Présent" -> Icons.Default.Feed
                        "Futur" -> Icons.Default.Event
                        "Explorer" -> Icons.Default.Explore
                        else -> Icons.Default.Home
                    }
                    NavigationBarItem(selected = tab == name, onClick = { tab = name },
                        icon = { Icon(icon, contentDescription = name) }, label = { Text(name) })
                }
            } }, containerColor = Page
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize()) {
                if (settings) {
                    Personalization(categories, regions, category, region,
                        { category = it }, { region = it }, { settings = false })
                } else if (tab == "Explorer") {
                    Explorer(categories, regions, { category = it; tab = "Accueil" },
                        { region = it; tab = "Accueil" })
                } else {
                    val period = when (tab) {
                        "Passé" -> Period.PAST
                        "Futur" -> Period.FUTURE
                        "Présent" -> Period.NOW
                        else -> null
                    }
                    Feed(period, category, region)
                }
            }
        }
    }
}

@Composable
private fun Feed(period: Period?, category: String, region: String) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(when (period) {
            Period.PAST -> "Explorer le passé"
            Period.NOW -> "L'actualité"
            Period.FUTURE -> "Ce qui arrive"
            null -> "À la une"
        }, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = LeafGreen,
            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
        Text("Catégorie : $category  •  Région : $region", color = LeafBrown, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(demoNews.filter {
                (period == null || it.period == period) &&
                (category == "Toutes" || it.category == category) &&
                (region == "Toutes" || it.region == region || it.region == "Monde")
            }) { item ->
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = LeafCream.copy(alpha = .45f), shape = RoundedCornerShape(50)) {
                                Text(item.category, Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    color = LeafGreen, fontSize = 11.sp)
                            }
                            Spacer(Modifier.weight(1f))
                            Text(item.dateLabel, color = LeafBrown, fontSize = 11.sp)
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(item.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(item.description, color = Color(0xFF5F665F), fontSize = 13.sp)
                        Spacer(Modifier.height(10.dp))
                        Text("🌍 ${item.region}  •  Source à connecter", color = LeafBrown, fontSize = 12.sp)
                    }
                }
            }
            item { Text("Démonstration : ces cartes ne sont pas des actualités en direct. Les sources réelles seront connectées ensuite.",
                color = LeafBrown, fontSize = 12.sp, modifier = Modifier.padding(vertical = 12.dp)) }
        }
    }
}

@Composable
private fun Explorer(categories: List<String>, regions: List<String>,
    onCategory: (String) -> Unit, onRegion: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("Explorer", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = LeafGreen) }
        item { Text("CATÉGORIES", fontWeight = FontWeight.Bold, color = LeafBrown) }
        items(categories) { value ->
            ListItem(headlineContent = { Text(value) },
                leadingContent = { Icon(Icons.Default.Label, null, tint = LeafGreen) },
                modifier = Modifier.clickable { onCategory(value) })
        }
        item { Text("RÉGIONS", fontWeight = FontWeight.Bold, color = LeafBrown) }
        items(regions) { value ->
            ListItem(headlineContent = { Text(value) },
                leadingContent = { Icon(Icons.Default.Public, null, tint = LeafGreen) },
                modifier = Modifier.clickable { onRegion(value) })
        }
    }
}

@Composable
private fun Personalization(categories: List<String>, regions: List<String>, category: String,
    region: String, onCategory: (String) -> Unit, onRegion: (String) -> Unit, close: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Personnalisation", fontSize = 23.sp, fontWeight = FontWeight.Bold,
                color = LeafGreen, modifier = Modifier.weight(1f))
            TextButton(onClick = close) { Text("Terminé") }
        }
        Text("Catégorie suivie", color = LeafBrown)
        categories.forEach { value ->
            Row(Modifier.fillMaxWidth().clickable { onCategory(value) }.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                RadioButton(category == value, onClick = { onCategory(value) }); Text(value)
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Text("Région suivie", color = LeafBrown)
        regions.forEach { value ->
            Row(Modifier.fillMaxWidth().clickable { onRegion(value) }.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                RadioButton(region == value, onClick = { onRegion(value) }); Text(value)
            }
        }
        Text("Les choix ne sont pas encore sauvegardés durablement dans cette version prototype.",
            fontSize = 12.sp, color = LeafBrown, modifier = Modifier.padding(top = 10.dp))
    }
}
