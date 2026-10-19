package com.guru.jobboard.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/**
 * Known skills and their spellings. Used to pull skills out of a resume and to see which skills a job asks for.
 * Each entry is "Canonical name|alias|alias…". Very short/ambiguous words (C, R, Go) are deliberately left out.
 */
@Component
public class SkillCatalog {

    private static final String[] SKILLS = {
            // Languages
            "Java", "Python", "JavaScript|js|ecmascript", "TypeScript", "C++|cpp", "C#|csharp", "Golang",
            "Kotlin", "Scala", "Ruby", "PHP", "Swift", "Rust", "Dart", "Perl", "Groovy", "Bash|shell scripting",
            "SQL", "PL/SQL|plsql", "HTML|html5", "CSS|css3", "Sass|scss", "MATLAB", "Objective-C",
            // Java / backend frameworks
            "Spring Boot|springboot|spring-boot", "Spring|spring framework", "Spring MVC", "Spring Security",
            "Spring Data JPA|spring data", "Spring Cloud", "Hibernate", "JPA", "JDBC", "Servlets|servlet", "JSP",
            "Microservices|micro services|microservice", "REST API|rest apis|restful|rest api|restful apis|restful services|rest services",
            "GraphQL", "gRPC", "SOAP", "Maven", "Gradle", "JUnit|junit5", "Mockito", "Lombok", "Thymeleaf",
            "Node.js|nodejs|node", "Express.js|expressjs", "NestJS", "Django", "Flask", "FastAPI",
            ".NET|dotnet|asp.net|.net core", "Ruby on Rails|rails", "Laravel", "Quarkus", "Micronaut",
            // Frontend
            "React|reactjs|react.js", "Redux|redux toolkit", "Next.js|nextjs", "Angular|angularjs", "Vue.js|vue|vuejs",
            "Svelte", "jQuery", "Bootstrap", "Tailwind CSS|tailwind|tailwindcss", "Material UI|mui", "Vite",
            "Webpack", "React Native", "Flutter", "Android", "iOS",
            // Databases
            "PostgreSQL|postgres|postgresql", "MySQL", "Oracle|oracle db", "SQL Server|mssql|ms sql",
            "MongoDB|mongo", "Redis", "Cassandra", "DynamoDB", "Elasticsearch|elastic search", "SQLite",
            "Firebase", "Snowflake", "BigQuery", "Neo4j", "MariaDB",
            // Messaging
            "Kafka|apache kafka", "RabbitMQ", "ActiveMQ", "JMS",
            // Cloud & DevOps
            "AWS|amazon web services", "Azure|microsoft azure", "GCP|google cloud|google cloud platform",
            "Docker", "Kubernetes|k8s", "Jenkins", "GitHub Actions", "GitLab CI|gitlab", "CI/CD|ci cd|cicd",
            "Terraform", "Ansible", "Helm", "Linux|unix", "Nginx", "Apache Tomcat|tomcat", "Git", "GitHub",
            "Bitbucket", "Jira", "Confluence", "Postman", "Swagger|openapi", "Prometheus", "Grafana",
            "AWS Lambda|lambda", "EC2", "S3", "Serverless", "OAuth|oauth2", "JWT|json web token",
            "Keycloak", "SonarQube",
            // Data / ML
            "Machine Learning|ml", "Deep Learning", "TensorFlow", "PyTorch", "scikit-learn|sklearn", "Pandas",
            "NumPy", "Spark|apache spark|pyspark", "Hadoop", "Airflow", "ETL", "Power BI|powerbi", "Tableau",
            "Excel", "Data Analysis|data analytics", "NLP", "LLM|llms|large language models", "Generative AI|genai",
            "LangChain", "Computer Vision", "Statistics",
            // Practices
            "Agile", "Scrum", "TDD|test driven development", "Unit Testing", "Selenium", "Cypress", "Jest",
            "Playwright", "System Design", "Data Structures", "Algorithms", "OOP|object oriented programming",
            "Design Patterns", "Multithreading|concurrency",
            "DevOps", "SDLC", "Distributed Systems", "WebSockets|websocket",
            "Figma", "UI/UX|ui ux|ux",
    };

    /** Knowing the key skill implies knowing these (Spring Boot ⇒ Spring). */
    private static final Map<String, List<String>> IMPLIES = Map.ofEntries(
            Map.entry("Spring Boot", List.of("Spring")),
            Map.entry("Spring MVC", List.of("Spring")),
            Map.entry("Spring Security", List.of("Spring")),
            Map.entry("Spring Data JPA", List.of("Spring", "JPA")),
            Map.entry("Spring Cloud", List.of("Spring", "Microservices")),
            Map.entry("Hibernate", List.of("JPA")),
            Map.entry("PostgreSQL", List.of("SQL")),
            Map.entry("MySQL", List.of("SQL")),
            Map.entry("SQL Server", List.of("SQL")),
            Map.entry("Oracle", List.of("SQL")),
            Map.entry("MariaDB", List.of("SQL")),
            Map.entry("PL/SQL", List.of("SQL")),
            Map.entry("Next.js", List.of("React")),
            Map.entry("Redux", List.of("React")),
            Map.entry("TypeScript", List.of("JavaScript")),
            Map.entry("AWS Lambda", List.of("AWS", "Serverless")),
            Map.entry("EC2", List.of("AWS")),
            Map.entry("S3", List.of("AWS")),
            Map.entry("GitHub", List.of("Git")),
            Map.entry("GitHub Actions", List.of("GitHub", "Git", "CI/CD")),
            Map.entry("GitLab CI", List.of("Git", "CI/CD")),
            Map.entry("Jenkins", List.of("CI/CD")),
            Map.entry("Kubernetes", List.of("Docker")),
            Map.entry("JUnit", List.of("Unit Testing")),
            Map.entry("Mockito", List.of("Unit Testing")),
            Map.entry("Jest", List.of("Unit Testing")));

    /** The given skills plus everything they imply, compared case-insensitively. */
    public Set<String> withImplied(Collection<String> skills) {
        Set<String> out = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (String s : skills) {
            out.add(s);
            for (Map.Entry<String, List<String>> e : IMPLIES.entrySet()) {
                if (e.getKey().equalsIgnoreCase(s)) {
                    out.addAll(e.getValue());
                }
            }
        }
        return out;
    }

    private record Skill(String name, List<String> aliases, List<Pattern> patterns) {
    }

    private final List<Skill> skills = new ArrayList<>();

    public SkillCatalog() {
        for (String entry : SKILLS) {
            String[] parts = entry.split("\\|");
            List<String> aliases = new ArrayList<>();
            for (String p : parts) {
                aliases.add(p.toLowerCase(Locale.ROOT));
            }
            List<Pattern> patterns = aliases.stream().map(SkillCatalog::wordPattern).toList();
            skills.add(new Skill(parts[0], aliases, patterns));
        }
    }

    /** Skills found in the text, most mentioned first. */
    public List<String> extractRanked(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String lower = text.toLowerCase(Locale.ROOT);
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Skill skill : skills) {
            int n = 0;
            for (int i = 0; i < skill.aliases().size(); i++) {
                if (!lower.contains(skill.aliases().get(i))) {
                    continue; // cheap pre-check before running the regex
                }
                Matcher m = skill.patterns().get(i).matcher(lower);
                while (m.find()) {
                    n++;
                }
            }
            if (n > 0) {
                counts.put(skill.name(), n);
            }
        }
        removeImpliedParents(counts);
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .map(Map.Entry::getKey)
                .toList();
    }

    /** Skills found in the text as a set (canonical names). Only the first 10k characters are scanned. */
    public Set<String> extract(String text) {
        if (text != null && text.length() > 10_000) {
            text = text.substring(0, 10_000);
        }
        return new LinkedHashSet<>(extractRanked(text));
    }

    /** Maps user-typed skills ("reactjs", "k8s") to canonical names; unknown ones are kept as typed. */
    public List<String> normalize(Collection<String> input) {
        Set<String> out = new LinkedHashSet<>();
        for (String raw : input) {
            String s = raw == null ? "" : raw.trim();
            if (s.isEmpty()) {
                continue;
            }
            String lower = s.toLowerCase(Locale.ROOT);
            String canonical = skills.stream()
                    .filter(k -> k.aliases().contains(lower))
                    .map(Skill::name)
                    .findFirst()
                    .orElse(s);
            out.add(canonical);
        }
        return List.copyOf(out);
    }

    /** "Spring Boot" also matches the plain "Spring" alias; drop the parent when only the child was meant. */
    private static void removeImpliedParents(Map<String, Integer> counts) {
        dropIfOnlyFrom(counts, "Spring", "Spring Boot", "Spring MVC", "Spring Security", "Spring Data JPA", "Spring Cloud");
        dropIfOnlyFrom(counts, "React", "React Native");
        dropIfOnlyFrom(counts, "SQL", "PL/SQL", "SQL Server");
        dropIfOnlyFrom(counts, "GitHub", "GitHub Actions");
    }

    private static void dropIfOnlyFrom(Map<String, Integer> counts, String parent, String... children) {
        Integer parentCount = counts.get(parent);
        if (parentCount == null) {
            return;
        }
        int childTotal = 0;
        for (String c : children) {
            childTotal += counts.getOrDefault(c, 0);
        }
        if (parentCount <= childTotal) {
            counts.remove(parent);
        }
    }

    /** Whole-word match that also works for names with symbols like "c++", ".net" or "node.js". */
    static Pattern wordPattern(String alias) {
        return Pattern.compile("(?<![a-z0-9+#.])" + Pattern.quote(alias) + "(?![a-z0-9+#])");
    }
}
