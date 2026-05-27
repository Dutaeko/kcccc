# Komikcast

Project Android Full Jetpack Compose untuk aplikasi Komikcast.

## Konfigurasi
- Nama aplikasi: Komikcast
- Package: co.komikcast.app
- Default mode: dark
- Material 3 Dynamic Color

## Navigasi
- Home: data terbaru
- Populer: data populer
- Favorite Manga
- History Baca Manga
- Pencarian Manga dan filter genre

## Fitur
- Grid 3 untuk Home, Populer, Pencarian, hasil filter genre, dan Favorite.
- Home, Populer, Pencarian, dan filter genre memakai pagination bertahap.
- Card manga Home, Populer, Pencarian, dan filter genre menampilkan cover, judul, chapter terbaru.
- Detail manga menampilkan cover, judul, deskripsi dalam card, informasi lengkap dalam card, dan daftar chapter.
- Judul manga dan deskripsi manga di detail bisa diseleksi untuk disalin.
- Favorite Manga memakai Room Database dan MangaDao.
- History Baca Manga memakai Room Database dan MangaDao.
- Reader manga fullscreen menyembunyikan status bar dan navigation bar.
- Reader memiliki tombol Prev, Refresh, Daftar Chapter, dan Next dengan icon sesuai konteks.
- Home, Populer, dan Detail manga memakai Swipe Refresh.
- Pengambilan data memakai OkHttp Cache, Jsoup, Coroutines, CompletableFuture, dan ExecutorService.
- Cover manga memakai Coil dengan crossfade.

## GitHub Actions
- `.github/workflows/build-debug.yml`
- `.github/workflows/build-release-no-sign.yml`
