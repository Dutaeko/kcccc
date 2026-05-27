package co.komikcast.app

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import co.komikcast.app.data.ChapterItem
import co.komikcast.app.data.DetailUiState
import co.komikcast.app.data.FavoriteMangaEntity
import co.komikcast.app.data.MangaItem
import co.komikcast.app.data.PageUiState
import co.komikcast.app.data.ReaderUiState
import co.komikcast.app.data.ReadingHistoryEntity
import co.komikcast.app.data.displayChapterIndex
import co.komikcast.app.data.numericChapterIndex
import co.komikcast.app.data.toMangaItem
import co.komikcast.app.ui.theme.KomikcastTheme
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.flow.distinctUntilChanged

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KomikcastTheme {
                KomikcastApp(viewModel)
            }
        }
    }
}

private data class BottomDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val bottomDestinations = listOf(
    BottomDestination("home", "Home", Icons.Rounded.Home),
    BottomDestination("popular", "Populer", Icons.Rounded.LocalFireDepartment),
    BottomDestination("favorite", "Favorite", Icons.Rounded.Favorite),
    BottomDestination("history", "History", Icons.Rounded.History),
    BottomDestination("search", "Pencarian", Icons.Rounded.Search)
)

@Composable
private fun KomikcastApp(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val message by viewModel.message.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: "home"
    ApplySystemBars(currentRoute == "reader")

    LaunchedEffect(message) {
        val value = message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(value)
        viewModel.consumeMessage()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (bottomDestinations.any { it.route == currentRoute }) {
                AppBottomBar(currentRoute, navController)
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            composable("home") {
                val state by viewModel.home.collectAsStateWithLifecycle()
                FeedScreen(
                    title = "Rilisan Terbaru",
                    state = state,
                    onRefresh = viewModel::refreshHome,
                    onLoadMore = viewModel::loadMoreHome,
                    onOpen = { manga -> openDetail(navController, viewModel, manga) },
                    onLatestChapter = { manga ->
                        viewModel.openLatestReader(manga) { navController.navigate("reader") }
                    }
                )
            }
            composable("popular") {
                val state by viewModel.popular.collectAsStateWithLifecycle()
                FeedScreen(
                    title = "Populer",
                    state = state,
                    onRefresh = viewModel::refreshPopular,
                    onLoadMore = viewModel::loadMorePopular,
                    onOpen = { manga -> openDetail(navController, viewModel, manga) },
                    onLatestChapter = { manga ->
                        viewModel.openLatestReader(manga) { navController.navigate("reader") }
                    }
                )
            }
            composable("favorite") {
                val favorites by viewModel.favorites.collectAsStateWithLifecycle()
                FavoriteScreen(
                    favorites = favorites,
                    viewModel = viewModel,
                    onOpen = { manga -> openDetail(navController, viewModel, manga) }
                )
            }
            composable("history") {
                val histories by viewModel.histories.collectAsStateWithLifecycle()
                HistoryScreen(
                    histories = histories,
                    onOpen = { history ->
                        viewModel.openDetail(history.seriesSlug)
                        navController.navigate("detail")
                    },
                    onPlay = { history ->
                        viewModel.openReader(
                            slug = history.seriesSlug,
                            title = history.seriesTitle,
                            coverImage = history.coverImage,
                            chapterIndex = history.chapterIndex
                        ) {
                            navController.navigate("reader")
                        }
                    },
                    onClearAll = viewModel::clearHistories,
                    onDeleteSelected = viewModel::deleteHistories
                )
            }
            composable("search") {
                val state by viewModel.search.collectAsStateWithLifecycle()
                val query by viewModel.query.collectAsStateWithLifecycle()
                val selectedGenre by viewModel.selectedGenre.collectAsStateWithLifecycle()
                val genres by viewModel.genres.collectAsStateWithLifecycle()
                SearchScreen(
                    state = state,
                    query = query,
                    selectedGenre = selectedGenre,
                    genres = genres.map { it.name },
                    onQueryChange = viewModel::setQuery,
                    onSubmit = viewModel::refreshSearch,
                    onGenre = viewModel::setGenre,
                    onLoadMore = viewModel::loadMoreSearch,
                    onOpen = { manga -> openDetail(navController, viewModel, manga) },
                    onLatestChapter = { manga ->
                        viewModel.openLatestReader(manga) { navController.navigate("reader") }
                    }
                )
            }
            composable("detail") {
                val state by viewModel.detail.collectAsStateWithLifecycle()
                DetailScreen(
                    state = state,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onRead = { viewModel.startReading { navController.navigate("reader") } },
                    onChapter = { manga, chapter ->
                        viewModel.openReader(manga.slug, manga.title, manga.coverImage, chapter.index) {
                            navController.navigate("reader")
                        }
                    },
                    onGenre = { genre ->
                        viewModel.openGenreResult(genre)
                        navController.navigate("genre_result")
                    }
                )
            }
            composable("genre_result") {
                val state by viewModel.genreResult.collectAsStateWithLifecycle()
                val title by viewModel.genreResultTitle.collectAsStateWithLifecycle()
                FeedScreen(
                    title = title.ifBlank { "Genre" },
                    state = state,
                    onRefresh = { viewModel.openGenreResult(title) },
                    onLoadMore = viewModel::loadMoreGenreResult,
                    onOpen = { manga -> openDetail(navController, viewModel, manga) },
                    onLatestChapter = { manga ->
                        viewModel.openLatestReader(manga) { navController.navigate("reader") }
                    }
                )
            }
            composable("reader") {
                val state by viewModel.reader.collectAsStateWithLifecycle()
                ReaderScreen(
                    state = state,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun ApplySystemBars(readerMode: Boolean) {
    val view = LocalView.current
    val background = if (readerMode) Color.Black else MaterialTheme.colorScheme.background
    SideEffect {
        val activity = view.context as? Activity ?: return@SideEffect
        val window = activity.window
        WindowCompat.setDecorFitsSystemWindows(window, !readerMode)
        window.statusBarColor = background.toArgb()
        window.navigationBarColor = background.toArgb()
        val controller = WindowInsetsControllerCompat(window, view)
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
        if (readerMode) {
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

private fun openDetail(navController: NavController, viewModel: MainViewModel, manga: MangaItem) {
    viewModel.openDetail(manga)
    navController.navigate("detail")
}

@Composable
private fun AppBottomBar(currentRoute: String, navController: NavController) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 12.dp
    ) {
        bottomDestinations.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = {
                    if (currentRoute != destination.route) {
                        navController.navigate(destination.route) {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(destination.icon, contentDescription = destination.label) },
                label = { Text(destination.label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            )
        }
    }
}

@Composable
private fun FeedScreen(
    title: String,
    state: PageUiState,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onOpen: (MangaItem) -> Unit,
    onLatestChapter: (MangaItem) -> Unit
) {
    val gridState = rememberLazyGridState()
    LoadMoreEffect(gridState, state.items.size, onLoadMore)
    SwipeRefresh(
        state = rememberSwipeRefreshState(state.isRefreshing),
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ScreenBrush())
                .padding(horizontal = 14.dp)
        ) {
            HeroHeader(title)
            MangaGrid(
                items = state.items,
                state = gridState,
                isLoading = state.isLoading,
                error = state.error,
                onOpen = onOpen,
                onLatestChapter = onLatestChapter,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SearchScreen(
    state: PageUiState,
    query: String,
    selectedGenre: String?,
    genres: List<String>,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onGenre: (String?) -> Unit,
    onLoadMore: () -> Unit,
    onOpen: (MangaItem) -> Unit,
    onLatestChapter: (MangaItem) -> Unit
) {
    val gridState = rememberLazyGridState()
    LoadMoreEffect(gridState, state.items.size, onLoadMore)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBrush())
            .padding(horizontal = 14.dp)
    ) {
        HeroHeader("Pencarian Manga")
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
            singleLine = true,
            label = { Text("Judul manga") }
        )
        Spacer(Modifier.height(12.dp))
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedGenre == null,
                    onClick = { onGenre(null) },
                    label = { Text("Semua") }
                )
            }
            items(genres) { genre ->
                FilterChip(
                    selected = selectedGenre == genre,
                    onClick = { onGenre(genre) },
                    label = { Text(genre) }
                )
            }
        }
        MangaGrid(
            items = state.items,
            state = gridState,
            isLoading = state.isLoading,
            error = state.error,
            onOpen = onOpen,
            onLatestChapter = onLatestChapter,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FavoriteScreen(
    favorites: List<FavoriteMangaEntity>,
    viewModel: MainViewModel,
    onOpen: (MangaItem) -> Unit
) {
    val context = LocalContext.current
    var pendingExport by remember { mutableStateOf<String?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        val data = pendingExport ?: return@rememberLauncherForActivityResult
        pendingExport = null
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(data.toByteArray(Charsets.UTF_8))
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val data = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (data.isNotBlank()) viewModel.importFavorites(data)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBrush())
            .padding(horizontal = 14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Favorite Manga",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { importLauncher.launch("text/*") },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.FileUpload, contentDescription = "Import")
            }
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        viewModel.exportFavorites { encrypted ->
                            pendingExport = encrypted
                            exportLauncher.launch("komikcast_favorite.kcf")
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.FileDownload, contentDescription = "Export")
            }
        }
        if (favorites.isEmpty()) {
            EmptyState("Belum ada favorite")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(favorites, key = { it.slug }) { item ->
                    MangaCard(manga = item.toMangaItem(), showLatestChapter = false, onOpen = onOpen)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryScreen(
    histories: List<ReadingHistoryEntity>,
    onOpen: (ReadingHistoryEntity) -> Unit,
    onPlay: (ReadingHistoryEntity) -> Unit,
    onClearAll: () -> Unit,
    onDeleteSelected: (Set<String>) -> Unit
) {
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var confirmDelete by remember { mutableStateOf(false) }
    val selectionMode = selected.isNotEmpty()

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Hapus History") },
            text = { Text(if (selectionMode) "Hapus history yang dipilih?" else "Hapus semua history manga?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (selectionMode) {
                            onDeleteSelected(selected)
                            selected = emptySet()
                        } else {
                            onClearAll()
                        }
                        confirmDelete = false
                    }
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Batal")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBrush())
            .padding(horizontal = 14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "History Baca",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            if (histories.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { confirmDelete = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Hapus history")
                }
            }
        }
        if (histories.isEmpty()) {
            EmptyState("Belum ada history baca")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 18.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(histories, key = { it.seriesSlug + it.chapterIndex }) { history ->
                    val isSelected = selected.contains(history.seriesSlug)
                    HistoryCard(
                        history = history,
                        selected = isSelected,
                        selectionMode = selectionMode,
                        onOpen = onOpen,
                        onPlay = onPlay,
                        onToggleSelection = {
                            selected = if (isSelected) selected - history.seriesSlug else selected + history.seriesSlug
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailScreen(
    state: DetailUiState,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onRead: () -> Unit,
    onChapter: (MangaItem, ChapterItem) -> Unit,
    onGenre: (String) -> Unit
) {
    val detail = state.detail
    val manga = detail?.manga
    val observedSlug = manga?.slug.orEmpty()
    val isFavorite by viewModel.observeFavorite(observedSlug).collectAsStateWithLifecycle(false)
    val chapterHistories by viewModel.observeHistoriesForManga(observedSlug).collectAsStateWithLifecycle(emptyMap())
    val context = LocalContext.current
    val chapterPrefs = remember { context.getSharedPreferences("chapter_layout", Context.MODE_PRIVATE) }
    var descendingOrder by remember { mutableStateOf(chapterPrefs.getBoolean("descending_order", true)) }
    var gridChapter by remember { mutableStateOf(chapterPrefs.getBoolean("grid_chapter", false)) }

    Scaffold(
        topBar = {
            DetailTopBar(
                title = manga?.title ?: "Detail Manga",
                showActions = manga != null,
                isFavorite = isFavorite,
                onBack = onBack,
                onFavorite = { manga?.let { viewModel.toggleFavorite(it) } },
                onSaveCover = { manga?.let { viewModel.saveCover(it) } }
            )
        }
    ) { padding ->
        SwipeRefresh(
            state = rememberSwipeRefreshState(state.isRefreshing),
            onRefresh = viewModel::refreshDetail,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.error != null -> EmptyState(state.error)
                state.isLoading || detail == null -> LoadingState()
                else -> {
                    val currentDetail = detail!!
                    val currentManga = currentDetail.manga
                    val orderedChapters = remember(state.chapters, descendingOrder) {
                        if (descendingOrder) {
                            state.chapters.sortedByDescending { numericChapterIndex(it.index) }
                        } else {
                            state.chapters.sortedBy { numericChapterIndex(it.index) }
                        }
                    }
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ScreenBrush())
                    ) {
                        item {
                            DetailHero(currentManga)
                        }
                        item {
                            Box(Modifier.padding(horizontal = 16.dp)) {
                                InfoCard("Deskripsi Manga") {
                                    androidx.compose.foundation.text.selection.SelectionContainer {
                                        Text(
                                            currentManga.synopsis,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                        item {
                            Box(Modifier.padding(horizontal = 16.dp)) {
                                InfoCard("Informasi Lengkap Manga") {
                                    currentDetail.information.forEach { row ->
                                        InfoRow(row.first, row.second)
                                    }
                                }
                            }
                        }
                        if (currentManga.genres.isNotEmpty()) {
                            item {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(currentManga.genres) { genre ->
                                        FilterChip(
                                            selected = false,
                                            onClick = { onGenre(genre) },
                                            label = { Text(genre) }
                                        )
                                    }
                                }
                            }
                        }
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Daftar Chapter",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.weight(1f)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            descendingOrder = !descendingOrder
                                            chapterPrefs.edit().putBoolean("descending_order", descendingOrder).apply()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.SwapVert, contentDescription = "Ubah urutan")
                                }
                                Spacer(Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            gridChapter = !gridChapter
                                            chapterPrefs.edit().putBoolean("grid_chapter", gridChapter).apply()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.GridView, contentDescription = "Ubah tampilan")
                                }
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = onRead,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.height(40.dp)
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Mulai membaca", maxLines = 1)
                                }
                            }
                        }
                        if (gridChapter) {
                            items(orderedChapters.chunked(2), key = { row -> row.joinToString("|") { it.index } }) { row ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    row.forEach { chapter ->
                                        ChapterRow(
                                            chapter = chapter,
                                            history = chapterHistories.values.firstOrNull { numericChapterIndex(it.chapterIndex) == numericChapterIndex(chapter.index) },
                                            modifier = Modifier.weight(1f),
                                            onClick = { onChapter(currentManga, chapter) }
                                        )
                                    }
                                    if (row.size == 1) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        } else {
                            items(orderedChapters, key = { it.index }) { chapter ->
                                ChapterRow(
                                    chapter = chapter,
                                    history = chapterHistories.values.firstOrNull { numericChapterIndex(it.chapterIndex) == numericChapterIndex(chapter.index) },
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    onClick = { onChapter(currentManga, chapter) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderScreen(
    state: ReaderUiState,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val reader = state.reader
    val listState = rememberLazyListState()
    var showChapters by remember { mutableStateOf(false) }
    var showControls by remember(reader?.chapterIndex) { mutableStateOf(false) }
    val chapterHistories by viewModel.observeHistoriesForManga(reader?.seriesSlug.orEmpty()).collectAsStateWithLifecycle(emptyMap())

    LaunchedEffect(reader?.chapterIndex) {
        if (reader != null) {
            showControls = false
            listState.scrollToItem(0)
        }
    }

    LaunchedEffect(reader?.chapterIndex, reader?.images?.size) {
        if (reader == null) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index -> viewModel.markReaderProgress(index) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            state.isLoading -> LoadingState()
            state.error != null -> EmptyState(state.error)
            reader == null -> EmptyState("Reader belum dibuka")
            else -> LazyColumn(
                state = listState,
                contentPadding = PaddingValues(bottom = 4.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(reader.images) { image ->
                    ReaderImage(image = image, contentDescription = reader.chapterTitle)
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(56.dp)
                .fillMaxHeight()
                .pointerInput(reader?.chapterIndex) {
                    detectTapGestures(
                        onTap = {
                            if (!listState.isScrollInProgress) {
                                showControls = !showControls
                            }
                        }
                    )
                }
        )
        if (showControls && reader != null) {
            ReaderTopBar(
                title = reader.seriesTitle,
                chapter = "Chapter ${displayChapterIndex(reader.chapterIndex)}",
                onBack = onBack,
                modifier = Modifier.align(Alignment.TopCenter)
            )
            ReaderControlBar(
                onPrev = viewModel::readerPrev,
                onRefresh = viewModel::refreshReader,
                onList = { showChapters = true },
                onNext = viewModel::readerNext,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
        if (showChapters && reader != null) {
            ReaderChapterOverlay(
                reader = reader,
                histories = chapterHistories,
                onDismiss = { showChapters = false },
                onChapter = { chapter ->
                    showChapters = false
                    showControls = false
                    viewModel.openReader(
                        slug = reader.seriesSlug,
                        title = reader.seriesTitle,
                        coverImage = reader.coverImage,
                        chapterIndex = chapter.index
                    ) {}
                }
            )
        }
    }
}

@Composable
private fun ReaderChapterOverlay(
    reader: co.komikcast.app.data.ReaderChapter,
    histories: Map<String, ReadingHistoryEntity>,
    onDismiss: () -> Unit,
    onChapter: (ChapterItem) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.58f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.72f),
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 430.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    Text(
                        "Daftar Chapter",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                items(reader.chapters.sortedByDescending { numericChapterIndex(it.index) }, key = { it.index }) { chapter ->
                    val history = histories.values.firstOrNull { numericChapterIndex(it.chapterIndex) == numericChapterIndex(chapter.index) }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (numericChapterIndex(chapter.index) == numericChapterIndex(reader.chapterIndex)) {
                            Color.White.copy(alpha = 0.18f)
                        } else {
                            Color.White.copy(alpha = 0.08f)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onChapter(chapter) }
                    ) {
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Chapter ${displayChapterIndex(chapter.index)}",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f)
                                )
                                if (history != null) {
                                    Text(
                                        "Progres baca ${history.progress}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            if (chapter.releaseDate.isNotBlank()) {
                                Text(chapter.releaseDate, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.68f))
                            }
                            if (history != null) {
                                LinearProgressIndicator(
                                    progress = { history.progress / 100f },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MangaGrid(
    items: List<MangaItem>,
    state: LazyGridState,
    isLoading: Boolean,
    error: String?,
    onOpen: (MangaItem) -> Unit,
    onLatestChapter: ((MangaItem) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (items.isEmpty() && isLoading) {
            LoadingState()
        } else if (items.isEmpty() && error != null) {
            EmptyState(error)
        } else if (items.isEmpty()) {
            EmptyState("Belum ada data")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                state = state,
                contentPadding = PaddingValues(top = 2.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items, key = { it.slug }) { manga ->
                    MangaCard(manga = manga, showLatestChapter = true, onOpen = onOpen, onLatestChapter = onLatestChapter)
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    AnimatedVisibility(isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MangaCard(
    manga: MangaItem,
    showLatestChapter: Boolean,
    onOpen: (MangaItem) -> Unit,
    onLatestChapter: ((MangaItem) -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen(manga) },
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data(manga.coverImage)
                    .crossfade(true)
                    .build(),
                contentDescription = manga.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.68f)
                    .clip(RoundedCornerShape(8.dp))
            )
            Text(
                manga.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
            if (showLatestChapter && manga.latestChapter.isNotBlank()) {
                Text(
                    manga.latestChapter,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(horizontal = 2.dp, vertical = 1.dp)
                        .clickable(enabled = onLatestChapter != null) { onLatestChapter?.invoke(manga) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryCard(
    history: ReadingHistoryEntity,
    selected: Boolean,
    selectionMode: Boolean,
    onOpen: (ReadingHistoryEntity) -> Unit,
    onPlay: (ReadingHistoryEntity) -> Unit,
    onToggleSelection: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (selectionMode) onToggleSelection() else onOpen(history) },
                onLongClick = onToggleSelection
            )
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data(history.coverImage)
                    .crossfade(true)
                    .build(),
                contentDescription = history.seriesTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 72.dp, height = 104.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(history.seriesTitle, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("Chapter ${displayChapterIndex(history.chapterIndex)}", color = MaterialTheme.colorScheme.primary)
                LinearProgressIndicator(
                    progress = { history.progress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Progres baca ${history.progress}%", style = MaterialTheme.typography.labelMedium)
            }
            if (selected) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = "Dipilih", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            } else {
                FloatingActionButton(onClick = { onPlay(history) }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = "Baca")
                }
            }
        }
    }
}

@Composable
private fun ChapterRow(
    chapter: ChapterItem,
    history: ReadingHistoryEntity?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Chapter ${displayChapterIndex(chapter.index)}",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (history != null) {
                    Text(
                        "Progres baca ${history.progress}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
            }
            if (chapter.releaseDate.isNotBlank()) {
                Text(chapter.releaseDate, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (history != null) {
                LinearProgressIndicator(
                    progress = { history.progress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun DetailHero(manga: MangaItem) {
    val backgroundColor = MaterialTheme.colorScheme.background
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(278.dp)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(manga.coverImage)
                .crossfade(true)
                .build(),
            contentDescription = manga.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.48f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(92.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            backgroundColor.copy(alpha = 0.92f),
                            backgroundColor
                        )
                    )
                )
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 30.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(manga.coverImage)
                    .crossfade(true)
                    .build(),
                contentDescription = manga.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(126.dp)
                    .aspectRatio(0.68f)
                    .clip(RoundedCornerShape(14.dp))
            )
            Spacer(Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 20.dp)
            ) {
                androidx.compose.foundation.text.selection.SelectionContainer {
                    Text(
                        manga.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.62f))
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.42f))
        Text(value, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.58f))
    }
}

@Composable
private fun ActionPill(text: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = modifier.height(46.dp), shape = RoundedCornerShape(14.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(7.dp))
        Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun DetailTopBar(
    title: String,
    showActions: Boolean,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onFavorite: () -> Unit,
    onSaveCover: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.background, tonalElevation = 0.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Kembali")
            }
            Spacer(Modifier.width(4.dp))
            Text(
                title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (showActions) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSaveCover() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = "Simpan cover", tint = Color.White)
                }
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onFavorite() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color.Red else Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ReaderTopBar(title: String, chapter: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        color = Color.Black.copy(alpha = 0.42f),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 10.dp, end = 10.dp, top = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Kembali", tint = Color.White)
            }
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Black, color = Color.White)
                Text(chapter, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.72f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ReaderControlBar(
    onPrev: () -> Unit,
    onRefresh: () -> Unit,
    onList: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Black.copy(alpha = 0.42f),
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ReaderButton(Icons.Rounded.SkipPrevious, "Prev", onPrev)
            ReaderButton(Icons.Rounded.Refresh, "Refresh", onRefresh)
            ReaderButton(Icons.Rounded.FormatListBulleted, "Daftar Chapter", onList)
            ReaderButton(Icons.Rounded.SkipNext, "Next", onNext)
        }
    }
}

@Composable
private fun ReaderButton(icon: ImageVector, text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = text, tint = Color.White, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun ReaderImage(image: String, contentDescription: String) {
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(image)
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        contentScale = ContentScale.FillWidth,
        modifier = Modifier.fillMaxWidth()
    ) {
        when (painter.state) {
            is AsyncImagePainter.State.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(480.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp, color = Color.White)
                }
            }
            is AsyncImagePainter.State.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Gagal memuat gambar", color = Color.White.copy(alpha = 0.72f))
                }
            }
            else -> SubcomposeAsyncImageContent()
        }
    }
}

@Composable
private fun HeroHeader(title: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 8.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyState(message: String?, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message ?: "Kosong", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ScreenBrush(): Brush {
    return Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.background
        )
    )
}

@Composable
private fun LoadMoreEffect(gridState: LazyGridState, itemCount: Int, onLoadMore: () -> Unit) {
    LaunchedEffect(gridState, itemCount) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .distinctUntilChanged()
            .collect { index ->
                if (itemCount > 0 && index >= itemCount - 4) onLoadMore()
            }
    }
}
