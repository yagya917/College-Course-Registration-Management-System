College Course & Registration Management System (CCRM)

A simple Java command-line application for managing college students, courses, enrollments, marks, grades, and academic records.

Overview

The College Course & Registration Management System (CCRM) is designed to bring common college registration activities into one application. It allows users to add students, create courses, enroll students, record marks, generate grades, and calculate GPA.

The project focuses on applying Object-Oriented Programming and core Java concepts to a practical academic management system.
 Features

- 👨‍🎓 Add and display students
- 📚 Add and display courses
- 📝 Enroll students in courses
- 🚫 Prevent duplicate enrollments
- ⚠️ Enforce an 18-credit maximum
- 📊 Record percentage marks
- 🎓 Convert marks into grades
- 📄 Generate student transcripts
- 📈 Calculate credit-weighted GPA
- 💾 Directory backup utility
- 🛡️ Custom exceptions for enrollment validation

Technologies Used

- Java
- Object-Oriented Programming
- Java Collections
- "ConcurrentHashMap"
- Java NIO
- Date & Time API
- Design Patterns

 Project Architecture

The project is divided into domain classes and service classes to keep responsibilities separated.

CCRM
│
├── CCRMApp
│   └── Command-line interface
│
├── Domain Classes
│   ├── Person
│   ├── Student
│   ├── Course
│   ├── Enrollment
│   └── Grade
│
├── Services
│   ├── StudentService
│   ├── CourseService
│   ├── EnrollmentService
│   └── BackupService
│
├── Implementations
│   ├── InMemoryStudentService
│   └── InMemoryCourseService
│
├── Configuration
│   └── AppConfig
│
└── Custom Exceptions
    ├── DuplicateEnrollmentException
    └── MaxCreditLimitExceededException

 Enrollment Rules

Before creating an enrollment, the system checks:

1. The student must exist.
2. The course must exist.
3. The student must not already be enrolled in the course.
4. The student's total registered credits must not exceed 18 credits.

If a rule is violated, an appropriate custom exception is raised.

Grading System

Marks| Grade| Grade Points
90+| S| 10
80–89| A| 9
70–79| B| 8
60–69| C| 7
50–59| D| 6
40–49| E| 5
Below 40| F| 0

GPA Calculation

The GPA is calculated using course credits:

GPA = Σ(Grade Points × Course Credits) / Σ(Course Credits)

Only completed courses are considered for the GPA calculation.

 Java Concepts Demonstrated

This project demonstrates several important Java concepts:

- Classes and Objects
- Inheritance
- Interfaces
- Enums
- Collections
- Custom Exceptions
- Service-based architecture
- Builder Pattern
- Singleton Pattern
- Java NIO file operations
- Date and Time API

For example, "Student" inherits common information from "Person", while service interfaces separate the required operations from their implementations.

 Data Storage

The current version uses in-memory storage through "ConcurrentHashMap".

This keeps the project lightweight and avoids database configuration. However, because there is no database, all records are lost when the application is closed.

 Current Limitations

The current version does not include:

- Database storage
- Login/authentication
- Role-based authorization
- Encryption
- Graphical user interface
- Web interface
- Network functionality
- Automated testing

These features can be added in future versions.

Future Improvements

Possible future enhancements include:

- 🗄️ Database integration
- 🔐 Login and role-based access
- 🌐 Web or desktop interface
- ✅ Automated testing
- 🔎 Stronger input validation
- 💾 Database-backed persistent records
- 📦 Backup option directly from the main CLI menu
 Testing

The application was manually tested using normal and invalid scenarios, including:

- Starting the application
- Adding students
- Adding courses
- Valid enrollment
- Duplicate enrollment
- Exceeding the 18-credit limit
- Recording marks
- Grade conversion
- GPA calculation
- Invalid student/course records

 Backup Utility

The project includes a "BackupService" that uses Java NIO and "Files.walkFileTree()" to traverse a source directory, recreate its structure, and copy files into a timestamped backup folder.

The backup utility is currently present in the source code but is not connected to the main CLI menu.

Getting Started

Prerequisites

- Java Development Kit (JDK)
- Command-line terminal
- Git (optional)

Run the Project

Clone the repository:

git clone <your-repository-url>

Navigate to the project directory:

cd <project-directory>

Compile the Java source:

javac CCRMApp.java

Run the application:

java CCRMApp

 Project

College Course & Registration Management System (CCRM)
Developed using Java as a practical application of Object-Oriented Programming and software design concepts.
