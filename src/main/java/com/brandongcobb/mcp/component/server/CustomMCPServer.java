/*  CustomMCPServer.java The primary purpose of this class is to serve as
 *  the Model Context Protocol server for the MCP spring boot application.
 *
 *  Copyright (C) 2025  github.com/brandongrahamcobb
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.brandongcobb.mcp.component.server;

import com.brandongcobb.mcp.domain.*;
import com.brandongcobb.mcp.*;
import com.brandongcobb.mcp.service.*;
import com.brandongcobb.mcp.tools.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.logging.Logger;

@Component
public class CustomMCPServer {
    
    private static final Logger LOGGER = Logger.getLogger(MCP.class.getName());
    private final ObjectMapper mapper = new ObjectMapper();
    private boolean initialized = false;
    private ToolService toolService;

    @Autowired
    public CustomMCPServer(ToolService toolService) {
        this.toolService = toolService;
        initializeTools();
    }
    
    
    private ObjectNode createError(int code, String message) {
        ObjectNode error = mapper.createObjectNode();
        error.put("code", code);
        error.put("message", message);
        return error;
    }

    private ObjectNode createErrorResponse(JsonNode idNode, int code, String message) {
        ObjectNode response = mapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        if (idNode != null && !idNode.isNull()) {
            response.set("id", idNode);
        }
        response.set("error", createError(code, message));
        return response;
    }


    private CompletableFuture<JsonNode> handleInitialize(JsonNode params, String id) {
        initialized = true;
        ObjectNode result = mapper.createObjectNode();
        result.put("protocolVersion", params.path("protocolVersion").asText("2025-06-18"));

        ObjectNode capabilities = mapper.createObjectNode();

        ObjectNode toolsCapabilities = mapper.createObjectNode();
        toolsCapabilities.put("canExecute", true);

        ArrayNode toolNames = mapper.createArrayNode();
        for (CustomTool<?, ?> tool : toolService.getTools()) {
            toolNames.add(tool.getName());
        }
        toolsCapabilities.set("toolNames", toolNames);

        capabilities.set("tools", toolsCapabilities);

        ObjectNode serverInfo = mapper.createObjectNode();
        serverInfo.put("name", "mcp");
        serverInfo.put("version", "1.0.0");

        result.set("capabilities", capabilities);
        result.set("serverInfo", serverInfo);

        ObjectNode response = mapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.set("result", result);

        return CompletableFuture.completedFuture(response);
    }
    
    private void sendError(PrintWriter writer, JsonNode idNode, int code, String message) {
        ObjectNode errorResponse = createErrorResponse(idNode, code, message);
        try {
            writer.println(mapper.writeValueAsString(errorResponse));
            writer.flush();
        } catch (Exception e) {
            LOGGER.severe("Failed to send error response: " + e.getMessage());
        }
    }

    public CompletableFuture<JsonNode> handleResourcesList(JsonNode request) {
        // Get the request ID
        int requestId = request.get("id").asInt();
        
        // Create empty resources response
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode response = mapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.put("id", requestId);
        
        ObjectNode result = mapper.createObjectNode();
        ArrayNode resources = mapper.createArrayNode();
        result.set("resources", resources);
        response.set("result", result);
        
        return CompletableFuture.completedFuture(response);
    }
    
    public CompletableFuture<JsonNode> handlePromptsList(JsonNode request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                int requestId = request.get("id").asInt();
                
                ObjectNode response = mapper.createObjectNode();
                response.put("jsonrpc", "2.0");
                response.put("id", requestId);
                
                ObjectNode result = mapper.createObjectNode();
                ArrayNode prompts = mapper.createArrayNode();
                
                // Add any prompts here if needed
                // Example:
                // ObjectNode prompt = mapper.createObjectNode();
                // prompt.put("name", "example-prompt");
                // prompt.put("description", "An example prompt");
                // ArrayNode arguments = mapper.createArrayNode();
                // prompt.set("arguments", arguments);
                // prompts.add(prompt);
                
                result.set("prompts", prompts);
                response.set("result", result);
                
                LOGGER.info("Prompts list completed successfully");
                return response;
                
            } catch (Exception e) {
                LOGGER.severe("Error in prompts/list: " + e);
                return createErrorResponse(request.get("id"),
                                           -32603, "Prompts list failed");
            }
        });
    }
    
    public CompletableFuture<String> handleRequest(String requestLine) {
        CompletableFuture<String> resultFuture = new CompletableFuture<>();
        try {
            LOGGER.finer("[JSON-RPC →] " + requestLine);
            JsonNode request = mapper.readTree(requestLine);
            String method = request.get("method").asText();
            JsonNode idNode = request.get("id");
            boolean isNotification = method.equals("notifications/initialized");
            JsonNode params = request.get("params");

            CompletableFuture<JsonNode> responseFuture = switch (method) {
                case "initialize" -> handleInitialize(params, idNode != null ? idNode.asText() : null);
                case "tools/list" -> handleToolsList(idNode != null ? idNode.asText() : null);
                case "tools/call" -> handleToolCall(params, idNode != null ? idNode.asText() : null);
                case "notifications/initialized" -> {
                    LOGGER.info("Received notifications/initialized → ignoring");
                    yield CompletableFuture.completedFuture(null); // skip response
                }
                case "resources/list" -> handleResourcesList(request);
                case "prompts/list" -> handlePromptsList(request);
                default -> CompletableFuture.completedFuture(createErrorResponse(idNode, -32601, "Method not found"));
            };

            responseFuture.thenAccept(responseJson -> {
                if (isNotification) {
                    resultFuture.complete(null); // No response expected
                    return;
                }
                try {
                    String jsonString = mapper.writeValueAsString(responseJson);
                    LOGGER.finer("[JSON-RPC ←] " + jsonString);
                    resultFuture.complete(jsonString);
                } catch (JsonProcessingException e) {
                    LOGGER.severe("JSON serialization error: " + e.getMessage());
                    resultFuture.completeExceptionally(e);
                }
            }).exceptionally(ex -> {
                LOGGER.severe("Internal error: " + ex.getMessage());
                resultFuture.completeExceptionally(ex);
                return null;
            });
        } catch (Exception e) {
            LOGGER.severe("Failed to parse request: " + e.getMessage());
            resultFuture.completeExceptionally(e);
        }

        return resultFuture;
    }
    
    private CompletableFuture<JsonNode> handleToolCall(JsonNode params, String id) {
        if (!initialized) {
            throw new IllegalStateException("Server not initialized");
        }
        try {
            String toolName = params.get("name").asText();
            JsonNode arguments = params.get("arguments");
            return toolService.callTool(toolName, arguments)
                .thenApply(toolResult -> {
                    ObjectNode response = mapper.createObjectNode();
                    response.put("jsonrpc", "2.0");
                    response.put("id", id);
                    response.set("result", toolResult);
                    LOGGER.finer(response.toString());
                    return response;
                });
        } catch (Exception e) {
            throw new CompletionException(e);
        }
    }

    private CompletableFuture<JsonNode> handleToolsList(String id) {
        ObjectNode result = mapper.createObjectNode();
        ArrayNode toolsArray = mapper.createArrayNode();

        for (CustomTool<?, ?> tool : toolService.getTools()) {
            ObjectNode toolDef = mapper.createObjectNode();
            toolDef.put("name", tool.getName());
            toolDef.put("description", tool.getDescription());
            toolDef.set("inputSchema", tool.getJsonSchema());
            toolsArray.add(toolDef);
        }
        result.set("tools", toolsArray);

        ObjectNode response = mapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.set("result", result);

        return CompletableFuture.completedFuture(response);
    }

    
    public void initializeTools() {
        toolService.registerTool(new CountFileLines());
        toolService.registerTool(new CreateFile());
        toolService.registerTool(new DiffFiles());
        toolService.registerTool(new FindInFile());
        toolService.registerTool(new ListLatexStructure());
        toolService.registerTool(new PDFLatex());
        toolService.registerTool(new Maven());
        toolService.registerTool(new Patch());
        toolService.registerTool(new ReadFile());
        toolService.registerTool(new ReadFileLines());
        toolService.registerTool(new ReadLatexSegment());
        toolService.registerTool(new SearchFiles());
        toolService.registerTool(new SearchWeb());
    }

    public static class ToolWrapper {
    
        private static final ObjectMapper mapper = new ObjectMapper();
    
        private final String name;
        private final String description;
        private final JsonNode inputSchema;
        private final ToolExecutor executor;
    
        public ToolWrapper(String name, String description, JsonNode inputSchema, ToolExecutor executor) {
            this.name = name;
            this.description = description;
            this.inputSchema = inputSchema;
            this.executor = executor;
        }
        
        public String getDescription() { return description; }
        public JsonNode getInputSchema() { return inputSchema; }
        public String getName() { return name; }
    
        public CompletableFuture<? extends ToolStatus> execute(JsonNode input) {
            try {
                return executor.execute(input);
            } catch (Exception e) {
                CompletableFuture<ToolStatus> failed = new CompletableFuture<>();
                failed.completeExceptionally(e);
                return failed;
            }
        }
    }

    @FunctionalInterface
    public interface ToolExecutor {
        CompletableFuture<? extends ToolStatus> execute(JsonNode input) throws Exception;
    }

    @Bean
    public List<ToolCallback> tools(ToolService service) {
        return List.of(ToolCallbacks.from(service));
    }
}

