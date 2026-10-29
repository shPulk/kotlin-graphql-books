package com.acme.books

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest
import org.springframework.graphql.test.tester.GraphQlTester

@GraphQlTest(BookController::class)
class BookControllerTest {

    @Autowired
    lateinit var graphQlTester: GraphQlTester

    @Test
    fun `books query returns two titles`() {
        graphQlTester.document("{ books { id title } }")
            .execute()
            .path("books[*].title")
            .entityList(String::class.java)
            .hasSize(2)
    }
}
