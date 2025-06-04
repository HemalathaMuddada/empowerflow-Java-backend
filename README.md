# HRMS (Human Resource Management System)

This project is a Human Resource Management System built with Spring Boot.

## Project Structure

*   **`src/main/java/com/hrms`**: Contains the main application code.
    *   **`HrmsApplication.java`**: The main Spring Boot application class.
*   **`src/main/resources`**: Contains application resources.
    *   **`application.properties`**: Configuration file for the application (database, JWT, mail, etc.).
*   **`src/test/java/com/hrms`**: Contains test code.
*   **`pom.xml`**: Maven project configuration file, including dependencies and build settings.

## Prerequisites

*   Java 17 or higher
*   Maven 3.2+
*   PostgreSQL database

## Configuration

Before running the application, you need to update the placeholder values in `src/main/resources/application.properties` with your actual configuration details, especially for:

*   **Datasource**: `spring.datasource.username` and `spring.datasource.password`
*   **JWT Secret**: `jwt.secret` (generate a strong, unique key)
*   **Mail Settings**: `spring.mail.username` and `spring.mail.password` (if using Gmail, an "App Password" might be required)

## How to Run

1.  **Clone the repository.**
2.  **Configure `application.properties`** as described above.
3.  **Build the project using Maven**:
    ```bash
    mvn clean install
    ```
4.  **Run the application**:
    ```bash
    java -jar target/hrms-0.0.1-SNAPSHOT.jar
    ```
    Alternatively, you can run it from your IDE by running the `HrmsApplication` class.

The application will start on `http://localhost:8080` by default.
