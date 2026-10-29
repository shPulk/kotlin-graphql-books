package com.acme.books

import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

data class Book(val id: String, val title: String, val pageCount: Int)

@Controller
class BookController {

    private val books = listOf(
        Book("1", "Kotlin in Action", 480),
        Book("2", "Effective Java", 416)
    )

    @QueryMapping
    fun books(): List<Book> = books
}
