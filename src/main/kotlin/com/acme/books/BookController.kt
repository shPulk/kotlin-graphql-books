package com.acme.books

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.BatchMapping
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

data class AddBookInput(
    val title: String,
    val pageCount: Int,
    val authorId: Long
)

// every field is optional: only the ones sent are changed
data class UpdateBookInput(
    val title: String?,
    val pageCount: Int?,
    val authorId: Long?
)

data class BookPage(
    val content: List<Book>,
    val page: Int,
    val size: Int,
    val totalElements: Int,
    val totalPages: Int
)

@Controller
class BookController(
    private val bookRepository: BookRepository,
    private val authorRepository: AuthorRepository
) {

    @QueryMapping
    fun books(@Argument page: Int, @Argument size: Int): BookPage {
        require(page >= 0) { "Page must not be negative" }
        require(size in 1..100) { "Size must be between 1 and 100" }

        val result = bookRepository.findAll(
            PageRequest.of(page, size, Sort.by("id"))
        )

        return BookPage(
            result.content,
            page,
            size,
            result.totalElements.toInt(),
            result.totalPages
        )
    }

    @QueryMapping
    fun bookById(@Argument id: Long): Book? =
        bookRepository.findById(id).orElse(null)

    @QueryMapping
    fun authors(): List<Author> =
        authorRepository.findAll()

    @MutationMapping
    fun addBook(@Argument input: AddBookInput): Book {
        require(input.title.isNotBlank()) {
            "Title must not be blank"
        }

        require(input.pageCount > 0) {
            "Page count must be positive"
        }

        require(authorRepository.existsById(input.authorId)) {
            "Author ${input.authorId} not found"
        }

        return bookRepository.save(
            Book(
                title = input.title,
                pageCount = input.pageCount,
                authorId = input.authorId
            )
        )
    }

    @MutationMapping
    fun updateBook(
        @Argument id: Long,
        @Argument input: UpdateBookInput
    ): Book {
        val book = bookRepository.findById(id).orElseThrow {
            IllegalArgumentException("Book $id not found")
        }

        input.title?.let {
            require(it.isNotBlank()) {
                "Title must not be blank"
            }
            book.title = it
        }

        input.pageCount?.let {
            require(it > 0) {
                "Page count must be positive"
            }
            book.pageCount = it
        }

        input.authorId?.let {
            require(authorRepository.existsById(it)) {
                "Author $it not found"
            }
            book.authorId = it
        }

        return bookRepository.save(book)
    }

    // returns false for an unknown id, so deleting twice is harmless
    @MutationMapping
    fun deleteBook(@Argument id: Long): Boolean {
        if (!bookRepository.existsById(id)) return false

        bookRepository.deleteById(id)
        return true
    }

    // Called once with ALL the books in the response, so authors load in one query
    @BatchMapping(typeName = "Book", field = "author")
    fun author(books: List<Book>): Map<Book, Author> {
        val authors = authorRepository
            .findAllById(books.map { it.authorId }.toSet())
            .associateBy { it.id }

        return books.associateWith {
            authors.getValue(it.authorId)
        }
    }
}
