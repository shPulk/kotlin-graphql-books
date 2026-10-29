package com.acme.books

import org.springframework.data.jpa.repository.JpaRepository

interface BookRepository : JpaRepository<Book, Long>

interface AuthorRepository : JpaRepository<Author, Long>
