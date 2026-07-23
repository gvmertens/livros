package com.library.catalog.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "books")
public class Book extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(unique = true, length = 13)
    public String isbn;

    @Column(name = "google_books_id", length = 255)
    public String googleBooksId;

    @Column(nullable = false, length = 500)
    public String title;

    @Column(length = 500)
    public String subtitle;

    @Column(columnDefinition = "TEXT")
    public String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    public Author author;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "book_authors",
            joinColumns = @JoinColumn(name = "book_id"),
            inverseJoinColumns = @JoinColumn(name = "author_id"))
    @OrderColumn(name = "author_order")
    public List<Author> authors = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publisher_id")
    public Publisher publisher;

    @Column(name = "isbn_10", length = 10)
    public String isbn10;

    @Column(name = "isbn_13", length = 13)
    public String isbn13;

    @Column(length = 16)
    public String language;

    @Column(name = "published_date", length = 32)
    public String publishedDate;

    @Column(name = "page_count")
    public Integer pageCount;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "book_external_categories", joinColumns = @JoinColumn(name = "book_id"))
    @Column(name = "category", length = 255)
    @OrderColumn(name = "category_order")
    public List<String> externalCategories = new ArrayList<>();

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    public String thumbnailUrl;

    @Column(name = "large_cover_url", columnDefinition = "TEXT")
    public String largeCoverUrl;

    @Column(name = "info_url", columnDefinition = "TEXT")
    public String infoUrl;

    @Column(name = "print_type", length = 50)
    public String printType;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_source", nullable = false, length = 30)
    public BookSource dataSource = BookSource.MANUAL;

    @Column(name = "metadata_updated_at")
    public Instant metadataUpdatedAt;

    @Column(name = "normalized_title", length = 500)
    public String normalizedTitle;

    @Column(name = "normalized_primary_author", length = 255)
    public String normalizedPrimaryAuthor;

    @Column(name = "normalized_publisher", length = 255)
    public String normalizedPublisher;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = Instant.now();
        if (metadataUpdatedAt == null) {
            metadataUpdatedAt = updatedAt;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
