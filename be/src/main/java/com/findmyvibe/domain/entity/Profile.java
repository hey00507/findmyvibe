package com.findmyvibe.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;

@Entity
@Table(name = "profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Profile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    private Session session;

    @Column(nullable = false, length = 50)
    private String typeLabel;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> keywords;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Integer> traits;

    private Profile(Session session, String typeLabel, String description,
                    List<String> keywords, Map<String, Integer> traits) {
        this.session = session;
        this.typeLabel = typeLabel;
        this.description = description;
        this.keywords = keywords;
        this.traits = traits;
    }

    public static Profile create(Session session, String typeLabel, String description,
                                 List<String> keywords, Map<String, Integer> traits) {
        return new Profile(session, typeLabel, description, keywords, traits);
    }
}
