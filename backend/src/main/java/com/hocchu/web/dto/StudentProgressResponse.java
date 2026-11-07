package com.hocchu.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentProgressResponse {
    private Long studentId;
    private String studentName;
    private Integer totalPractices;
    private Float averagePracticeScore;
    private Integer totalSubmissions;
    private Float averageSubmissionScore;
    private List<PracticePoint> practiceHistory; // Lịch sử luyện viết gần nhất
    private List<SubmissionPoint> submissionHistory; // Lịch sử nộp bài gần nhất

    @Getter
    @Setter
    @AllArgsConstructor
    public static class PracticePoint {
        private String letter;
        private Float score;
        private String date; // dd/MM
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class SubmissionPoint {
        private String title;
        private Float score;
        private String date; // dd/MM
    }
}
