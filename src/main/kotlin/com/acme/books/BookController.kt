package com.acme.books

import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller
import org.springframework.graphql.data.method.annotation.Argument

data class Book(val id: String, val title: String, val pageCount: Int)

@Controller
class BookController {

    private val books = listOf(
        Book("1", "Kotlin in Action", 480),
        Book("2", "Effective Java", 416)
    )

    @QueryMapping
    fun books(): List<Book> = books

    @QueryMapping
    fun bookById(@Argument id: String): Book? = books.find { it.id == id }
}
