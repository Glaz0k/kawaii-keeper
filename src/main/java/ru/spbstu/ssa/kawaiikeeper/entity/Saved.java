package ru.spbstu.ssa.kawaiikeeper.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = { "user_id", "external_id" }, name = "uk_user_external")
})
public final class Saved {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "user_id")
    private Long userId;

    @Column(nullable = false, name = "external_id")
    private String externalId;

    @Column(nullable = false)
    private String imageUrl;

    @Column(nullable = false)
    private String categoryName;

    @CreationTimestamp
    @Column(nullable = false)
    private Instant createdAt;

}
