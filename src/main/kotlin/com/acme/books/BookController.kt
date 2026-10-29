package com.acme.books

import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller

data class AddBookInput(val title: String, val pageCount: Int, val authorId: Long)

@Controller
class BookController(
    private val bookRepository: BookRepository,
    private val authorRepository: AuthorRepository
) {

    @QueryMapping
    fun books(): List<Book> = bookRepository.findAll()

    @QueryMapping
    fun bookById(@Argument id: Long): Book? = bookRepository.findById(id).orElse(null)

    @QueryMapping
    fun authors(): List<Author> = authorRepository.findAll()

    @MutationMapping
    fun addBook(@Argument input: AddBookInput): Book {
        require(input.title.isNotBlank()) { "Title must not be blank" }
        require(input.pageCount > 0) { "Page count must be positive" }
        require(authorRepository.existsById(input.authorId)) { "Author ${input.authorId} not found" }

        return bookRepository.save(
            Book(
                title = input.title,
                pageCount = input.pageCount,
                authorId = input.authorId
            )
        )
    }

    @SchemaMapping(typeName = "Book", field = "author")
    fun author(book: Book): Author =
        authorRepository.findById(book.authorId).orElseThrow()
}
