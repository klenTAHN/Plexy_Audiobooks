package com.klentahn.plexyaudiobooks.ui.navigation

sealed class Screen(val route: String) {
    object PlexLink : Screen("plex_link")
    object ServerSelect : Screen("server_select")
    object LibrarySelect : Screen("library_select")
    object MainLibrary : Screen("main_library")
    object Authors : Screen("authors")
    object AuthorBooks : Screen("author_books/{author}") {
        fun createRoute(author: String) = "author_books/$author"
    }
    object Player : Screen("player/{ratingKey}") {
        fun createRoute(ratingKey: String) = "player/$ratingKey"
    }
}
