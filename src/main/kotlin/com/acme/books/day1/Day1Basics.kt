package com.acme.books.day1

data class Author(val name: String, val country: String? = null)

data class Book(
    val id: Int,
    val title: String,
    val author: Author,
    val pages: Int,
    val year: Int
)

fun describe(book: Book, showYear: Boolean = false): String =
    if (showYear) "${book.title} by ${book.author.name} (${book.year})"
    else "${book.title} by ${book.author.name}"

fun size(book: Book): String = when {
    book.pages < 200 -> "short"
    book.pages < 400 -> "medium"
    else -> "long"
}

fun main() {
    val library = listOf(
        Book(1, "Kotlin in Action", Author("Dmitry Jemerov", "Russia"), 480, 2017),
        Book(2, "Effective Java", Author("Joshua Bloch", "USA"), 416, 2018),
        Book(3, "Clean Code", Author("Robert Martin"), 464, 2008),
        Book(4, "The Pragmatic Programmer", Author("Andrew Hunt", "USA"), 352, 1999),
        Book(5, "Refactoring", Author("Martin Fowler", "UK"), 448, 2018)
    )

    println(describe(library[0]))
    println(describe(library[0], showYear = true))

    val country: String? = library[2].author.country
    println("Country length: ${country?.length}")
    println("Country: ${country ?: "unknown"}")

    println(library.filter { it.pages > 400 }.map { it.title })
    println(library.sortedBy { it.year }.map { "${it.year} ${it.title}" })
    println(library.groupBy { it.year }.mapValues { (_, books) -> books.size })
    println(library.groupBy { size(it) })

    val updated = library[0].copy(pages = 500)
    println(updated)
    println(library[0] == library[0].copy())
}