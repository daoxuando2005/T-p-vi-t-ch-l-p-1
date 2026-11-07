-- V5__add_tracing_image_url_to_alphabet_lessons.sql
-- Thêm cột tracing_image_url cho bảng alphabet_lessons
ALTER TABLE alphabet_lessons ADD COLUMN tracing_image_url VARCHAR(255);
