package com.klentahn.plexyaudiobooks.ui.navigation

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object ServerSelect : Screen("server_select")
    object LibrarySelect : Screen("library_select")
    object MainLibrary : Screen("main_library")
    object Authors : Screen("authors")
    object About : Screen("about")

    object AuthorBooks : Screen("author_books/{author}") {
        fun createRoute(author: String): String = "author_books/$author"
    }

    object Player : Screen("player/{ratingKey}") {
        fun createRoute(ratingKey: String): String = "player/$ratingKey"
    }
}
