import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

// ===== MAIN CLASS =====
public class CCRMApp {
    // ========== CONFIG (Singleton) ==========
    static class AppConfig {
        private static AppConfig instance;
        private final Path dataFolder;
        private AppConfig() {
            this.dataFolder = Paths.get(System.getProperty("user.home"), "ccrm_data");
        }
        public static synchronized AppConfig getInstance() {
            if (instance == null) instance = new AppConfig();
            return instance;
        }
        public Path getDataFolder() { return dataFolder; }
        public String timestamp() { return Instant.now().toString().replace(":", "-"); }
    }

    // ========== DOMAIN ==========
    enum Semester { SPRING, SUMMER, FALL; }

    enum Grade {
        S(10), A(9), B(8), C(7), D(6), E(5), F(0);
        private final int points;
        Grade(int p) { this.points = p; }
        public int getPoints() { return points; }
        public static Grade fromPercentage(double pct) {
            if (pct >= 90) return S;
            if (pct >= 80) return A;
            if (pct >= 70) return B;
            if (pct >= 60) return C;
            if (pct >= 50) return D;
            if (pct >= 40) return E;
            return F;
        }
    }

    abstract static class Person {
        protected final String id;
        protected String fullName;
        protected String email;
        protected LocalDate createdOn;
        public Person(String id, String fullName, String email) {
            this.id = id;
            this.fullName = fullName;
            this.email = email;
            this.createdOn = LocalDate.now();
        }
        public abstract String profile();
    }

    static class Student extends Person {
        enum Status { ACTIVE, INACTIVE }
        private String regNo;
        private Status status;
        private final Map<String, Enrollment> enrolledCourses = new ConcurrentHashMap<>();
        public Student(String id, String regNo, String fullName, String email) {
            super(id, fullName, email);
            this.regNo = regNo;
            this.status = Status.ACTIVE;
        }
        public void deactivate() { status = Status.INACTIVE; }
        public void enroll(Enrollment e) { enrolledCourses.put(e.getCourse().getCode(), e); }
        public void unenroll(String code) { enrolledCourses.remove(code); }
        public Collection<Enrollment> getEnrollments() { return enrolledCourses.values(); }
        @Override public String profile() {
            return "Student: " + fullName + " (" + regNo + ") - " + status;
        }
        public String transcript() {
            double pts=0, cr=0;
            StringBuilder sb=new StringBuilder("Transcript for "+fullName+"\n");
            for(Enrollment e:enrolledCourses.values()){
                Grade g=e.getGrade(); int c=e.getCourse().getCredits();
                sb.append(e.getCourse().getCode()).append(" ").append(e.getCourse().getTitle())
                  .append(" -> ").append(g==null?"N/A":g).append("\n");
                if(g!=null){ pts+=g.getPoints()*c; cr+=c; }
            }
            sb.append(String.format("GPA: %.2f", cr==0?0:pts/cr));
            return sb.toString();
        }
    }

    static class Course {
        private final String code; private String title; private int credits;
        private Semester semester; private String department;
        private Course(Builder b){ this.code=b.code; this.title=b.title; this.credits=b.credits;
            this.semester=b.semester; this.department=b.department; }
        public String getCode(){return code;} public String getTitle(){return title;}
        public int getCredits(){return credits;} public String getDepartment(){return department;}
        @Override public String toString(){ return code+" - "+title+" ("+credits+"cr)"; }
        static class Builder{
            private final String code; private String title=""; private int credits=3;
            private Semester semester=Semester.FALL; private String department="";
            public Builder(String c){code=c;} public Builder title(String t){title=t;return this;}
            public Builder credits(int cr){credits=cr;return this;}
            public Builder semester(Semester s){semester=s;return this;}
            public Builder department(String d){department=d;return this;}
            public Course build(){return new Course(this);}
        }
    }

    static class Enrollment {
        private final Student student; private final Course course;
        private Grade grade;
        public Enrollment(Student s,Course c){student=s;course=c;}
        public Course getCourse(){return course;} public Grade getGrade(){return grade;}
        public void recordMarks(double m){ grade=Grade.fromPercentage(m); }
    }

    // ========== EXCEPTIONS ==========
    static class DuplicateEnrollmentException extends RuntimeException { public DuplicateEnrollmentException(String m){super(m);} }
    static class MaxCreditLimitExceededException extends RuntimeException { public MaxCreditLimitExceededException(String m){super(m);} }

    // ========== SERVICES ==========
    interface StudentService {
        Student addStudent(String regNo,String name,String email);
        Optional<Student> findByRegNo(String regNo);
        Collection<Student> listAll();
    }
    static class InMemoryStudentService implements StudentService {
        private final Map<String,Student> map=new ConcurrentHashMap<>();
        public Student addStudent(String regNo,String name,String email){
            Student s=new Student(UUID.randomUUID().toString(),regNo,name,email); map.put(regNo,s); return s;}
        public Optional<Student> findByRegNo(String r){return Optional.ofNullable(map.get(r));}
        public Collection<Student> listAll(){return map.values();}
    }

    interface CourseService {
        Course addCourse(Course c); Optional<Course> findByCode(String code); Collection<Course> listAll();
    }
    static class InMemoryCourseService implements CourseService {
        private final Map<String,Course> map=new ConcurrentHashMap<>();
        public Course addCourse(Course c){map.put(c.getCode(),c);return c;}
        public Optional<Course> findByCode(String code){return Optional.ofNullable(map.get(code));}
        public Collection<Course> listAll(){return map.values();}
    }

    static class EnrollmentService {
        private final int maxCredits;
        public EnrollmentService(int mc){maxCredits=mc;}
        public Enrollment enroll(Student s,Course c){
            if(s.getEnrollments().stream().anyMatch(e->e.getCourse().getCode().equals(c.getCode())))
                throw new DuplicateEnrollmentException("Already enrolled");
            int total=s.getEnrollments().stream().mapToInt(e->e.getCourse().getCredits()).sum();
            if(total+c.getCredits()>maxCredits) throw new MaxCreditLimitExceededException("Exceeds limit");
            Enrollment e=new Enrollment(s,c); s.enroll(e); return e;
        }
        public void recordMarks(Student s,Course c,double m){
            s.getEnrollments().stream().filter(e->e.getCourse().getCode().equals(c.getCode())).findFirst().ifPresent(e->e.recordMarks(m));
        }
    }

    // ========== BACKUP UTILITY ==========
    static class BackupService {
        public static Path backupFolder(Path src) throws IOException {
            Path dest=AppConfig.getInstance().getDataFolder().resolve("backup-"+AppConfig.getInstance().timestamp());
            Files.createDirectories(dest);
            Files.walkFileTree(src,new SimpleFileVisitor<Path>(){
                public FileVisitResult preVisitDirectory(Path dir,BasicFileAttributes a)throws IOException{
                    Files.createDirectories(dest.resolve(src.relativize(dir))); return FileVisitResult.CONTINUE; }
                public FileVisitResult visitFile(Path f,BasicFileAttributes a)throws IOException{
                    Files.copy(f,dest.resolve(src.relativize(f)),StandardCopyOption.REPLACE_EXISTING); return FileVisitResult.CONTINUE; }
            });
            return dest;
        }
    }

    // ========== CLI ==========
    private final Scanner sc=new Scanner(System.in);
    private final StudentService studentSvc=new InMemoryStudentService();
    private final CourseService courseSvc=new InMemoryCourseService();
    private final EnrollmentService enrollSvc=new EnrollmentService(18);

    public static void main(String[] args) {
        System.out.println("CCRM started. Data folder: "+AppConfig.getInstance().getDataFolder());
        new CCRMApp().run();
    }

    void run() {
        boolean exit=false;
        while(!exit){
            System.out.println("\n1) Students 2) Courses 3) Enrollment 0) Exit");
            switch(sc.nextLine()){
                case "1": manageStudents(); break;
                case "2": manageCourses(); break;
                case "3": manageEnrollment(); break;
                case "0": exit=true; break;
                default: System.out.println("Invalid");
            }
        }
    }

    void manageStudents(){
        studentSvc.listAll().forEach(s->System.out.println(s.profile()));
        System.out.print("Add student? (y/n):");
        if(sc.nextLine().equalsIgnoreCase("y")){
            System.out.print("RegNo:"); String r=sc.nextLine();
            System.out.print("Name:"); String n=sc.nextLine();
            System.out.print("Email:"); String e=sc.nextLine();
            studentSvc.addStudent(r,n,e);
        }
    }

    void manageCourses(){
        courseSvc.listAll().forEach(System.out::println);
        System.out.print("Add course? (y/n):");
        if(sc.nextLine().equalsIgnoreCase("y")){
            System.out.print("Code:"); String c=sc.nextLine();
            System.out.print("Title:"); String t=sc.nextLine();
            System.out.print("Credits:"); int cr=Integer.parseInt(sc.nextLine());
            courseSvc.addCourse(new Course.Builder(c).title(t).credits(cr).build());
        }
    }

    void manageEnrollment(){
        System.out.print("Student regNo:"); String r=sc.nextLine();
        Optional<Student> so=studentSvc.findByRegNo(r); if(!so.isPresent()){System.out.println("Not found"); return;}
        Student s=so.get();
        System.out.print("Course code:"); String code=sc.nextLine();
        Optional<Course> co=courseSvc.findByCode(code); if(!co.isPresent()){System.out.println("Not found"); return;}
        Course c=co.get();
        try{ enrollSvc.enroll(s,c); System.out.println("Enrolled."); }catch(Exception ex){System.out.println(ex.getMessage());}
        System.out.print("Marks? (y/n):"); if(sc.nextLine().equalsIgnoreCase("y")){
            System.out.print("Enter marks:"); double m=Double.parseDouble(sc.nextLine());
            enrollSvc.recordMarks(s,c,m);
        }
        System.out.println(s.transcript());
    }
}

