package com.hocchu.web.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "writing_practices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WritingPractice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(nullable = false, length = 10)
    private String letter;

    @Lob
    @Column(name = "canvas_image_url", columnDefinition = "LONGTEXT")
    private String canvasImageUrl; // Chứa dữ liệu ảnh Base64 vẽ chữ của học sinh

    @Column(name = "accuracy_score")
    private Float accuracyScore;

    @Column(name = "practiced_at", insertable = false, updatable = false)
    private LocalDateTime practicedAt;
}
