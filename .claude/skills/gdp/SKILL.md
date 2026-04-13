```markdown
# gdp Development Patterns

> Auto-generated skill from repository analysis

## Overview
This skill teaches the core development patterns and conventions used in the `gdp` Java repository. You'll learn how to structure files, write imports and exports, follow commit message conventions, and understand the project's approach to testing. While no specific frameworks or automated workflows are detected, this guide will help you contribute code that fits seamlessly into the existing codebase.

## Coding Conventions

### File Naming
- **Convention:** PascalCase
- **Example:**  
  ```java
  // Correct
  MyClass.java

  // Incorrect
  myClass.java
  my_class.java
  ```

### Import Style
- **Convention:** Relative imports
- **Example:**  
  ```java
  import com.example.project.MyClass;
  ```

### Export Style
- **Convention:** Named exports (Java uses public classes)
- **Example:**  
  ```java
  public class MyClass {
      // class body
  }
  ```

### Commit Messages
- **Convention:** Conventional commits with `feat` prefix
- **Format:**  
  ```
  feat: Add new GDP calculation method
  ```
- **Average Length:** 31 characters

## Workflows

### Commit New Feature
**Trigger:** When adding a new feature to the codebase  
**Command:** `/commit-feature`

1. Implement your feature in a new or existing PascalCase-named Java file.
2. Use relative imports for any dependencies.
3. Export your class with a named `public class`.
4. Write a commit message starting with `feat:` followed by a concise description.
   - Example: `feat: Add GDP growth calculation`
5. Push your changes for review.

## Testing Patterns

- **Test File Pattern:** `*.test.ts`  
  While the main codebase is Java, test files are written in TypeScript with the `.test.ts` suffix. The specific testing framework is unknown.
- **Example:**  
  ```
  GDPService.test.ts
  ```
- **Note:** Place your tests in appropriately named `.test.ts` files, following the PascalCase convention.

## Commands
| Command           | Purpose                                      |
|-------------------|----------------------------------------------|
| /commit-feature   | Guide for adding a new feature and committing|
```
