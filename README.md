# MCP

I haven't chosen a name for this project. It is a Java-based application which provides tools via the [Model Context Protocol Server](https://modelcontextprotocol.io/introduction):.

## Project Description

This project 

## Technologies Used

*   **Asynchronous Programming:** `CompletableFuture` for non-blocking operations.
*   **Java:** The primary programming language.
*   **Logging:** Utilizes Java's logging framework.
*   **Maven:** The primary package manager (Gradle support upcoming).
*   **[Model Context Protocol Server](https://modelcontextprotocol.io/introduction):** A backend component to reveal context to the LLM's. 
*   **Spring-Boot-AI:** Autowires beans, component, services and the main SpringBootApplication.

## Setting Up the Project

This project is built using Maven.

**Prerequisites:**

*   Java 17 or higher installed and configured.
*   Maven installed and configured.

**Installation (Maven):**


1.  Clone the repository.
2.  Run `mvn clean install` to build the project and download dependencies.
3.  Copy Vyrtuous-0.1.jar to the root directory of the project as Vyrtuous.jar.
4.  Configure the application with appropriate settings in .env and save it to ~/.env or add the fields to your .env file.
5. **Linux:** docker build --platform=linux/arm64/v8 -t vyrtuous .
5. **Mac:** docker buildx build --platform=linux/arm64/v8 -t vyrtuous --load .
6. docker run -it --env-file ~/.env --rm vyrtuous

**Configuration:**

1.  Configure API keys, model paths, and other settings in the appropriate configuration files (e.g., `.bashrc`). Refer to the project's documentation for specific details.

## Description 
**Dependencies:**

This project relies on the following key dependencies (listed in `pom.xml`):

*   Apache Http Client
*   Jackson Databind
*   JDA
*   [Metadata](https://github.com/brandongrahamcobb/Metadata)
*   Spring-Boot

** Package Structure:**

*   `package com.brandongcobb.vyrtuous.service;`:  This package is for services, indicating their role in servicing requests from other parts of the code.
*   `package com.brandongcobb.vytuous.tools`: This package is for tools, self explanatory.
*   `package com.brandongcobb.record`: This package is for recording tool statistics.
*   `package com.brandongcobb.domain`: This package formalizes the connection between JSON tool schemas and their programatic fields.
*   `package com.brandongcobb.component`: This package contains the [Model Context Protocol](https://modelcontextprotocol.io/introduction) server.

**[Model Context Protocol Server](https://modelcontextprotocol.io/introduction)**

The server is the core component. It acts as a server that handles requests and orchestrates the execution of different tools.

*   **`count_file_lines`:**  Counts the number of lines in a file.
*   **`create_file`:** Creates a new file with specified content.
*   **`find_in_file`:** Searches for specified terms within a file, providing context lines around matches.
*   **`patch`:** Applies patches (replacements, insertions, deletions) to files.
*   **`read_file`:** Reads the contents of a file.
*   **`search_files`:** Searches for files matching specified criteria.
*   **`search_web`:**  Searches the web for information using the Google Programmable Search API.

## Future Enhancements

*   Further implementation of Spring-Boot AI

## License

This project is licensed under the [GNU General Public License v3](https://www.gnu.org/licenses/gpl-3.0).
