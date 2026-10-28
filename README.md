# Audition API

The purpose of this Spring Boot application is to test general knowledge of SpringBoot, Java, Gradle etc. It is created
for hiring needs of our company but can be used for other purposes.

## Overarching expectations & Assessment areas

<pre>
This is not a university test. 
This is meant to be used for job applications and MUST showcase your full skillset. 
<b>As such, PRODUCTION-READY code must be written and submitted. </b> 
</pre>

- clean, easy to understand code
- good code structures
- Proper code encapsulation
- unit tests with minimum 80% coverage.
- A Working application to be submitted.
- Observability. Does the application contain Logging, Tracing and Metrics instrumentation?
- Input validation.
- Proper error handling.
- Ability to use and configure rest template. We allow for half-setup object mapper and rest template
- Not all information in the Application is perfect. It is expected that a person would figure these out and correct.

## Getting Started

### Prerequisite tooling

- Any Springboot/Java IDE. Ideally IntelliJIdea.
- Java 17
- Gradle 8

### Prerequisite knowledge

- Java
- SpringBoot
- Gradle
- Junit

### Importing Google Java codestyle into INtelliJ

```
- Go to IntelliJ Settings
- Search for "Code Style"
- Click on the "Settings" icon next to the Scheme dropdown
- Choose "Import -> IntelliJ Idea code style XML
- Pick the file "google_java_code_style.xml" from root directory of the application
__Optional__
- Search for "Actions on Save"
    - Check "Reformat Code" and "Organise Imports"
```

---
**NOTE** -
It is highly recommended that the application be loaded and started up to avoid any issues.

---

## Audition Application information

This section provides information on the application and what the needs to be completed as part of the audition
application.

The audition consists of multiple TODO statements scattered throughout the codebase. The applicants are expected to:

- Complete all the TODO statements.
- Add unit tests where applicants believe it to be necessary.
- Make sure that all code quality check are completed.
- Gradle build completes sucessfully.
- Make sure the application if functional.

## Submission process

Applicants need to do the following to submit their work:

- Clone this repository
- Complete their work and zip up the working application.
- Applicants then need to send the ZIP archive to the email of the recruiting manager. This email be communicated to the
  applicant during the recruitment process.

  
---

## Additional Information based on the implementation

### Running and testing

- `.\gradlew bootRun` starts the application on port 8080 (Java 17).
- `.\gradlew clean build` compiles, runs the tests, Checkstyle, PMD and the coverage rule (80% minimum, enforced by
  `check`).
- Swagger UI: `http://localhost:8080/swagger-ui.html`; OpenAPI spec: `/v3/api-docs`.
- Request and response bodies of upstream calls are logged at DEBUG only. To see them:
  `.\gradlew bootRun --args="--logging.level.com.audition.configuration.RestTemplateLoggingInterceptor=DEBUG"`

### Endpoints

| Endpoint                    | Behaviour                                                                                                                 |
|-----------------------------|---------------------------------------------------------------------------------------------------------------------------|
| `GET /posts`                | All posts. Optional filters `userId` (exact), `title` and `body` (case-insensitive "contains"). Filters combine with AND. |
| `GET /posts/{id}`           | One post.                                                                                                                 |
| `GET /posts/{id}/comments`  | The post **with its comments embedded**.                                                                                  |
| `GET /comments?postId={id}` | The comments of a post as a **separate list**.                                                                            |

### Design decisions

- **Comments.** The two comment TODOs are worded differently, so I implemented them as two endpoints. The embedded
  version uses `AuditionPostWithComments` (a subclass), so `AuditionPost` stays unchanged. The post is fetched first, so
  an unknown post gives a 404; the upstream comments call returns `200 []` for unknown ids.
- **Filtering** is done in the service layer, not the controller, so the controller only maps HTTP.
- **Validation.** Ids must be positive whole numbers (at most 9 digits) and filter text at most 100 characters. Invalid
  input is rejected in the controller with a 400, before any upstream call. The checks use `@Validated` on the
  controller class.
- **Error handling.** Every error is an RFC 7807 problem detail.
    - The integration client translates upstream failures: 404 stays 404, other 4xx keep their status, upstream 5xx
      becomes 502, an unreachable upstream becomes 503. The original exception is kept as the cause, which resolves
      the "original error message is lost" TODO.
    - The advice shows the exception message to callers for 4xx only. For 5xx the caller gets a generic message and the
      full cause goes to the log, so internal details never leak.
    - Standard MVC errors (405, missing parameter, type mismatch) are handled by `ResponseEntityExceptionHandler`.
- **RestTemplate and ObjectMapper.** Built from Spring Boot's builders, with the base URL and timeouts in
  `application.yml` (`audition.client.*`). The mapper uses `yyyy-MM-dd`, ignores unknown properties, omits null and
  empty values, and does not write dates as timestamps. The `comments` field always appears, even when empty.
- **Observability.** A servlet filter adds `X-Trace-Id` and `X-Span-Id` to every response, taken from Micrometer's
  `Tracer`, so it works with the Brave bridge now and with OpenTelemetry later. The same ids appear in every log line. A
  RestTemplate interceptor logs each upstream call at INFO (method, URL, status, time).
- **Actuator.** Only `health` and `info` are exposed, with health details hidden. Metrics and Prometheus are
  deliberately not exposed, since the application has no authentication. In a real deployment I would put them on a
  separate management port or protect them with Spring Security.

### Dependency review (`build.gradle`)

Removed: `starter-data-jpa` (no database), `starter-webflux` and `reactor-test` (blocking `RestTemplate` only),
`starter-aop` and the Resilience4j starter (no circuit breaker), `spring-cloud-starter-config` (declared twice, no
config server), the duplicate Prometheus registry, `context-propagation`, and the pinned logback and slf4j versions (
managed by Spring Boot). Added `starter-validation`. Comments in `build.gradle` give the reason for each.

### Code quality

- Checkstyle and PMD are enabled, and the build fails on violations.
- Test code uses its own PMD ruleset (`config/pmd/ruleset-test.xml`) that drops rules that do not suit tests (for
  example, PMD 6 does not recognise MockMvc `andExpect` and Mockito `verify` as assertions). Production code uses the
  full ruleset. Gradle's default PMD rule set is disabled so the exclusions apply.
- Coverage excludes only code without logic: the constants class and the `main` class. Lombok-generated code is excluded
  through `lombok.config`.
- `SystemException` was reduced to the constructors actually used, and error constants live in one class.

### Assumptions and things I would do next

- I read "the comments must be returned as part of the post" as an embedded `comments` list on the response, and the
  second comments TODO as a plain list.
- The combined endpoint makes two upstream calls; with more time I would run them in parallel and cache upstream
  responses.
- With more time: a circuit breaker and retry for the upstream, pagination for `/posts`, authentication for the
  management endpoints, and contract tests against a stubbed upstream.
