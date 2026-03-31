# AI Coding Conventions

## Framework & Language Rules
- Language: Java (Spring/Quarkus)
- Database Access: Always use the Panache Repository pattern.
- Code Generation: Rely on OpenAPI generators (`openapi-generator`) for REST APIs controllers and DTOs.
- Avoid deprecated libraries and always use constructor injection over `@Autowired` on fields.

## Styling & Language
- Variable naming: English camelCase.
- Documentation: English Javadoc for public methods.
- Commit Messages: Use Conventional Commits (`feat:`, `fix:`, `docs:`) and link ticket IDs.
