package br.com.minhagibiteca.catalog.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity @Table(name = "editions")
public class Edition {
 @Id private UUID id; @Column(name="owner_user_id",nullable=false,updatable=false) private UUID ownerUserId;
 @Column(name="publisher_id") private UUID publisherId; @Column(name="series_id") private UUID seriesId;
 @Column(nullable=false,length=240) private String title; @Column(name="normalized_title",nullable=false,length=240) private String normalizedTitle;
 @Column(name="editorial_number",length=60) private String editorialNumber; @Column(length=120) private String variant;
 @Column(length=32) private String isbn; @Column(name="publication_year") private Integer publicationYear;
 @Column(name="format",length=60) private String format; @Column(length=2000) private String notes;
 @Column(name="cover_file_name",length=255) private String coverFileName; @Column(name="cover_content_type",length=100) private String coverContentType; @Column(name="cover_updated_at") private Instant coverUpdatedAt;
 @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt; @Column(name="updated_at",nullable=false) private Instant updatedAt;
 protected Edition() { }
 private Edition(UUID owner, UUID publisher, UUID series, String title, String number, String variant, String isbn, Integer year, String format, String notes) { id=UUID.randomUUID(); ownerUserId=owner; update(publisher,series,title,number,variant,isbn,year,format,notes); createdAt=Instant.now(); }
 public static Edition create(UUID owner, UUID publisher, UUID series, String title, String number, String variant, String isbn, Integer year, String format, String notes) { return new Edition(owner,publisher,series,title,number,variant,isbn,year,format,notes); }
 public void update(UUID publisher, UUID series, String title, String number, String variant, String isbn, Integer year, String format, String notes) { publisherId=publisher; seriesId=series; this.title=title.trim(); normalizedTitle=title.trim().toLowerCase(Locale.ROOT); editorialNumber=blank(number); this.variant=blank(variant); this.isbn=blank(isbn); publicationYear=year; this.format=blank(format); this.notes=blank(notes); updatedAt=Instant.now(); }
 public void updateCover(String fileName, String contentType) { coverFileName=fileName; coverContentType=contentType; coverUpdatedAt=Instant.now(); updatedAt=Instant.now(); }
 private static String blank(String s){return s==null||s.isBlank()?null:s.trim();}
 public UUID getId(){return id;} public UUID getOwnerUserId(){return ownerUserId;} public UUID getPublisherId(){return publisherId;} public UUID getSeriesId(){return seriesId;} public String getTitle(){return title;} public String getEditorialNumber(){return editorialNumber;} public String getVariant(){return variant;} public String getIsbn(){return isbn;} public Integer getPublicationYear(){return publicationYear;} public String getFormat(){return format;} public String getNotes(){return notes;} public String getCoverFileName(){return coverFileName;} public String getCoverContentType(){return coverContentType;}
}
