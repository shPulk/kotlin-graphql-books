package com.acme.books

import jakarta.persistence.EntityManagerFactory
import org.hibernate.SessionFactory
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.graphql.test.tester.GraphQlTester

@SpringBootTest
@AutoConfigureGraphQlTester
class BookControllerTest {

    @Autowired
    lateinit var graphQlTester: GraphQlTester

    @Autowired
    lateinit var entityManagerFactory: EntityManagerFactory

    private fun createBook(title: String): String =
        graphQlTester.document(
            """mutation { addBook(input: {title: "$title", pageCount: 100, authorId: 1}) { id } }"""
        )
            .execute()
            .path("addBook.id")
            .entity(String::class.java)
            .get()

    @Test
    fun `books query returns at least the seeded books`() {
        graphQlTester.document(
            """{ books(size: 100) { content { id title } } }"""
        )
            .execute()
            .path("books.content[*].title")
            .entityList(String::class.java)
            .hasSizeGreaterThan(4)
    }

    @Test
    fun `first page is limited to the requested size`() {
        graphQlTester.document(
            """{ books(page: 0, size: 2) { content { id } page size totalPages } }"""
        )
            .execute()
            .path("books.content")
            .entityList(Any::class.java)
            .hasSize(2)
    }

    @Test
    fun `totals describe the whole collection`() {
        graphQlTester.document(
            """{ books(page: 0, size: 2) { totalElements totalPages } }"""
        )
            .execute()
            .path("books.totalElements")
            .entity(Int::class.java)
            .matches { it >= 5 }
    }

    @Test
    fun `second page does not repeat the first page`() {
        val first = graphQlTester.document(
            """{ books(page: 0, size: 2) { content { id } } }"""
        )
            .execute()
            .path("books.content[*].id")
            .entityList(String::class.java)
            .get()

        val second = graphQlTester.document(
            """{ books(page: 1, size: 2) { content { id } } }"""
        )
            .execute()
            .path("books.content[*].id")
            .entityList(String::class.java)
            .get()

        assertTrue(first.intersect(second.toSet()).isEmpty())
    }

    @Test
    fun `a page beyond the end is empty`() {
        graphQlTester.document(
            """{ books(page: 1000, size: 10) { content { id } } }"""
        )
            .execute()
            .path("books.content")
            .entityList(Any::class.java)
            .hasSize(0)
    }

    @Test
    fun `an invalid size is rejected`() {
        graphQlTester.document(
            """{ books(size: 0) { content { id } } }"""
        )
            .execute()
            .errors()
            .expect {
                it.message?.contains("Size must be between 1 and 100") == true
            }
            .verify()
    }

    @Test
    fun `bookById returns null for an unknown id`() {
        graphQlTester.document(
            """{ bookById(id: 99999) { title } }"""
        )
            .execute()
            .path("bookById")
            .valueIsNull()
    }

    @Test
    fun `books include their author`() {
        graphQlTester.document(
            """{ books(size: 100) { content { title author { name } } } }"""
        )
            .execute()
            .path("books.content[0].author.name")
            .entity(String::class.java)
            .isEqualTo("Dmitry Jemerov")
    }

    @Test
    fun `books with authors use a constant number of queries`() {
        val stats = entityManagerFactory
            .unwrap(SessionFactory::class.java)
            .statistics

        stats.isStatisticsEnabled = true
        stats.clear()

        graphQlTester.document(
            """{ books(size: 100) { content { title author { name } } } }"""
        )
            .execute()
            .path("books.content")
            .entityList(Any::class.java)
            .hasSizeGreaterThan(0)

        // books, optionally a count, and one batched query for all authors; never one per book
        assertTrue(
            stats.prepareStatementCount <= 3,
            "statements: ${stats.prepareStatementCount}"
        )
    }

    @Test
    fun `addBook creates a book`() {
        graphQlTester.document(
            """mutation { addBook(input: {title: "Test Book", pageCount: 100, authorId: 1}) { id title author { name } } }"""
        )
            .execute()
            .path("addBook.title")
            .entity(String::class.java)
            .isEqualTo("Test Book")
    }

    @Test
    fun `addBook rejects an unknown author`() {
        graphQlTester.document(
            """mutation { addBook(input: {title: "Orphan", pageCount: 100, authorId: 999}) { id } }"""
        )
            .execute()
            .errors()
            .expect {
                it.message?.contains("Author 999 not found") == true
            }
            .verify()
    }

    @Test
    fun `updateBook changes the title`() {
        val id = createBook("Old Title")

        graphQlTester.document(
            """mutation { updateBook(id: $id, input: {title: "New Title"}) { title } }"""
        )
            .execute()
            .path("updateBook.title")
            .entity(String::class.java)
            .isEqualTo("New Title")
    }

    @Test
    fun `updateBook leaves fields that were not sent`() {
        val id = createBook("Partial")

        graphQlTester.document(
            """mutation { updateBook(id: $id, input: {title: "Partial 2"}) { id } }"""
        ).execute()

        graphQlTester.document(
            """{ bookById(id: $id) { pageCount } }"""
        )
            .execute()
            .path("bookById.pageCount")
            .entity(Int::class.java)
            .isEqualTo(100)
    }

    @Test
    fun `updateBook rejects an unknown book`() {
        graphQlTester.document(
            """mutation { updateBook(id: 99999, input: {title: "X"}) { id } }"""
        )
            .execute()
            .errors()
            .expect {
                it.message?.contains("not found") == true
            }
            .verify()
    }

    @Test
    fun `deleteBook removes the book and is safe to repeat`() {
        val id = createBook("To Delete")

        graphQlTester.document(
            """mutation { deleteBook(id: $id) }"""
        )
            .execute()
            .path("deleteBook")
            .entity(Boolean::class.java)
            .isEqualTo(true)

        graphQlTester.document(
            """{ bookById(id: $id) { title } }"""
        )
            .execute()
            .path("bookById")
            .valueIsNull()

        graphQlTester.document(
            """mutation { deleteBook(id: $id) }"""
        )
            .execute()
            .path("deleteBook")
            .entity(Boolean::class.java)
            .isEqualTo(false)
    }
}
