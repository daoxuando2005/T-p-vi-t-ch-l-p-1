package com.hocchu.web.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "alphabet_lessons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlphabetLesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 10)
    private String letter;

    @Column(name = "pronunciation_guide")
    private String pronunciationGuide;

    @Column(name = "audio_url")
    private String audioUrl;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "example_audio_url")
    private String exampleAudioUrl;

    @Column(name = "example_word", length = 100)
    private String exampleWord;

    @Column(name = "is_hidden")
    @Builder.Default
    private Boolean isHidden = false;
}
