-- V4__add_example_audio_url_to_alphabet_lessons.sql
-- Thêm cột example_audio_url cho bảng alphabet_lessons
ALTER TABLE alphabet_lessons ADD COLUMN example_audio_url VARCHAR(255);
