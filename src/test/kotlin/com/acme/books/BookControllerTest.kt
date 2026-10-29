package com.acme.books

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

    @Test
    fun `books query returns at least the seeded books`() {
        graphQlTester.document("{ books { id title } }")
            .execute()
            .path("books[*].title")
            .entityList(String::class.java)
            .hasSizeGreaterThan(4)
    }

    @Test
    fun `bookById returns null for an unknown id`() {
        graphQlTester.document("{ bookById(id: 99999) { title } }")
            .execute()
            .path("bookById")
            .valueIsNull()
    }

    @Test
    fun `books include their author`() {
        graphQlTester.document("{ books { title author { name } } }")
            .execute()
            .path("books[0].author.name")
            .entity(String::class.java)
            .isEqualTo("Dmitry Jemerov")
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
            .expect { it.message?.contains("Author 999 not found") == true }
            .verify()
    }
}
