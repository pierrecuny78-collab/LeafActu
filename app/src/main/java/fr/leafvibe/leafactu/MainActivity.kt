package fr.leafvibe.leafactu

import android.os.Bundle
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject

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
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferences = remember {
        context.getSharedPreferences("leafactu_preferences", Context.MODE_PRIVATE)
    }
    var tab by remember { mutableStateOf("Accueil") }
    var category by remember {
        mutableStateOf(preferences.getString("category", "Toutes") ?: "Toutes")
    }
    var region by remember {
        mutableStateOf(preferences.getString("region", "Toutes") ?: "Toutes")
    }
    var settings by remember { mutableStateOf(false) }
    val categories = listOf("Toutes", "Politique", "Monde", "Technologie", "Sciences", "Sport", "Économie", "Environnement", "Culture", "Histoire", "Santé", "Éducation", "International", "Local", "Justice", "Météo", "Espace", "Automobile", "Jeux vidéo", "Lifestyle", "Bonnes nouvelles")
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
            bottomBar = { Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                listOf("Accueil", "Passé", "Présent", "Futur", "LeafSwipe", "Enregistrés", "Explorer").forEach { name ->
                    val icon = when (name) {
                        "Passé" -> Icons.Default.History
                        "Présent" -> Icons.Default.Feed
                        "Futur" -> Icons.Default.Event
                        "Enregistrés" -> Icons.Default.Bookmarks
                        "LeafSwipe" -> Icons.Default.Swipe
                        "Explorer" -> Icons.Default.Explore
                        else -> Icons.Default.Home
                    }
                    NavigationBarItem(selected = tab == name, onClick = { tab = name }, modifier = Modifier.width(76.dp), icon = { Icon(icon, contentDescription = name) }, label = { Text(name, fontSize = 9.sp, maxLines = 1, softWrap = false) })
                }
            } }, containerColor = Page
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize()) {
                if (settings) {
                    Personalization(categories, regions, category, region,
                        { category = it; preferences.edit().putString("category", it).apply() },
                        { region = it; preferences.edit().putString("region", it).apply() },
                        { settings = false })
                } else if (tab == "LeafSwipe") {
                    LeafSwipeScreen()
                } else if (tab == "Explorer") {
                    Explorer(categories, regions,
                        { category = it; preferences.edit().putString("category", it).apply(); tab = "Accueil" },
                        { region = it; preferences.edit().putString("region", it).apply(); tab = "Accueil" })
                } else {
                    val period = when (tab) {
                        "Passé" -> Period.PAST
                        "Futur" -> Period.FUTURE
                        "Présent" -> Period.NOW
                        else -> null
                    }
                    if (period == Period.NOW || period == null || tab == "Enregistrés") LiveNewsFeed(category, region, savedOnly = tab == "Enregistrés") else Feed(period, category, region)
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
private fun LiveNewsFeed(selectedCategory: String, selectedRegion: String, savedOnly: Boolean = false) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var articles by remember { mutableStateOf<List<LiveArticle>>(emptyList()) }
    val preferences = remember { context.getSharedPreferences("leafactu_preferences", Context.MODE_PRIVATE) }
    var favorites by remember { mutableStateOf(preferences.getStringSet("favorites", emptySet())?.toSet() ?: emptySet()) }
    var savedArticles by remember { mutableStateOf(readSavedArticles(preferences.getString("saved_articles", "[]") ?: "[]")) }
    var searchText by remember { mutableStateOf("") }
    var cardLayout by remember { mutableStateOf(preferences.getString("layout", "cards") ?: "cards") }
    val sourceArticles = if (savedOnly) savedArticles else articles
    val visibleArticles = sourceArticles.filter { article ->
        (!savedOnly || (article.link.ifBlank { article.title }) in favorites) &&
        (selectedCategory == "Toutes" || article.category == selectedCategory) &&
        (selectedRegion == "Toutes" || article.region == selectedRegion || article.region == "Monde") &&
        (searchText.isBlank() || article.title.contains(searchText, true) || article.summary.contains(searchText, true))
    }
    var loading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshKey) {
        loading = true
        articles = RssRepository.loadLatest()
        loading = false
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("L'actualité en direct", fontSize = 23.sp, fontWeight = FontWeight.Bold,
                    color = LeafGreen, modifier = Modifier.padding(top = 12.dp))
                Text(if (savedOnly) "Articles enregistrés" else "Flux RSS • ${visibleArticles.size} articles • Région : $selectedRegion",
                    color = LeafBrown, fontSize = 12.sp)
            }
            IconButton(onClick = { refreshKey++ }, enabled = !loading) {
                Icon(Icons.Default.Refresh, contentDescription = "Actualiser", tint = LeafGreen)
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Text("Affichage", color = LeafBrown, fontSize = 12.sp)
            Spacer(Modifier.width(8.dp))
            FilterChip(selected = cardLayout == "cards", onClick = { cardLayout = "cards"; preferences.edit().putString("layout", "cards").apply() }, label = { Text("Cartes") })
            Spacer(Modifier.width(6.dp))
            FilterChip(selected = cardLayout == "list", onClick = { cardLayout = "list"; preferences.edit().putString("layout", "list").apply() }, label = { Text("Liste") })
        }
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            singleLine = true,
            label = { Text("Rechercher dans les actualités") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
        )
        when {
            loading -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = LeafGreen)
            }
            visibleArticles.isEmpty() -> Column(Modifier.padding(top = 24.dp)) {
                Text(if (savedOnly) "Aucun article enregistré." else "Aucun article récupéré pour le moment.", fontWeight = FontWeight.Bold)
                Text(if (savedOnly) "Enregistre un article avec le marque-page pour le retrouver ici." else "Vérifie ta connexion Internet ou réessaie plus tard. Certains flux peuvent être indisponibles.",
                    color = LeafBrown, fontSize = 13.sp)
                TextButton(onClick = { refreshKey++ }) { Text("Réessayer") }
            }
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Text("${visibleArticles.size} article(s) affiché(s)", color = LeafBrown, fontSize = 11.sp) }
                items(visibleArticles) { article ->
                    Card(Modifier.fillMaxWidth().clickable {
                        if (article.link.startsWith("https://") || article.link.startsWith("http://")) {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(article.link)))
                        }
                    }, shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(if (cardLayout == "cards") 0.dp else 12.dp)) {
                            if (cardLayout == "cards") {
                                if (!article.imageUrl.isNullOrBlank()) {
                                    AsyncImage(model = article.imageUrl, contentDescription = "Image : ${article.title}", modifier = Modifier.fillMaxWidth().height(190.dp), contentScale = ContentScale.Crop)
                                } else {
                                    Box(Modifier.fillMaxWidth().height(112.dp), contentAlignment = Alignment.Center) {
                                        Surface(color = LeafCream.copy(alpha = .35f), modifier = Modifier.fillMaxSize()) {}
                                        Text("🍃 LeafActu", color = LeafGreen, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                            Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(article.category.uppercase(), color = LeafGreen,
                                    fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(8.dp))
                                Text("• ${article.source} • ${article.region}", color = LeafBrown, fontSize = 10.sp,
                                    modifier = Modifier.weight(1f))
                                IconButton(onClick = {
                                    val key = article.link.ifBlank { article.title }
                                    favorites = if (key in favorites) favorites - key else favorites + key
                                    savedArticles = if (key in favorites) savedArticles.filterNot { it.link.ifBlank { it.title } == key } else savedArticles.filterNot { it.link.ifBlank { it.title } == key } + article
                                    preferences.edit().putStringSet("favorites", favorites).putString("saved_articles", writeSavedArticles(savedArticles)).apply()
                                }) {
                                    Icon(
                                        if ((article.link.ifBlank { article.title }) in favorites)
                                            Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = "Ajouter ou retirer des favoris",
                                        tint = LeafGreen
                                    )
                                }
                            }
                            Spacer(Modifier.height(5.dp))
                            Text(article.title, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            if (article.summary.isNotBlank()) {
                                Spacer(Modifier.height(5.dp))
                                Text(article.summary, color = Color(0xFF5F665F), fontSize = 13.sp)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(article.published.ifBlank { "Date non précisée" },
                                color = LeafBrown, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeafSwipeScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("leafactu_preferences", Context.MODE_PRIVATE) }
    var mode by remember { mutableStateOf("Actualités") }
    var articles by remember { mutableStateOf<List<LiveArticle>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refresh by remember { mutableIntStateOf(0) }
    val prompts = remember {
        mapOf(
            "Le saviez-vous" to listOf("Les pieuvres possèdent trois cœurs.", "Une journée sur Vénus dure plus longtemps qu'une année sur Vénus.", "Le phytoplancton océanique produit une grande partie de l'oxygène terrestre.", "Les koalas ont des empreintes digitales très proches de celles des humains.", "Le miel correctement conservé peut rester stable pendant très longtemps."),
            "Motivation" to listOf("Un petit progrès régulier vaut mieux qu'un grand projet jamais commencé.", "La curiosité transforme chaque journée en occasion d'apprendre.", "Célèbre une chose que tu as terminée aujourd'hui.", "Tu peux ajuster ton objectif et avancer à ton rythme.", "Une bonne question est le début d'une découverte."),
            "Défi du jour" to listOf("Découvre une actualité locale et vérifie-la auprès de deux sources.", "Apprends cinq mots dans une langue étrangère.", "Fais une promenade et observe trois détails nouveaux.", "Explique à quelqu'un une information apprise aujourd'hui.", "Repère une initiative écologique près de chez toi.")
        )
    }
    LaunchedEffect(refresh) {
        loading = true
        val fresh = runCatching { RssRepository.loadLatest() }.getOrDefault(emptyList())
        val old = readSavedArticles(prefs.getString("news_cache", "[]") ?: "[]")
        articles = (fresh + old).distinctBy { it.link.ifBlank { it.title } }.sortedByDescending { it.published }
        prefs.edit().putString("news_cache", writeSavedArticles(articles)).apply()
        loading = false
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("LeafSwipe", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = LeafGreen); Text("Découvre, apprends et passe à l’action", fontSize = 12.sp, color = LeafBrown) }
            IconButton(onClick = { refresh++ }, enabled = !loading) { Icon(Icons.Default.Refresh, contentDescription = "Actualiser", tint = LeafGreen) }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Actualités", "Le saviez-vous", "Motivation", "Défi du jour").forEach { item -> FilterChip(selected = mode == item, onClick = { mode = item }, label = { Text(item, maxLines = 1) }) }
        }
        if (loading) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = LeafGreen) } }
        else if (mode == "Actualités" && articles.isEmpty()) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Aucune actualité récupérée. Réessaie.", color = LeafBrown) } }
        else {
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 14.dp), contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (mode == "Actualités") {
                    items(articles, key = { it.link.ifBlank { it.title } }) { article ->
                        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column {
                                if (!article.imageUrl.isNullOrBlank()) AsyncImage(model = article.imageUrl, contentDescription = article.title, modifier = Modifier.fillMaxWidth().height(190.dp), contentScale = ContentScale.Crop)
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(article.category + " • " + article.source, color = LeafGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(article.title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    Text(article.summary, color = LeafBrown, fontSize = 13.sp)
                                    Text(article.region + " • " + article.published, color = LeafBrown, fontSize = 11.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(onClick = {
                                            val key = article.link.ifBlank { article.title }
                                            val keys = prefs.getStringSet("favorites", emptySet())?.toMutableSet() ?: mutableSetOf()
                                            keys.add(key)
                                            val saved = readSavedArticles(prefs.getString("saved_articles", "[]") ?: "[]").filterNot { it.link.ifBlank { it.title } == key } + article
                                            prefs.edit().putStringSet("favorites", keys).putString("saved_articles", writeSavedArticles(saved)).apply()
                                            android.widget.Toast.makeText(context, "Article enregistré", android.widget.Toast.LENGTH_SHORT).show()
                                        }) { Icon(Icons.Default.BookmarkAdd, null); Text("Enregistrer") }
                                        Button(onClick = { if (article.link.startsWith("http")) context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(article.link))) }) { Text("Lire") }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(prompts[mode].orEmpty()) { prompt ->
                        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(if (mode == "Le saviez-vous") "💡" else if (mode == "Motivation") "🌱" else "🎯", fontSize = 36.sp)
                                Text(mode, color = LeafGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text(prompt, color = LeafBrown, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
private fun readSavedArticles(raw: String): List<LiveArticle> = runCatching { val a = JSONArray(raw); (0 until a.length()).mapNotNull { i -> val x = a.optJSONObject(i) ?: return@mapNotNull null; LiveArticle(x.optString("title"), x.optString("summary"), x.optString("link"), x.optString("published"), x.optString("source"), x.optString("category"), x.optString("region"), x.optString("imageUrl").takeIf { it.isNotBlank() }) } }.getOrDefault(emptyList())
private fun writeSavedArticles(items: List<LiveArticle>): String = JSONArray().apply { items.forEach { a -> put(JSONObject().apply { put("title", a.title); put("summary", a.summary); put("link", a.link); put("published", a.published); put("source", a.source); put("category", a.category); put("region", a.region); put("imageUrl", a.imageUrl ?: "") }) } }.toString()

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
        Text("Tes préférences sont enregistrées sur cet appareil.",
            fontSize = 12.sp, color = LeafBrown, modifier = Modifier.padding(top = 10.dp))
        HorizontalDivider(Modifier.padding(vertical = 14.dp))
        Text("Sauvegarde et transfert", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = LeafGreen)
        Text("Exporte tes réglages et favoris dans un fichier JSON, ou restaure-les sur cet appareil.",
            fontSize = 12.sp, color = LeafBrown)
        val context = androidx.compose.ui.platform.LocalContext.current
        val exportLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/json")
        ) { uri ->
            if (uri != null) runCatching {
                val json = JSONObject()
                json.put("format", "leafactu-backup")
                json.put("version", 1)
                json.put("exportedAt", System.currentTimeMillis())
                val values = JSONObject()
                context.getSharedPreferences("leafactu_preferences", Context.MODE_PRIVATE).all.forEach { (key, value) ->
                    when (value) {
                        is Set<*> -> values.put(key, JSONArray(value.toList()))
                        is String -> values.put(key, value)
                        is Boolean -> values.put(key, value)
                        is Int -> values.put(key, value)
                        is Long -> values.put(key, value)
                        is Float -> values.put(key, value.toDouble())
                    }
                }
                json.put("preferences", values)
                json.put("savedArticles", JSONArray(context.getSharedPreferences("leafactu_preferences", Context.MODE_PRIVATE).getString("saved_articles", "[]") ?: "[]"))
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toString(2).toByteArray(Charsets.UTF_8)) }
                    ?: error("Impossible d'écrire le fichier")
            }.onFailure { android.widget.Toast.makeText(context, "Export impossible : \${it.message}", android.widget.Toast.LENGTH_LONG).show() }
             .onSuccess { android.widget.Toast.makeText(context, "Sauvegarde JSON exportée", android.widget.Toast.LENGTH_LONG).show() }
        }
        val importLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) runCatching {
                val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("Impossible de lire le fichier")
                val json = JSONObject(raw)
                require(json.optString("format") == "leafactu-backup") { "Ce fichier n'est pas une sauvegarde LeafActu" }
                val values = json.getJSONObject("preferences")
                val savedArticlesJson = json.optJSONArray("savedArticles") ?: JSONArray()
                val editor = context.getSharedPreferences("leafactu_preferences", Context.MODE_PRIVATE).edit().clear()
                values.keys().forEach { key ->
                    when (val value = values.get(key)) {
                        is JSONArray -> editor.putStringSet(key, (0 until value.length()).map { value.getString(it) }.toSet())
                        is String -> editor.putString(key, value)
                        is Boolean -> editor.putBoolean(key, value)
                        is Int -> editor.putInt(key, value)
                        is Long -> editor.putLong(key, value)
                        is Double -> editor.putFloat(key, value.toFloat())
                    }
                }
                editor.putString("saved_articles", savedArticlesJson.toString())
                editor.apply()
            }.onFailure { android.widget.Toast.makeText(context, "Import impossible : \${it.message}", android.widget.Toast.LENGTH_LONG).show() }
             .onSuccess { android.widget.Toast.makeText(context, "Sauvegarde restaurée. Rouvre l'application pour actualiser les réglages.", android.widget.Toast.LENGTH_LONG).show() }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { exportLauncher.launch("LeafActu_backup.json") }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.FileDownload, contentDescription = null)
                Spacer(Modifier.width(6.dp)); Text("Exporter JSON")
            }
            OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/*")) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.FileUpload, contentDescription = null)
                Spacer(Modifier.width(6.dp)); Text("Importer JSON")
            }
        }
    }
}
