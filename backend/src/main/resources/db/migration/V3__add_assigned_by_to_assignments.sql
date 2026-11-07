-- V3__add_assigned_by_to_assignments.sql
-- Thêm cột assigned_by vào bảng assignments để phân biệt bài tập do Giáo viên hay Phụ huynh giao

ALTER TABLE assignments ADD COLUMN assigned_by VARCHAR(20) DEFAULT 'TEACHER';
