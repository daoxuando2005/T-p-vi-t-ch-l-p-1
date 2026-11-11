package com.hocchu.web.service;

import com.hocchu.web.dto.ApiResponse;
import com.hocchu.web.entity.ParentStudentLink;
import com.hocchu.web.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import java.security.*;
import java.nio.charset.StandardCharsets;

@Service
public class LearningTrackingService {
    public static final ZoneId SCHOOL_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    public static final List<String> LETTERS = List.of("A","Ă","Â","B","C","D","Đ","E","Ê","G","H","I","K","L","M","N","O","Ô","Ơ","P","Q","R","S","T","U","Ư","V","X","Y");
    private static final Set<String> KINDS = Set.of("READING","WRITING","WORDS","SENTENCES","LESSON_COMPLETE");
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ParentStudentLinkRepository links;
    @Autowired private UserRepository users;
    @Autowired private SubmissionRepository submissions;
    @Autowired private WritingPracticeRepository practices;

    public record Event(String id, String kind, String letter, String source, boolean review, Instant occurredAt) {}
    public record Progress(String letter, boolean read, int writes, int words, int sentences) {
        public boolean complete() { return read && writes == 2 && words == 3 && sentences == 3; }
    }
    public record Sync(List<Event> events, List<Progress> progress) {}
    public record Activity(String id, String kind, String letter, String title, Instant occurredAt, Float score, String feedback) {}

    @Transactional(readOnly=true)
    public List<Progress> progress(Long studentId) {
        var saved=jdbc.query("SELECT letter,read_done,writes,words,sentences FROM learning_progress WHERE student_id=?",
            (rs,n)->new Progress(rs.getString("letter"),rs.getBoolean("read_done"),rs.getInt("writes"),rs.getInt("words"),rs.getInt("sentences")),studentId);
        Map<String,Progress> byLetter=new HashMap<>();saved.forEach(p->byLetter.put(p.letter(),p));
        return LETTERS.stream().map(l->byLetter.getOrDefault(l,new Progress(l,false,0,0,0))).toList();
    }

    @Transactional
    public Map<String,Object> sync(Long studentId, Sync body) {
        if(body == null || body.events() == null || body.progress() == null || body.events().size() > 100 || body.progress().size() != 29)
            throw new IllegalArgumentException("Dữ liệu tiến độ chưa hợp lệ.");
        Instant now = Instant.now();
        boolean previousComplete = true;
        for(int i=0;i<LETTERS.size();i++) {
            Progress p=body.progress().get(i);
            if(p == null || !LETTERS.get(i).equals(p.letter()) || p.writes()<0 || p.writes()>2 || p.words()<0 || p.words()>3 || p.sentences()<0 || p.sentences()>3
                || (!p.read() && (p.writes()>0 || p.words()>0 || p.sentences()>0)) || (p.writes()<2 && (p.words()>0 || p.sentences()>0))
                || (p.words()<3 && p.sentences()>0) || (!previousComplete && (p.read() || p.writes()>0 || p.words()>0 || p.sentences()>0)))
                throw new IllegalArgumentException("Tiến độ bài học chưa hợp lệ.");
            previousComplete = previousComplete && p.complete();
        }
        for(Event e:body.events()) {
            if(e == null || e.id()==null || e.kind()==null || e.letter()==null || e.source()==null || !UUID.fromString(e.id()).toString().equals(e.id()) || !KINDS.contains(e.kind()) || !LETTERS.contains(e.letter())
                || !Set.of("FREE","JOURNEY").contains(e.source()) || e.occurredAt()==null || e.occurredAt().isAfter(now.plusSeconds(300))
                || e.occurredAt().isBefore(Instant.parse("2000-01-01T00:00:00Z"))) throw new IllegalArgumentException("Ngày giờ hoạt động chưa hợp lệ. Hãy kiểm tra đồng hồ thiết bị.");
        }
        for(Progress p:body.progress()) jdbc.update("""
            INSERT INTO learning_progress(student_id,letter,read_done,writes,words,sentences,updated_at_utc) VALUES(?,?,?,?,?,?,?)
            ON DUPLICATE KEY UPDATE read_done=GREATEST(read_done,VALUES(read_done)),writes=GREATEST(writes,VALUES(writes)),
            words=GREATEST(words,VALUES(words)),sentences=GREATEST(sentences,VALUES(sentences)),updated_at_utc=VALUES(updated_at_utc)
            """,studentId,p.letter(),p.read(),p.writes(),p.words(),p.sentences(),utc(now));
        for(Event e:body.events()) jdbc.update("INSERT IGNORE INTO learning_events(student_id,event_id,kind,letter,source,review,occurred_at_utc,received_at_utc) VALUES(?,?,?,?,?,?,?,?)",
            studentId,e.id(),e.kind(),e.letter(),e.source(),e.review(),utc(e.occurredAt()),utc(now));
        return Map.of("success",true,"acceptedIds",body.events().stream().map(Event::id).toList());
    }

    public ParentStudentLink requireLinked(Long parentId, Long studentId) {
        return links.findByParentIdAndStudentId(parentId,studentId).filter(l->"APPROVED".equals(l.getStatus()))
            .orElseThrow(()->new AccessDeniedException("Không có quyền xem thông tin của học sinh này."));
    }
    @Transactional public Map<String,Object> unlink(Long parentId,Long studentId) {
        var link=requireLinked(parentId,studentId);links.delete(link);
        jdbc.update("DELETE FROM parent_link_invites WHERE student_id=?",studentId);
        return Map.of("success",true,"message","Đã ngắt liên kết. Tiến độ và bài làm của con vẫn được giữ.");
    }

    @Transactional(readOnly=true)
    public Map<String,Object> dashboard(Long parentId,Long studentId,LocalDate date) {
        ParentStudentLink link=requireLinked(parentId,studentId);
        LocalDate today=LocalDate.now(SCHOOL_ZONE);
        if(date.isAfter(today) || date.isBefore(today.minusDays(90))) throw new IllegalArgumentException("Chọn ngày trong 90 ngày gần đây.");
        Instant from=date.atStartOfDay(SCHOOL_ZONE).toInstant(),to=date.plusDays(1).atStartOfDay(SCHOOL_ZONE).toInstant();
        List<Activity> events=jdbc.query("SELECT * FROM learning_events WHERE student_id=? AND occurred_at_utc>=? AND occurred_at_utc<? ORDER BY occurred_at_utc DESC LIMIT 200",
            (rs,n)->new Activity("activity:"+rs.getString("event_id"),rs.getString("kind"),rs.getString("letter"),
                title(rs.getString("kind"),rs.getString("letter"))+(rs.getBoolean("review")?" · Ôn tập":""),instant(rs.getString("occurred_at_utc")),null,""),studentId,utc(from),utc(to));
        List<Activity> timeline=new ArrayList<>(events);
        var allSubmissions=submissions.findByStudentId(studentId);
        var allPractices=practices.findByStudentIdOrderByPracticedAtDesc(studentId);
        for(var s:allSubmissions) if(s.getSubmittedAt()!=null && s.getSubmittedAt().toLocalDate().equals(date))
            timeline.add(new Activity("submission:"+s.getId(),"SUBMISSION","","Nộp bài: "+s.getAssignment().getTitle(),
                s.getSubmittedAt().atZone(SCHOOL_ZONE).toInstant(),s.getScore(),s.getFeedback()));
        for(var p:allPractices) if(p.getPracticedAt()!=null && p.getPracticedAt().toLocalDate().equals(date))
            timeline.add(new Activity("practice:"+p.getId(),"WRITING",p.getLetter(),"Luyện viết chữ "+p.getLetter(),p.getPracticedAt().atZone(SCHOOL_ZONE).toInstant(),null,""));
        timeline.sort(Comparator.comparing(Activity::occurredAt).reversed());
        var counts=jdbc.queryForList("SELECT kind,COUNT(*) n FROM learning_events WHERE student_id=? AND occurred_at_utc>=? AND occurred_at_utc<? GROUP BY kind",studentId,utc(from),utc(to));
        Map<String,Long> totals=new HashMap<>(); counts.forEach(row->totals.put((String)row.get("kind"),((Number)row.get("n")).longValue()));
        long writes=totals.getOrDefault("WRITING",0L)+allPractices.stream().filter(p->p.getPracticedAt()!=null&&p.getPracticedAt().toLocalDate().equals(date)).count();
        long submitted=allSubmissions.stream().filter(s->s.getSubmittedAt()!=null&&s.getSubmittedAt().toLocalDate().equals(date)).count();
        List<Map<String,Object>> progress=jdbc.query("SELECT * FROM learning_progress WHERE student_id=?",(rs,n)->Map.<String,Object>of("letter",rs.getString("letter"),
            "read",rs.getBoolean("read_done"),"writes",rs.getInt("writes"),"words",rs.getInt("words"),"sentences",rs.getInt("sentences"),
            "complete",rs.getBoolean("read_done")&&rs.getInt("writes")==2&&rs.getInt("words")==3&&rs.getInt("sentences")==3),studentId);
        List<Map<String,Object>> week=new ArrayList<>();
        for(int i=6;i>=0;i--) {
            LocalDate day=date.minusDays(i);Instant start=day.atStartOfDay(SCHOOL_ZONE).toInstant(),end=day.plusDays(1).atStartOfDay(SCHOOL_ZONE).toInstant();
            long amount=jdbc.queryForObject("SELECT COUNT(*) FROM learning_events WHERE student_id=? AND occurred_at_utc>=? AND occurred_at_utc<? AND kind<>'LESSON_COMPLETE'",Long.class,studentId,utc(start),utc(end));
            amount+=allSubmissions.stream().filter(s->s.getSubmittedAt()!=null&&s.getSubmittedAt().toLocalDate().equals(day)).count();
            amount+=allPractices.stream().filter(p->p.getPracticedAt()!=null&&p.getPracticedAt().toLocalDate().equals(day)).count();
            week.add(Map.of("date",day,"activities",amount));
        }
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("studentId",studentId);result.put("studentName",Optional.ofNullable(link.getStudent().getFullName()).orElse(link.getStudent().getUsername()));
        result.put("date",date);result.put("zoneId",SCHOOL_ZONE.getId());
        result.put("summary",Map.of("readings",totals.getOrDefault("READING",0L),"writings",writes,"exercises",totals.getOrDefault("WORDS",0L)+totals.getOrDefault("SENTENCES",0L),"submissions",submitted));
        result.put("timeline",timeline.stream().limit(200).toList());result.put("timelineTruncated",timeline.size()>200||events.size()==200);
        result.put("progress",progress);result.put("progressAvailable",progress.size()==29);result.put("week",week);
        result.put("completedLetters",progress.stream().filter(p->Boolean.TRUE.equals(p.get("complete"))).count());
        return result;
    }

    @Transactional
    public Map<String,String> issueLinkCode(Long studentId) {
        var linked=links.findAllByStudentId(studentId).stream().filter(l->"APPROVED".equals(l.getStatus())).findFirst();
        if(linked.isPresent()) return Map.of("linked","true","parentName",Optional.ofNullable(linked.get().getParent().getFullName()).orElse("Phụ huynh"));
        byte[] bytes=new byte[16];new SecureRandom().nextBytes(bytes);
        String code=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expires=Instant.now().plusSeconds(1800);
        jdbc.update("INSERT INTO parent_link_invites(student_id,code_hash,expires_at_utc) VALUES(?,?,?) ON DUPLICATE KEY UPDATE code_hash=VALUES(code_hash),expires_at_utc=VALUES(expires_at_utc)",studentId,hash(code),utc(expires));
        return Map.of("linked","false","linkCode",code,"expiresAt",expires.toString());
    }

    @Transactional
    public ApiResponse linkParent(Long parentId,String value) {
        String code=value==null?"":value.trim();
        if(!code.matches("[A-Za-z0-9_-]{22}")) return new ApiResponse(false,"Nhập mã liên kết được tạo trong hồ sơ của con.");
        var invites=jdbc.queryForList("SELECT student_id,CAST(expires_at_utc AS CHAR) expires_at_utc FROM parent_link_invites WHERE code_hash=? FOR UPDATE",hash(code));
        if(invites.isEmpty()||!instant(invites.get(0).get("expires_at_utc").toString()).isAfter(Instant.now())) return new ApiResponse(false,"Mã đã hết hạn hoặc chưa đúng. Hãy tạo mã mới từ app của con.");
        Long studentId=((Number)invites.get(0).get("student_id")).longValue();
        var existing=links.findAllByStudentId(studentId);
        if(existing.stream().anyMatch(l->"APPROVED".equals(l.getStatus())&&!l.getParent().getId().equals(parentId))) return new ApiResponse(false,"Học sinh đã liên kết với phụ huynh khác.");
        ParentStudentLink link=existing.stream().filter(l->l.getParent().getId().equals(parentId)).findFirst()
            .orElseGet(()->existing.stream().filter(l->"PENDING".equals(l.getStatus())).findFirst().orElseGet(()->ParentStudentLink.builder()
                .student(users.getReferenceById(studentId)).linkCode(UUID.randomUUID().toString().replace("-","").substring(0,16)).build()));
        link.setParent(users.getReferenceById(parentId));
        link.setStatus("APPROVED");link.setLinkedAt(LocalDateTime.now(SCHOOL_ZONE));links.save(link);
        jdbc.update("DELETE FROM parent_link_invites WHERE student_id=?",studentId);
        return new ApiResponse(true,"Đã liên kết với con.");
    }
    private static String hash(String value) { try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);} }
    private static String utc(Instant value) {return java.time.format.DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss.SSS").withZone(ZoneOffset.UTC).format(value);}
    private static Instant instant(String value) {return LocalDateTime.parse(value.replace(' ','T')).toInstant(ZoneOffset.UTC);}
    private static String title(String kind,String letter) {return switch(kind) {
        case "READING" -> "Nghe và đọc chữ "+letter;case "WRITING" -> "Tập viết chữ "+letter;case "WORDS" -> "Điền từ với chữ "+letter;
        case "SENTENCES" -> "Ghép câu với chữ "+letter;default -> "Hoàn thành bài chữ "+letter;};}
}
